package main

import (
	"encoding/json"
	"fmt"
	"log"
	"strings"

	"./protocol"
)

var restCommands = []string{
	protocol.Produce,
	protocol.Consume,
	protocol.Declare,
	protocol.Query,
	protocol.Remove,
	protocol.Empty,
}

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
	SessionTable map[string]*Session
	handlerTable map[string]func(*ServerHandler, *Message, *Session, *int)

	serverAddress string
	server        *Server
}

//NewServerHandler create ServerSessionHandler
func NewServerHandler(server *Server) *ServerHandler {
	s := &ServerHandler{
		SessionTable:  make(map[string]*Session),
		handlerTable:  make(map[string]func(*ServerHandler, *Message, *Session, *int)),
		serverAddress: server.ServerAddress.Address,
		server:        server,
	}

	s.handlerTable["favicon.ico"] = faviconHandler

	s.handlerTable[protocol.Home] = homeHandler
	s.handlerTable[protocol.Js] = jsHandler
	s.handlerTable[protocol.Css] = cssHandler
	s.handlerTable[protocol.Img] = imgHandler
	s.handlerTable[protocol.Page] = pageHandler
	s.handlerTable[protocol.Produce] = produceHandler
	s.handlerTable[protocol.Consume] = consumeHandler
	s.handlerTable[protocol.Declare] = declareHandler
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
	res := NewMessageStatus(400, "Bad format: command(%s) not support", cmd)
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
			if len(bb) > 0 {
				msg.SetHeaderIfNone(protocol.Topic, bb[0])
			}
			if len(bb) > 1 {
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
		res.Status = 404
		res.SetBodyString("File(%s) error: %s", file, err)
	} else {
		res.Status = 200
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

func auth(s *ServerHandler, msg *Message, sess *Session, msgType *int) bool {
	return true
}

func findMQ(s *ServerHandler, req *Message, sess *Session, msgType *int) *MessageQueue {
	topic := req.Topic()
	if topic == "" {
		reply(400, req.Id(), "Missing topic", sess, msgType)
		return nil
	}
	mq := s.server.MqTable[topic]
	if mq == nil {
		body := fmt.Sprintf("Topic(%s) not found", topic)
		reply(404, req.Id(), body, sess, msgType)
		return nil
	}
	return mq
}

func produceHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	mq := findMQ(s, req, sess, msgType)
	if mq == nil {
		return
	}

	mq.Write(req)

	if req.Ack() {
		body := fmt.Sprintf("%d", CurrMillis())
		reply(200, req.Id(), body, sess, msgType)
	}
}

func consumeHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	mq := findMQ(s, req, sess, msgType)
	if mq == nil {
		return
	}
	group := req.ConsumeGroup()
	if group == "" {
		group = mq.Name()
	}

	resp, status, err := mq.Read(group)
	if err != nil {
		resp = NewMessageStatus(status, err.Error())
		sess.WriteMessage(resp, msgType)
		return
	}
	if resp == nil {
		//to wait ... TODO!!!!
		return
	}

	resp.SetOriginId(resp.Id())
	resp.SetId(req.Id())
	if resp.Status == 0 {
		resp.SetOriginUrl(resp.Url)
	}
	resp.Status = 200
	sess.WriteMessage(resp, msgType)
}

func declareHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	topic := req.Topic()
	if topic == "" {
		reply(400, req.Id(), "Missing topic", sess, msgType)
		return
	}
	g := &ConsumeGroup{}
	g.LoadFrom(req)
	declareGroup := g.GroupName != ""
	if g.GroupName == "" {
		g.GroupName = topic
	}

	var err error
	var info interface{}

	mq := s.server.MqTable[topic]
	if mq == nil {
		mq, err = NewMessageQueue(s.server.MqDir, topic)
		if err != nil {
			body := fmt.Sprintf("Delcare Topic error: %s", err.Error())
			reply(500, req.Id(), body, sess, msgType)
			return
		}
		mq.SetCreator(req.Token()) //token as creator, TODO
		mask := req.TopicMask()
		if mask != nil {
			mq.SetMask(*mask)
		}
		s.server.MqTable[topic] = mq

		info, err = mq.DeclareGroup(g)
		if err != nil {
			body := fmt.Sprintf("Delcare ConsumeGroup error: %s", err.Error())
			reply(500, req.Id(), body, sess, msgType)
			return
		}
	} else {
		mask := req.TopicMask()
		if mask != nil {
			mq.SetMask(*mask)
		}

		if declareGroup {
			info, err = mq.DeclareGroup(g)
			if err != nil {
				body := fmt.Sprintf("Delcare ConsumeGroup error: %s", err.Error())
				reply(500, req.Id(), body, sess, msgType)
				return
			}
		}
	}

	if !declareGroup {
		info = mq.TopicInfo()
	}

	protocol.AddServerContext(info, s.server.ServerAddress)
	data, _ := json.Marshal(info)
	replyJson(200, req.Id(), string(data), sess, msgType)
}

func queryHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	mq := findMQ(s, req, sess, msgType)
	if mq == nil {
		return
	}

	var info interface{}
	group := req.ConsumeGroup()
	if group == "" {
		info = mq.TopicInfo()
	} else {
		groupInfo := mq.GroupInfo(group)
		if groupInfo == nil {
			body := fmt.Sprintf("ConsumeGroup(%s) not found", group)
			reply(404, req.Id(), body, sess, msgType)
			return
		}
		info = groupInfo
	}

	protocol.AddServerContext(info, s.server.ServerAddress)
	data, _ := json.Marshal(info)
	replyJson(200, req.Id(), string(data), sess, msgType)
}

func removeHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	mq := findMQ(s, req, sess, msgType)
	if mq == nil {
		return
	}
	topic := mq.Name()
	group := req.ConsumeGroup()
	if group == "" {
		delete(s.server.MqTable, mq.Name())
		err := mq.Destroy()
		if err != nil {
			body := fmt.Sprintf("Remove topic(%s) error: %s", topic, err.Error())
			reply(500, req.Id(), body, sess, msgType)
			return
		}
	} else {
		if mq.ConsumeGroup(group) == nil {
			body := fmt.Sprintf("ConsumeGroup(%s) not found", group)
			reply(404, req.Id(), body, sess, msgType)
			return
		}

		err := mq.RemoveGroup(group)
		if err != nil {
			body := fmt.Sprintf("Remove ConsumeGroup(%s) error: %s", group, err.Error())
			reply(500, req.Id(), body, sess, msgType)
			return
		}
	}

	reply(200, req.Id(), fmt.Sprintf("%d", CurrMillis()), sess, msgType)
}

func emptyHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	reply(500, req.Id(), "Not Implemented", sess, msgType)
}

func serverHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}

	res := NewMessage()
	res.Status = 200
	info := s.server.serverInfo()

	protocol.AddServerContext(info, s.server.ServerAddress)
	data, _ := json.Marshal(info)
	replyJson(200, req.Id(), string(data), sess, msgType)
}

func reply(status int, msgid string, body string, sess *Session, msgType *int) {
	resp := NewMessageStatus(status, body)
	resp.SetId(msgid)
	sess.WriteMessage(resp, msgType)
}

func replyJson(status int, msgid string, body string, sess *Session, msgType *int) {
	resp := NewMessageStatus(status)
	resp.SetId(msgid)
	resp.SetJsonBody(body)
	sess.WriteMessage(resp, msgType)
}
