package main

import (
	"bytes"
	"errors"
	"flag"
	"fmt"
	"io"
	"log"
	"net"
	"os"
	"sync/atomic"
	"time"

	"./proto"
	"./websocket"
)

//Config stores the conguration for server
type Config struct {
	Address      string
	ServerName   string //override Address if provided
	MqDir        string
	LogDir       string
	CertFileDir  string
	LogToConsole bool
	Verbose      bool
	TrackOnly    bool
	TrackerList  []string
	IdleTimeout  time.Duration
}

//Server = MqServer + Tracker
type Server struct {
	Config        *Config
	ServerAddress *proto.ServerAddress
	MqTable       SyncMap // map[string]*MessageQueue
	SessionTable  SyncMap //Safe

	handlerTable  map[string]func(*Server, *Message, *Session) //readonly
	consumerTable *ConsumerTable

	listener net.Listener

	infoVersion int64

	tracker *Tracker

	wsUpgrader *Upgrader // upgrade TCP to websocket
}

//NewServer create a zbus server
func NewServer(config *Config) *Server {
	s := &Server{}
	s.Config = config
	s.SessionTable.Map = make(map[string]interface{})
	s.handlerTable = make(map[string]func(*Server, *Message, *Session))

	host, port := ServerAddress(config.Address) //get real server address if needs
	if config.ServerName != "" {
		host = config.ServerName
	}
	addr := fmt.Sprintf("%s:%d", host, port)
	s.ServerAddress = &proto.ServerAddress{addr, false} //TODO Support SSL

	s.MqTable.Map = make(map[string]interface{})
	s.consumerTable = newConsumerTable()
	s.infoVersion = CurrMillis()
	s.wsUpgrader = &Upgrader{}

	//init at last
	s.tracker = NewTracker(s)
	s.initServerHandler()

	return s
}

//Start zbus server(MqServer + Tracker)
func (s *Server) Start() error {
	var err error
	if s.listener != nil {
		log.Printf("No need to start again")
		return nil
	}

	s.listener, err = net.Listen("tcp", s.Config.Address)
	if err != nil {
		log.Println("Error listening:", err.Error())
		return err
	}
	log.Println("Listening on " + s.Config.Address)

	log.Println("Trying to load MqTable...")
	if err = s.LoadMqTable(); err != nil { //load MQ table
		return err
	}
	log.Println("MqTable loaded")
	s.tracker.JoinUpstreams(s.Config.TrackerList)

	for {
		conn, err := s.listener.Accept()
		if err != nil {
			log.Println("Error accepting: ", err.Error())
			return err
		}
		go s.handleConnection(conn)
	}
}

//Close server
func (s *Server) Close() {
	s.listener.Close() //TODO more release may required
}

//LoadMqTable from disk
func (s *Server) LoadMqTable() error {
	if err := EnsureDir(s.Config.MqDir); err != nil {
		log.Printf("MqDir(%s) creation failed:%s", s.Config.MqDir, err.Error())
		return err
	}
	mqTable, err := LoadMqTable(s.Config.MqDir)
	if err != nil {
		log.Println("Error loading MQ table: ", err.Error())
		return err
	}
	for key, val := range mqTable {
		s.MqTable.Set(key, val)
	}
	return nil
}

func (s *Server) serverInfo() *proto.ServerInfo {
	info := &proto.ServerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = proto.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.TrackerList = []proto.ServerAddress{}
	info.TopicTable = make(map[string]*proto.TopicInfo)

	s.MqTable.RLock()
	for _, m := range s.MqTable.Map {
		mq, _ := m.(*MessageQueue)
		info.TopicTable[mq.Name()] = mq.TopicInfo()
	}
	s.MqTable.RUnlock()

	for _, address := range s.Config.TrackerList {
		sa := &proto.ServerAddress{Address: address, SslEnabled: false}
		info.TrackerList = append(info.TrackerList, *sa)
	}
	s.addServerContext(info)
	return info
}

func (s *Server) trackerInfo() *proto.TrackerInfo {
	info := &proto.TrackerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = proto.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.ServerTable = make(map[string]*proto.ServerInfo)

	s.tracker.serverTable.RLock()
	for key, sinfo := range s.tracker.serverTable.Map {
		serverInfo, _ := sinfo.(*proto.ServerInfo)
		info.ServerTable[key] = serverInfo
	}
	s.tracker.serverTable.RUnlock()
	if !s.Config.TrackOnly {
		info.ServerTable[s.ServerAddress.String()] = s.serverInfo()
	}
	return info
}

