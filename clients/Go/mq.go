package main

import (
	"./diskq"
)

//MessageQueue based on DiskQueue
type MessageQueue struct {
	*diskq.DiskQueue
}

//Produce a message to MQ
func (mq *MessageQueue) Produce(msg *Message) {

}

//Consume a message from MQ
func (mq *MessageQueue) Consume(group string) *Message {
	return nil
}

//Destroy MQ
func (mq *MessageQueue) Destroy() {

}
