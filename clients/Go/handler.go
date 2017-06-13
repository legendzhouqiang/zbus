package main

import "log"

//ServerSessionHandler manages session from clients
type ServerSessionHandler struct {
	SessionTable map[string]Session
}

//NewServerSessionHandler create ServerSessionHandler
func NewServerSessionHandler() *ServerSessionHandler {
	return &ServerSessionHandler{
		SessionTable: make(map[string]Session),
	}
}

//Created when new session from client joined
func (s *ServerSessionHandler) Created(sess *Session) {
	log.Printf("Session(%s) Created", sess)
}

//ToDestroy when connection from client going to close
func (s *ServerSessionHandler) ToDestroy(sess *Session) {
	log.Printf("Session(%s) Destroyed", sess)
}

//OnError when socket error occured
func (s *ServerSessionHandler) OnError(err error, sess *Session) {
	log.Printf("Session(%s) Error: %s", sess, err)
}

//OnMessage when message available on socket
func (s *ServerSessionHandler) OnMessage(msg *Message, sess *Session, msgType *int) {
	res := NewMessage()
	res.Status = "200"
	res.SetBodyString("Hello World")
	sess.WriteMessage(res, msgType)
}
