package main

import (
	"bytes"
	"net"
)

//MessageClient TCP client using Message type
type MessageClient struct {
	conn       *net.TCPConn
	address    string
	sslEnabled bool
	certFile   string
	bufRead    *bytes.Buffer

	msgTable map[string]*Message
}

//NewMessageClient create message client, if sslEnabled, certFile should be provided
func NewMessageClient(address string, certFile *string) *MessageClient {
	c := &MessageClient{}
	c.address = address
	c.bufRead = new(bytes.Buffer)
	c.msgTable = make(map[string]*Message)
	if certFile != nil {
		c.sslEnabled = true
		c.certFile = *certFile
	}
	return c
}

//Connect to server
func (c *MessageClient) Connect() error {
	if c.conn != nil {
		return nil
	}
	conn, err := net.Dial("tcp", c.address)
	if err != nil {
		return err
	}
	c.conn = conn.(*net.TCPConn)

	return nil
}

//Close client
func (c *MessageClient) Close() {
	if c.conn == nil {
		return
	}
	c.conn.Close()
	c.conn = nil
}

//Send Message
func (c *MessageClient) Send(req *Message) error {
	c.Connect() //connect if needs

	if req.Id() == "" {
		req.SetId(uuid())
	}
	buf := new(bytes.Buffer)
	req.EncodeMessage(buf)

	data := buf.Bytes()
	for {
		n, err := c.conn.Write(data)
		if err != nil {
			return err
		}
		if n >= len(data) {
			break
		}
		data = data[n:]
	}
	return nil
}

//Recv Message
func (c *MessageClient) Recv(msgid *string) (*Message, error) {
	c.Connect() //connect if needs
	for {
		if msgid != nil {
			msg := c.msgTable[*msgid]
			if msg != nil {
				delete(c.msgTable, *msgid)
				return msg, nil
			}
		}
		data := make([]byte, 10240)
		n, err := c.conn.Read(data)
		if err != nil {
			return nil, err
		}
		c.bufRead.Write(data[0:n])
		resp := DecodeMessage(c.bufRead)
		if resp == nil {
			bufRead2 := new(bytes.Buffer)
			bufRead2.Write(c.bufRead.Bytes())
			c.bufRead = bufRead2
			continue
		}

		respId := resp.Id()
		if msgid == nil || respId == "" || respId == *msgid {
			return resp, nil
		}
		c.msgTable[respId] = resp
	}
}
