package main

import (
	"fmt"
	"testing"
)

var mq, _ = NewMessageQueue("/tmp/diskq", "hong")

func TestNewMessageQueue(t *testing.T) {
	q, err := NewMessageQueue("/tmp/diskq", "hong")
	if err != nil {
		t.Fail()
	}
	defer q.Close()
}

func TestMessageQueue_Write(t *testing.T) {
	msg := NewMessage()
	msg.Header["cmd"] = "produce"
	msg.SetBodyString("hello world")
	err := mq.Write(msg)
	if err != nil {
		t.Fail()
	}
}

func TestMessageQueue_Read(t *testing.T) {

	msg, err := mq.Read("hong")
	if err != nil {
		t.Fail()
	}
	if msg != nil {
		fmt.Println(msg)
	}
}

func TestMessageQueue_ConsumeGroup(t *testing.T) {
	g := &ConsumeGroup{}
	g.Mask = 0
	g.GroupName = "hong"
	fmt.Println(g.Mask)
}
