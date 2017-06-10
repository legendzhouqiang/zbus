package main

import (
	"flag"
	"log"
	"net/http"

	"fmt"

	"github.com/gorilla/websocket"
)

var addr = flag.String("addr", "localhost:8080", "http service address")

var upgrader = websocket.Upgrader{}

func home(w http.ResponseWriter, r *http.Request) {
	if _, ok := r.Header["Upgrade"]; ok {
		c, err := upgrader.Upgrade(w, r, nil)
		if err != nil {
			log.Print("upgrade:", err)
			return
		}
		defer c.Close()
		for {
			mt, message, err := c.ReadMessage()
			if err != nil {
				log.Println("read:", err)
				break
			}
			log.Printf("recv: %s", message)
			err = c.WriteMessage(mt, message)
			if err != nil {
				log.Println("write:", err)
				break
			}
		}
		return
	}

	fmt.Fprintf(w, "Hello World")
}

func main2() {
	flag.Parse()
	log.SetFlags(0)

	http.HandleFunc("/", home)
	http.ListenAndServe(*addr, nil)
}
