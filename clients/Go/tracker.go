package main

import (
	"encoding/json"

	"./protocol"
)

//Tracker tracks MqServers
type Tracker struct {
	infoVersion int64
}

//NewTracker create Tracker
func NewTracker() *Tracker {
	t := &Tracker{}
	t.infoVersion = CurrMillis()

	return t
}

func trackerHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	info := s.server.trackerInfo()
	protocol.AddServerContext(info, s.server.ServerAddress)
	data, _ := json.Marshal(info)
	reply(200, req.Id(), string(data), sess, msgType)
}

func trackPubHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	reply(500, req.Id(), "Not Implemented", sess, msgType)
}

func trackSubHandler(s *ServerHandler, req *Message, sess *Session, msgType *int) {
	if !auth(s, req, sess, msgType) {
		return
	}
	info := s.server.trackerInfo()
	data, _ := json.Marshal(info)
	reply(200, req.Id(), string(data), sess, msgType)
}
