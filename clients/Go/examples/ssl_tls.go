package main

import (
	"bufio"
	"crypto/tls"
	"io"
	"log"
	"net"
	"os"
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

func main2() {

	logFile, err := os.OpenFile("/tmp/log.txt", os.O_CREATE|os.O_APPEND|os.O_RDWR, 0666)
	mw := io.MultiWriter(os.Stdout, logFile)
	log.SetOutput(mw)
	log.SetFlags(log.Lshortfile | log.Ldate | log.Ltime)

	cert, err := tls.LoadX509KeyPair("zbus.crt", "zbus.key")
	if err != nil {
		log.Println(err)
		return
	}
	config := &tls.Config{Certificates: []tls.Certificate{cert}}
	server, err := tls.Listen("tcp", ":15555", config)
	log.Println("Server listening at 0.0.0.0:15555")
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
