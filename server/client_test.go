package main

import (
	"fmt"
	"testing"

	"encoding/json"

	"./proto"
)

func TestNewMessageClient(t *testing.T) {
	c := NewMessageClient("localhost:15555", nil)

	err := c.Connect()

	if err != nil {
		t.Fail()
	}

	c.Close()
}

func TestMessageClient_Send(t *testing.T) {
	c := NewMessageClient("localhost:15555", nil)
	defer c.Close()

	var err error
	req := NewMessage()
	req.Url = "/server"

	err = c.Send(req)
	println(req.Id())
	if err != nil {
		t.Fail()
	}

	msg, err := c.Recv(nil)
	if err != nil {
		t.Fail()
	}

	println(msg.String())
}

func TestMessageClient_Timeout(t *testing.T) {
	c := NewMessageClient("localhost:15555", nil)
	defer c.Close()

	_, err := c.Recv(nil)
	fmt.Println(err)
}

func TestMessageClient_Invoke(t *testing.T) {
	c := NewMessageClient("localhost:15555", nil)
	msg := NewMessage()
	msg.SetCmd(proto.Tracker)

	resp, err := c.Invoke(msg)
	if err != nil {
		t.Fail()
	}
	fmt.Println(resp)
}

func TestMessageClient_Close(t *testing.T) {
	c := NewMessageClient("localhost:15550", nil)

	notify := make(chan bool)
	c.EnsureConnected(notify)

	c.Close()
	<-notify
}

func TestMqClient_QueryTracker(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.QueryTracker()
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_QueryServer(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.QueryServer()
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_QueryTopic(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.QueryTopic("hong")
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}

func TestMqClient_QueryConsumeGroup(t *testing.T) {
	c := NewMqClient("localhost:15555", nil)
	info, err := c.QueryGroup("hong", "hong")
	if err != nil {
		println(err)
		t.Fail()
	}
	s, _ := json.Marshal(info)
	fmt.Println(string(s))
	c.Close()
}