func (s *Server) addServerContext(t interface{}) {
	switch t.(type) {
	case *proto.TopicInfo:
		info := t.(*proto.TopicInfo)
		info.ServerAddress = s.ServerAddress
		info.ServerVersion = proto.VersionValue
		info.ConsumerCount = int32(s.consumerTable.countForTopic(info.TopicName))
		for _, groupInfo := range info.ConsumeGroupList {
			groupInfo.ConsumerCount = int32(s.consumerTable.countForGroup(info.TopicName, groupInfo.GroupName))
		}
	case *proto.ServerInfo:
		info := t.(*proto.ServerInfo)
		info.ServerAddress = s.ServerAddress
		info.ServerVersion = proto.VersionValue
		for _, topicInfo := range info.TopicTable {
			s.addServerContext(topicInfo)
		}
	case *proto.TrackerInfo:
		info := t.(*proto.TrackerInfo)
		info.ServerAddress = s.ServerAddress
		info.ServerVersion = proto.VersionValue
		for _, serverInfo := range info.ServerTable {
			s.addServerContext(serverInfo)
		}
	}
}

func (s *Server) handleConnection(conn net.Conn) {
	defer conn.Close()
	bufRead := new(bytes.Buffer)
	var wsConn *websocket.Conn
	session := NewSession(&conn, nil)
	s.Created(session)
outter:
	for {
		data := make([]byte, 1024)
		conn.SetReadDeadline(time.Now().Add(s.Config.IdleTimeout))
		n, err := conn.Read(data)
		if err != nil {
			if err, ok := err.(net.Error); ok && err.Timeout() {
				s.OnIdle(session)
				if session.active {
					continue
				} else {
					break
				}
			}

			s.OnError(err, session)
			break
		}
		bufRead.Write(data[0:n])

		for {
			req := DecodeMessage(bufRead)
			if req == nil {
				bufRead2 := new(bytes.Buffer)
				bufRead2.Write(bufRead.Bytes())
				bufRead = bufRead2
				break
			}

			//upgrade to Websocket if requested
			if IsWebSocketUpgrade(&req.Header) {
				wsConn, err = s.wsUpgrader.Upgrade(conn, req)
				if err == nil {
					//log.Printf("Upgraded to websocket: %s\n", req)
					session.Upgrade(wsConn)
					break outter
				}
			}
			go s.OnMessage(req, session)
		}
	}

	if wsConn != nil { //upgraded to Websocket
		bufRead = new(bytes.Buffer)
		for {
			_, data, err := wsConn.ReadMessage()
			if err != nil {
				if err, ok := err.(net.Error); ok && err.Timeout() {
					s.OnIdle(session)
					if session.active {
						continue
					} else {
						break
					}
				}
				s.OnError(err, session)
				break
			}
			bufRead.Write(data)
			req := DecodeMessage(bufRead)
			if req == nil {
				err = errors.New("Websocket invalid message: " + string(data))
				s.OnError(err, session)
				break
			}
			if IsWebSocketUpgrade(&req.Header) {
				continue
			}
			go s.OnMessage(req, session)
		}
	}
	s.ToDestroy(session)
	conn.Close() //make sure to close the underlying socket
}

//ParseConfig from command line or config file
func ParseConfig() *Config {
	cfg := &Config{}
	var idleTime int
	var trackerList string

	flag.StringVar(&cfg.Address, "addr", "0.0.0.0:15555", "Server address")
	flag.StringVar(&cfg.ServerName, "name", "", "Server public server name, e.g. zbus.io")
	flag.IntVar(&idleTime, "idle", 60, "Idle detection timeout in seconds") //default to 1 minute
	flag.StringVar(&cfg.MqDir, "mqdir", "/tmp/zbus", "Message Queue directory")
	flag.StringVar(&cfg.LogDir, "logdir", "", "Log file location")
	flag.StringVar(&trackerList, "tracker", "", "Tracker list, e.g.: localhost:15555;localhost:15556")
	flag.BoolVar(&cfg.TrackOnly, "trackonly", false, "True--Work as Tracker only, False--MqServer+Tracker")

	flag.Parse()

	cfg.IdleTimeout = time.Duration(idleTime) * time.Second
	cfg.TrackerList = SplitClean(trackerList, ";")
	return cfg
}

func main() {
	log.SetFlags(log.Lshortfile | log.Ldate | log.Ltime)
	printBanner()

	config := ParseConfig()
	var logTargets []io.Writer
	if config.LogToConsole {
		logTargets = append(logTargets, os.Stdout)
	}
	if config.LogDir != "" {
	}
	if logTargets != nil {
		w := io.MultiWriter(logTargets...)
		log.SetOutput(w)
	}

	server := NewServer(config)
	server.Start()
}

func printBanner() {
	logo := fmt.Sprintf(`
                /\\\       
                \/\\\        
                 \/\\\    
     /\\\\\\\\\\\ \/\\\         /\\\    /\\\  /\\\\\\\\\\     
     \///////\\\/  \/\\\\\\\\\  \/\\\   \/\\\ \/\\\//////     
           /\\\/    \/\\\////\\\ \/\\\   \/\\\ \/\\\\\\\\\\    
          /\\\/      \/\\\  \/\\\ \/\\\   \/\\\ \////////\\\  
         /\\\\\\\\\\\ \/\\\\\\\\\  \//\\\\\\\\\   /\\\\\\\\\\
         \///////////  \/////////    \/////////   \//////////     Version: %s
		`, proto.VersionValue)
	fmt.Println(logo)
}
