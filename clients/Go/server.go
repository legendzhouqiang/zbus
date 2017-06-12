package main

import (
	"bytes"
	"flag"
	"log"
	"net"

	"./websocket"
)

var upgrader = Upgrader{}

func handleConnection(conn net.Conn) {
	defer conn.Close()
	bufRead := new(bytes.Buffer)
	var wsConn *websocket.Conn

outter:
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

			log.Println(req.ToString())

			//upgrade to Websocket if requested
			if tokenListContainsValue(req.Header, "connection", "upgrade") {
				wsConn, err = upgrader.Upgrade(conn, req)
				if err == nil {
					break outter
				}
			}

			res := NewMessage()
			res.Status = "200"
			res.SetBodyString("Hello World")
			bufWrite := new(bytes.Buffer)
			res.EncodeMessage(bufWrite)
			conn.Write(bufWrite.Bytes())
		}
	}

	if wsConn != nil { //upgrade to Websocket
		for {
			msgtype, message, err := wsConn.ReadMessage()
			if err != nil {
				log.Println("read:", err)
				break
			}
			log.Printf("recv: %s", message)
			err = wsConn.WriteMessage(msgtype, message)
			if err != nil {
				log.Println("write:", err)
				break
			}
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
