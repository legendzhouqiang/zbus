package main

import (
	"encoding/json"
	"fmt"
	"log"
	"strings"

	"./protocol"
)

var restCommands = []string{protocol.Produce, protocol.Consume, protocol.Declare, protocol.Query, protocol.Empty}

func isRestCommand(cmd string) bool {
	for _, value := range restCommands {
		if value == cmd {
			return true
		}
	}
	return false
}

//ServerHandler manages session from clients
type ServerHandler struct {
	SessionTable  map[string]Session
	handlerTable  map[string]func(*ServerHandler, *Message, *Session, *int)
	serverAddress string
}

//NewServerHandler create ServerSessionHandler
func NewServerHandler(serverAddress string) *ServerHandler {
	s := &ServerHandler{
		SessionTable:  make(map[string]Session),
		handlerTable:  make(map[string]func(*ServerHandler, *Message, *Session, *int)),
		serverAddress: serverAddress,
	}

	s.handlerTable["favicon.ico"] = faviconHandler

	s.handlerTable[protocol.Home] = homeHandler
	s.handlerTable[protocol.Js] = jsHandler
	s.handlerTable[protocol.Css] = cssHandler
	s.handlerTable[protocol.Img] = imgHandler
	s.handlerTable[protocol.Page] = pageHandler
	s.handlerTable[protocol.Produce] = produceHandler
	s.handlerTable[protocol.Consume] = consumeHandler
	s.handlerTable[protocol.Query] = queryHandler
	s.handlerTable[protocol.Remove] = removeHandler
	s.handlerTable[protocol.Empty] = emptyHandler
	s.handlerTable[protocol.Tracker] = trackerHandler
	s.handlerTable[protocol.Server] = serverHandler
	s.handlerTable[protocol.TrackPub] = trackPubHandler
	s.handlerTable[protocol.TrackSub] = trackSubHandler
	return s
}

//Created when new session from client joined
func (s *ServerHandler) Created(sess *Session) {
	log.Printf("Session(%s) Created", sess)
}

//ToDestroy when connection from client going to close
func (s *ServerHandler) ToDestroy(sess *Session) {
	log.Printf("Session(%s) Destroyed", sess)
}

//OnError when socket error occured
func (s *ServerHandler) OnError(err error, sess *Session) {
	log.Printf("Session(%s) Error: %s", sess, err)
}

//OnMessage when message available on socket
func (s *ServerHandler) OnMessage(msg *Message, sess *Session, msgType *int) {
	msg.Header[protocol.Sender] = sess.ID
	msg.Header[protocol.Host] = s.serverAddress
	if _, ok := msg.Header[protocol.Id]; !ok {
		msg.Header[protocol.Id] = uuid()
	}

	handleUrlMessage(msg)

	cmd := msg.Header[protocol.Cmd]
	handler, ok := s.handlerTable[cmd]
	if ok {
		handler(s, msg, sess, msgType)
		return
	}
	res := NewMessage()
	res.Status = "400"
	res.SetBodyString(fmt.Sprintf("Bad format: command(%s) not support", cmd))
	sess.WriteMessage(res, msgType)
}

func handleUrlMessage(msg *Message) {
	if _, ok := msg.Header[protocol.Cmd]; ok {
		return
	}
	url := msg.Url
	if url == "/" {
		msg.Header[protocol.Cmd] = ""
		return
	}
	idx := strings.IndexByte(url, '?')
	var cmd string
	kvstr := ""
	if idx >= 0 {
		cmd = url[1:idx]
		kvstr = url[idx+1:]
	} else {
		cmd = url[1:]
	}

	topicStart := strings.IndexByte(cmd, '/')
	if topicStart > 0 {
		rest := cmd[topicStart+1:]

		cmd = cmd[0:topicStart]
		if cmd == protocol.Rpc {
			handleUrlRpc(msg, rest, kvstr)
		} else if isRestCommand(cmd) {
			bb := SplitClean(rest, "/")
			if len(bb) == 2 {
				msg.SetHeaderIfNone(protocol.Topic, bb[0])
				msg.SetHeaderIfNone(protocol.ConsumeGroup, bb[1])
			}
		}
	}
	msg.Header[protocol.Cmd] = strings.ToLower(cmd)

	if cmd != protocol.Rpc && kvstr != "" {
		handleUrlKVs(msg, kvstr)
	}
}

func handleUrlKVs(msg *Message, kvstr string) {
	if kvstr == "" {
		return
	}
	kvs := SplitClean(kvstr, "&")
	for _, kv := range kvs {
		bb := SplitClean(kv, "=")
		if len(bb) != 2 {
			continue
		}
		msg.SetHeaderIfNone(bb[0], bb[1])
	}
}

type request struct {
	Method string   `json:"method,omitempty"`
	Params []string `json:"params,omitempty"`
	Module string   `json:"module,omitempty"`
}

func handleUrlRpc(msg *Message, rest string, kvstr string) {
	// <topic>/<method>/<param_1>/../<param_n>[?module=<module>&&<header_ext_kvs>]
	handleUrlKVs(msg, kvstr)
	bb := SplitClean(rest, "/")
	if len(bb) < 2 {
		return //invalid
	}
	msg.SetHeaderIfNone(protocol.Topic, bb[0])
	method := bb[1]
	var params []string
	for i := 2; i < len(bb); i++ {
		params = append(params, bb[i])
	}
	req := &request{method, params, msg.Header["module"]}
	data, _ := json.Marshal(req)
	msg.SetBody(data)
}

func renderFile(file string, contentType string, s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	if file == "" {
		url := msg.Url
		bb := SplitClean(url, "/")
		if len(bb) > 1 {
			file = bb[1]
		}
	}

	data, err := ReadAssetFile(file)
	if err != nil {
		res.Status = "404"
		res.SetBodyString(fmt.Sprintf("File(%s) error: %s", file, err))
	} else {
		res.Status = "200"
		res.SetBody(data)
	}
	res.Header["content-type"] = contentType
	sess.WriteMessage(res, msgType)
}

func homeHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	renderFile("home.htm", "text/html", s, msg, sess, msgType)
}

func faviconHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	renderFile("logo.svg", "image/svg+xml", s, msg, sess, msgType)
}

func jsHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	renderFile("", "application/javascript", s, msg, sess, msgType)
}

func cssHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	renderFile("", "text/css", s, msg, sess, msgType)
}

func imgHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	renderFile("", "image/svg+xml", s, msg, sess, msgType)
}

func pageHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	renderFile("", "text/html", s, msg, sess, msgType)
}

func heartbeatHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	//just ignore
}

func produceHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}

func consumeHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}

func queryHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}

func removeHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}

func emptyHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}

func trackerHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}

func serverHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}

func trackPubHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}

func trackSubHandler(s *ServerHandler, msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "500"
	res.SetBodyString("Not implemented")
	sess.WriteMessage(res, msgType)
}
