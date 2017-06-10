package main

import (
	"bytes"
	"fmt"
	"os"
	"strconv"
	"strings"
)

//Message stands for HTTP message including Request and Response
type Message struct {
	Status string
	Url    string
	Method string
	Header map[string]string

	body []byte
}

//NewMessage creates a Message
func NewMessage() *Message {
	m := new(Message)
	m.Status = ""
	m.Url = "/"
	m.Method = "GET"
	m.Header = make(map[string]string)
	return m
}

//SetBody set binary body of Message
func (m *Message) SetBody(body []byte) {
	m.body = body
}

//SetBodyString set string body of Message
func (m *Message) SetBodyString(body string) {
	m.body = []byte(body)
}

//EncodeMessage encodes Message to []byte
func (m *Message) EncodeMessage(buf *bytes.Buffer) {
	if m.Status != "" {
		buf.WriteString(fmt.Sprintf("HTTP/1.1 %s %s\r\n", m.Status, "OK"))
	} else {
		buf.WriteString(fmt.Sprintf("%s %s HTTP/1.1\r\n", m.Method, m.Url))
	}
	for k, v := range m.Header {
		k = strings.ToLower(k)
		if k == "content-length" {
			continue
		}
		buf.WriteString(fmt.Sprintf("%s: %s\r\n", k, v))
	}
	bodyLen := 0
	if m.body != nil {
		bodyLen = len(m.body)
	}
	buf.WriteString(fmt.Sprintf("content-length: %d\r\n", bodyLen))

	buf.WriteString("\r\n")
	if m.body != nil {
		buf.Write(m.body)
	}
}

//DecodeMessage decode Message from Buffer, nil returned if not enought in buffer
func DecodeMessage(buf *bytes.Buffer) *Message {
	bb := buf.Bytes()
	idx := bytes.Index(bb, []byte("\r\n\r\n"))
	if idx == -1 {
		return nil
	}
	m := NewMessage()
	header := bytes.Split(bb[:idx], []byte("\r\n"))
	meta := string(header[0])
	metaFields := strings.Fields(meta)
	if strings.HasPrefix(strings.ToUpper(metaFields[0]), "HTTP") {
		m.Status = metaFields[1]
	} else {
		m.Method = metaFields[0]
		m.Url = metaFields[1]
	}
	for i := 1; i < len(header); i++ {
		s := string(header[i])
		kv := strings.Split(s, ":")
		key := strings.ToLower(strings.TrimSpace(kv[0]))
		val := strings.TrimSpace(kv[1])
		m.Header[key] = val
	}
	bodyLen := 0
	if lenStr, ok := m.Header["content-length"]; ok {
		bodyLen, _ = strconv.Atoi(lenStr)
	}
	if (buf.Len() - idx - 4) < bodyLen {
		return nil
	}
	if bodyLen > 0 {
		m.SetBody(bb[idx+4 : idx+4+bodyLen])
	}
	return m
}

func main() {
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
