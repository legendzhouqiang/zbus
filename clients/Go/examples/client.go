package main

import (
	"bufio"
	"crypto/tls"
	"log"
	"net"
)

func handleConnection(conn net.Conn) {
	defer conn.Close()
	r := bufio.NewReader(conn)
	for {
		msg, err := r.ReadString('\n')
		if err != nil {
			log.Println(err)
			return
		}
		println(msg)

		n, err := conn.Write([]byte("HTTP/1.1 200 OK\r\n\r\nHello World"))
		if err != nil {
			log.Println(n, err)
			return
		}
	}
}

func main() {
	m := make(map[string]string)
	m["hong"] = "leiming"

	x, e := m["yyy"]
	print(x)
	print(e)

	log.SetFlags(log.Lshortfile)
	cert, err := tls.LoadX509KeyPair("zbus.crt", "zbus.key")
	if err != nil {
		log.Println(err)
		return
	}
	config := &tls.Config{Certificates: []tls.Certificate{cert}}
	server, err := tls.Listen("tcp", ":15555", config)
	if err != nil {
		log.Println(err)
		return
	}
	defer server.Close()

	for {
		conn, err := server.Accept()
		if err != nil {
			log.Println(err)
			continue
		}
		go handleConnection(conn)
	}
}
