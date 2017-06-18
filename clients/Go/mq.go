package main

import (
	"path/filepath"

	"bytes"

	"./diskq"
	"./protocol"
)

//MessageQueue based on DiskQueue
type MessageQueue struct {
	*diskq.DiskQueue
}

//NewMessageQueue create a message queue
func NewMessageQueue(baseDir string, name string) (*MessageQueue, error) {
	dq, err := diskq.NewDiskQueue(filepath.Join(baseDir, name))
	if err != nil {
		return nil, err
	}
	q := &MessageQueue{dq}
	return q, nil
}

//Produce a message to MQ
func (mq *MessageQueue) Produce(msg *Message) error {
	data := &diskq.DiskMsg{}
	data.Id = msg.Header[protocol.Id]
	data.Tag = msg.Header[protocol.Tag]
	buf := new(bytes.Buffer)
	msg.EncodeMessage(buf)
	data.Body = buf.Bytes()

	_, err := mq.Write(data)
	return err
}

//Consume a message from MQ
func (mq *MessageQueue) Consume(group string) (*Message, error) {
	data, err := mq.Read(group)
	if err != nil {
		return nil, err
	}
	if data == nil {
		return nil, nil
	}
	buf := bytes.NewBuffer(data.Body)
	return DecodeMessage(buf), nil
}

//DeclareGroup create/update a consume group
func (mq *MessageQueue) DeclareGroup(group ConsumeGroup) (*protocol.ConsumeGroupInfo, error) {
	return nil, nil
}

//RemoveGroup remove a consume group
func (mq *MessageQueue) RemoveGroup(group string) error {
	return nil
}

//TopicInfo returns message queue info
func (mq *MessageQueue) TopicInfo() (*protocol.TopicInfo, error) {
	return nil, nil
}

//GroupInfo returns consume group info
func (mq *MessageQueue) GroupInfo(group string) (*protocol.ConsumeGroupInfo, error) {
	return nil, nil
}

//Destroy MQ
func (mq *MessageQueue) Destroy() {

}

//ConsumeGroup consume group info
type ConsumeGroup struct {
	GroupName string
	Filter    string //filter on message'tag
	Mask      int32

	StartCopy   string //create group from another group
	StartOffset int64
	StartMsgId  string //create group start from offset, msgId to validate
	StartTime   int64  //create group start from time

	//only used in server side, TODO
	Creator string
}

//NewConsumeGroup create consume group
func NewConsumeGroup() *ConsumeGroup {
	g := &ConsumeGroup{}
	g.Mask = -1
	g.StartOffset = -1
	g.StartTime = -1
	return g
}
