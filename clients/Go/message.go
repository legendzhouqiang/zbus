package main

import (
	"bytes"
	"flag"
	"fmt"
	"log"
	"net"
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
	m.Header["connection"] = "Keep-Alive"
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
	data := make([]byte, idx+4+bodyLen)
	buf.Read(data)
	return m
}

func handleConnection(conn net.Conn) {
	defer conn.Close()
	bufRead := new(bytes.Buffer)
	for {
		data := make([]byte, 1024)
		n, err := conn.Read(data)
		if err != nil {
			log.Println("Error reading: ", err.Error())
			break
		}
		bufRead.Write(data[0:n])

		for {
			req := DecodeMessage(bufRead)
			if req == nil {
				bufRead2 := new(bytes.Buffer)
				bufRead2.Write(bufRead.Bytes())
				bufRead = bufRead2
				break
			}

			res := NewMessage()
			res.Status = "200"
			res.SetBodyString("Hello World")
			bufWrite := new(bytes.Buffer)
			res.EncodeMessage(bufWrite)
			conn.Write(bufWrite.Bytes())
		}
	}
	log.Println("Connection closed, ", conn)
}

func main() {
	var addr = *flag.String("h", "0.0.0.0:15555", "zbus server address")
	server, err := net.Listen("tcp", addr)
	if err != nil {
		log.Println("Error listening:", err.Error())
		return
	}
	defer server.Close()
	log.Println("Listening on " + addr)
	for {
		conn, err := server.Accept()
		if err != nil {
			log.Println("Error accepting: ", err.Error())
			return
		}
		go handleConnection(conn)
	}
}
