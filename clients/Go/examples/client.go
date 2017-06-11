package main

import (
	"flag"
	"log"
	"net/http"

	"fmt"
)

var addr = flag.String("addr", "0.0.0.0:80", "http service address")

func home(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "Hello World")
}

func main() {
	flag.Parse()
	log.SetFlags(0)

	http.HandleFunc("/", home)
	http.ListenAndServe(*addr, nil)
}
