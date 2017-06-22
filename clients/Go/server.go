package main

import (
	"bytes"
	"errors"
	"flag"
	"fmt"
	"log"
	"net"
	"os"
	"sync/atomic"

	"time"

	"./proto"
	"./websocket"
	"io"
)

// Session abstract socket connection
type Session struct {
	ID          string
	netConn     net.Conn
	wsConn      websocket.Conn
	isWebsocket bool
}

//NewSession create session
func NewSession(netConn *net.Conn, wsConn *websocket.Conn) *Session {
	sess := &Session{}
	sess.ID = uuid()
	if netConn != nil {
		sess.isWebsocket = false
		sess.netConn = *netConn
	}
	if wsConn != nil {
		sess.isWebsocket = true
		sess.wsConn = *wsConn
	}
	return sess
}

//Upgrade session to be based on websocket
func (s *Session) Upgrade(wsConn *websocket.Conn) {
	s.wsConn = *wsConn
	s.isWebsocket = true
}

//String get string value of session
func (s *Session) String() string {
	return fmt.Sprintf("%s-%s", s.ID, s.netConn.RemoteAddr())
}

//WriteMessage write message to underlying connection
func (s *Session) WriteMessage(msg *Message, msgType *int) error {
	buf := new(bytes.Buffer)
	msg.EncodeMessage(buf)
	if s.isWebsocket {
		return s.wsConn.WriteMessage(*msgType, buf.Bytes())
	}
	_, err := s.netConn.Write(buf.Bytes()) //TODO write may return 0 without err
	return err
}

//SessionHandler handles session lifecyle
type SessionHandler interface {
	Created(sess *Session)
	ToDestroy(sess *Session)
	OnMessage(msg *Message, sess *Session, msgType *int) //msgType only used for websocket
	OnError(err error, sess *Session)
}

var upgrader = Upgrader{}

func handleConnection(conn net.Conn, handler SessionHandler) {
	defer conn.Close()
	bufRead := new(bytes.Buffer)
	var wsConn *websocket.Conn
	session := NewSession(&conn, nil)
	handler.Created(session)
outter:
	for {
		data := make([]byte, 1024)
		n, err := conn.Read(data)
		if err != nil {
			handler.OnError(err, session)
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
			if IsWebSocketUpgrade(req.Header) {
				wsConn, err = upgrader.Upgrade(conn, req)
				if err == nil {
					log.Printf("Upgraded to websocket: %s\n", req)
					session.Upgrade(wsConn)
					break outter
				}
			}

			handler.OnMessage(req, session, nil)
		}
	}

	if wsConn != nil { //upgraded to Websocket
		bufRead = new(bytes.Buffer)
		for {
			msgtype, data, err := wsConn.ReadMessage()
			if err != nil {
				handler.OnError(err, session)
				break
			}
			bufRead.Write(data)
			req := DecodeMessage(bufRead)
			if req == nil {
				err = errors.New("Websocket invalid message: " + string(data))
				handler.OnError(err, session)
				break
			}
			if IsWebSocketUpgrade(req.Header) {
				continue
			}
			handler.OnMessage(req, session, &msgtype)
		}
	}
	handler.ToDestroy(session)
	conn.Close()
}

//Server = MqServer + Tracker
type Server struct {
	ServerAddress *proto.ServerAddress
	MqTable       map[string]*MessageQueue

	MqDir       string
	TrackerList []string

	infoVersion int64

	trackerOnly bool
}

func newServer() *Server {
	s := &Server{}
	s.infoVersion = time.Now().UnixNano() / int64(time.Millisecond)
	s.trackerOnly = false
	return s
}

func (s *Server) serverInfo() *proto.ServerInfo {
	info := &proto.ServerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = proto.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.TrackerList = []proto.ServerAddress{}
	info.TopicTable = make(map[string]*proto.TopicInfo)
	for key, mq := range s.MqTable {
		info.TopicTable[key] = mq.TopicInfo()
	}

	return info
}

func (s *Server) trackerInfo() *proto.TrackerInfo {
	info := &proto.TrackerInfo{}
	info.ServerAddress = s.ServerAddress
	info.ServerVersion = proto.VersionValue
	atomic.AddInt64(&s.infoVersion, 1)
	info.InfoVersion = s.infoVersion
	info.ServerTable = make(map[string]*proto.ServerInfo)
	if !s.trackerOnly {
		info.ServerTable[s.ServerAddress.String()] = s.serverInfo()
	}

	return info
}

func main() {
	log.SetFlags(log.Lshortfile | log.Ldate | log.Ltime)

	var (
		addr        string
		mqDir       string
		logDir      string
		trackerOnly bool
		logToConsole bool
		trackerList string
	)
	flag.StringVar(&addr, "addr", "0.0.0.0:15555", "Server address")
	flag.StringVar(&mqDir, "mqdir", "/tmp/zbus", "Message Queue directory")
	flag.StringVar(&logDir, "logdir", "", "Log file location")
	flag.StringVar(&trackerList, "tracker", "", "Tracker list")
	flag.BoolVar(&trackerOnly, "trackonly", false, "True--Work as Tracker only, False--MqServer+Tracker")
	flag.BoolVar(&logToConsole, "logconsole", true, "Log to console flag")
	flag.Parse()

	var logTargets []io.Writer
	if logToConsole {
		logTargets = append(logTargets, os.Stdout)
	} 
	if logDir != ""{  
	}
	if logTargets != nil{
		w := io.MultiWriter(logTargets...)
		log.SetOutput(w)
	}

	if _, err := os.Stat(mqDir); err != nil {
		if err := os.MkdirAll(mqDir, 0644); err != nil {
			log.Println("MqDir creation failed:", err.Error())
			return
		}
	}

	tcpAddr, err := net.ResolveTCPAddr("tcp", addr)
	if err != nil {
		log.Println("Error addres:", err.Error())
		return
	}
	fd, err := net.ListenTCP("tcp", tcpAddr)
	if err != nil {
		log.Println("Error listening:", err.Error())
		return
	}
	defer fd.Close()

	log.Println("Listening on " + addr)
	addr = ServerAddress(addr) //get real server address if needs
	server := newServer()
	server.MqDir = mqDir
	server.trackerOnly = trackerOnly
	server.ServerAddress = &proto.ServerAddress{addr, false}
	server.TrackerList = SplitClean(trackerList, ";")

	mqTable, err := LoadMqTable(mqDir)
	if err != nil {
		log.Println("Error loading MQ table: ", err.Error())
		return
	}
	server.MqTable = mqTable

	handler := NewServerHandler(server)
	for {
		conn, err := fd.AcceptTCP()
		if err != nil {
			log.Println("Error accepting: ", err.Error())
			return
		}
		go handleConnection(conn, handler)
	}
}
