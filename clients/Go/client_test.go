package main

import "testing"

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
	err := c.Connect()
	if err != nil {
		t.Fail()
	}
	defer c.Close()

	req := NewMessage()
	req.Url = "/server"

	err = c.Send(req)
	if err != nil {
		t.Fail()
	}

	msg, err := c.Recv()
	if err != nil {
		t.Fail()
	}

	println(msg.String())
}
