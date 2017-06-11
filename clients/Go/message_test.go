package main

import (
	"bytes"
	"os"
	"testing"
)

func TestEncodeMessage(t *testing.T) {
	m := NewMessage()
	m.Status = "200"
	m.Header["cmd"] = "produce"
	m.Header["topic"] = "MyTopic"
	m.SetBodyString("Hello World")

	buf := new(bytes.Buffer)
	m.EncodeMessage(buf)
	m2 := DecodeMessage(buf)
	buf2 := new(bytes.Buffer)
	m2.EncodeMessage(buf2)

	buf2.WriteTo(os.Stdout)
}
