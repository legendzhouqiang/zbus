package main

import (
	"bytes"
	"fmt"
	"strconv"
	"strings"

	"./protocol"
)

var statusText = map[int]string{
	100: "Continue",
	101: "Switching Protocols",
	200: "OK",
	400: "Bad Request",
	401: "Unauthorized",
	403: "Forbidden",
	404: "Not Found",
	405: "Method Not Allowed",
	500: "Internal Server Error",
	502: "Bad Gateway",
	503: "Service Unavailable",
	504: "Gateway Timeout",
}

// StatusText returns a text for the HTTP status code.
func StatusText(code int) string {
	value, ok := statusText[code]
	if !ok {
		return "Unkown Status"
	}
	return value
}

//Message stands for HTTP message including Request and Response
type Message struct {
	Status int
	Url    string
	Method string
	Header map[string]string

	body []byte
}

//NewMessage creates a Message
func NewMessage() *Message {
	m := new(Message)
	m.Url = "/"
	m.Method = "GET"
	m.Header = make(map[string]string)
	m.Header["connection"] = "Keep-Alive"
	return m
}

//NewMessageStatus create message with status and body
func NewMessageStatus(status int, body string) *Message {
	m := NewMessage()
	m.Status = status
	m.SetBodyString(body)
	return m
}

//SetHeaderIfNone updates header by value if not set yet
func (m *Message) SetHeaderIfNone(key string, val string) {
	if _, ok := m.Header[key]; ok {
		return
	}
	m.Header[key] = val
}

//SetBody set binary body of Message
func (m *Message) SetBody(body []byte) {
	m.body = body
}

//SetBodyString set string body of Message
func (m *Message) SetBodyString(body string) {
	m.body = []byte(body)
}

//SetJsonBody set json body
func (m *Message) SetJsonBody(body string) {
	m.body = []byte(body)
	m.Header["content-type"] = "application/json"
}

//EncodeMessage encodes Message to []byte
func (m *Message) EncodeMessage(buf *bytes.Buffer) {
	if m.Status != 0 {
		buf.WriteString(fmt.Sprintf("HTTP/1.1 %d %s\r\n", m.Status, StatusText(m.Status)))
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

//String convert message to string
func (m *Message) String() string {
	buf := new(bytes.Buffer)
	m.EncodeMessage(buf)
	return string(buf.Bytes())
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
		m.Status, _ = strconv.Atoi(metaFields[1])
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
	data := make([]byte, idx+4+bodyLen)
	buf.Read(data)
	return m
}

//Ack return whether ack header set or not, default to true
func (m *Message) Ack() bool {
	ack := m.Header[protocol.Ack]
	if ack == "" {
		return true //default to ack if not set
	}
	boolAck, err := strconv.ParseBool(ack)
	if err != nil {
		return false
	}
	return boolAck
}

//SetAck set ack value to header
func (m *Message) SetAck(ack bool) {
	m.Header[protocol.Ack] = fmt.Sprintf("%v", ack)
}
