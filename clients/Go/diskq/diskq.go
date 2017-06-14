package main

import (
	"log"
)

func main() {
	mb, err := NewMBuf("/tmp/mmap", 1024)
	if err != nil {
		log.Println(err)
	}
	defer mb.Close()

	value, _ := mb.GetInt16()
	println(value)

	println("done")
}
