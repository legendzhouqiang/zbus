package main

import (
	"bytes"
	"errors"
	"flag"
	"log"
	"net"

	"./websocket"
)

// Session abstract socket connection
type Session struct {
	ID          string
	netConn     net.Conn
	wsConn      websocket.Conn
	isWebsocket bool
}

//NewSession create session
func NewSession(netConn *net.Conn, wsConn *websocket.Conn) Session {
	sess := Session{}
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
func (s Session) Upgrade(wsConn *websocket.Conn) {
	s.wsConn = *wsConn
	s.isWebsocket = true
}

//ToString get string value of session
func (s Session) ToString() string {
	return s.ID
}

//WriteMessage write message to underlying connection
func (s Session) WriteMessage(msg Message, msgType *int) error {
	buf := new(bytes.Buffer)
	msg.EncodeMessage(buf)
	if s.isWebsocket {
		return s.wsConn.WriteMessage(*msgType, buf.Bytes())
	}
	_, err := s.netConn.Write(buf.Bytes()) //TODO write may return 0 without err
	return err
}

//SessionHandler handles session lifecyle
type SessionHandler struct {
	Created   func(sess Session)
	ToDestroy func(sess Session)
	OnMessage func(msg Message, sess Session, msgType *int) //msgType only used for websocket
	OnError   func(err error, sess Session)
}

//NewSessionHandler create default handler
func NewSessionHandler() SessionHandler {
	return SessionHandler{
		Created: func(sess Session) {
			log.Printf("Session(%s) created", sess.ToString())
		},
		ToDestroy: func(sess Session) {
			log.Printf("Session(%s) destroyed", sess.ToString())
		},
		OnMessage: func(msg Message, sess Session, msgType *int) {
			log.Print(msg.ToString())
		},
		OnError: func(err error, sess Session) {
			log.Printf("Session(%s) error: %s", sess.ID, err)
		},
	}
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
			if tokenListContainsValue(req.Header, "connection", "upgrade") {
				wsConn, err = upgrader.Upgrade(conn, req)
				if err == nil {
					log.Printf("Upgraded to websocket: %s\n", req.ToString())
					session.Upgrade(wsConn)
					break outter
				}
			}
			handler.OnMessage(*req, session, nil)
		}
	}

	if wsConn != nil { //upgrade to Websocket
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

			handler.OnMessage(*req, session, &msgtype)
		}
	}
	handler.ToDestroy(session)
	conn.Close()
}

var sessionTable map[string]Session

func main() {
	log.SetFlags(log.Lshortfile)
	sessionTable = make(map[string]Session)
	var addr = *flag.String("h", "0.0.0.0:15555", "zbus server address")
	server, err := net.Listen("tcp", addr)
	if err != nil {
		log.Println("Error listening:", err.Error())
		return
	}
	defer server.Close()
	log.Println("Listening on " + addr)

	handler := NewSessionHandler()

	handler.OnMessage = func(msg Message, sess Session, msgType *int) {
		res := NewMessage()
		res.Status = "200"
		res.SetBodyString("Hello World")
		sess.WriteMessage(*res, msgType)
	}

	for {
		conn, err := server.Accept()
		if err != nil {
			log.Println("Error accepting: ", err.Error())
			return
		}
		go handleConnection(conn, handler)
	}
}
