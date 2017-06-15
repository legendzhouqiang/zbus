package main

import (
	"log"
)

func main() {
	m, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		log.Println(err)
	}
	defer m.Close()

	println("done")
}
