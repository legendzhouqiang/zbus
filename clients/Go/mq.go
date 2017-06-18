package main

import (
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
	"strings"

	"bytes"

	"./diskq"
	"./protocol"
)

//MessageQueue writer + N readers
type MessageQueue struct {
	index   *diskq.Index
	name    string
	writer  *diskq.QueueWriter
	readers map[string]*diskq.QueueReader
}

//NewMessageQueue create a message queue
func NewMessageQueue(baseDir string, name string) (*MessageQueue, error) {
	dirPath := filepath.Join(baseDir, name)
	index, err := diskq.NewIndex(dirPath)
	if err != nil {
		return nil, err
	}
	q := &MessageQueue{}
	q.index = index
	q.name = q.index.Name()
	q.writer, err = diskq.NewQueueWriter(index)
	if err != nil {
		q.Close()
		return nil, err
	}
	q.readers = make(map[string]*diskq.QueueReader)
	err = q.loadReaders()
	if err != nil {
		q.Close()
		return nil, err
	}
	return q, nil
}

//Close disk queue
func (q *MessageQueue) Close() {
	if q.writer != nil {
		q.writer.Close()
		q.writer = nil
	}
	for _, r := range q.readers {
		r.Close()
	}
	q.readers = make(map[string]*diskq.QueueReader)
}

//Produce a message to MQ
func (q *MessageQueue) Write(msg *Message) error {
	data := &diskq.DiskMsg{}
	data.Id = msg.Header[protocol.Id]
	data.Tag = msg.Header[protocol.Tag]
	buf := new(bytes.Buffer)
	msg.EncodeMessage(buf)
	data.Body = buf.Bytes()

	_, err := q.writer.Write(data)
	return err
}

//Read a message from MQ' consume group
func (q *MessageQueue) Read(group string) (*Message, error) {
	if group == "" {
		group = q.name //default to topic name
	}
	r, ok := q.readers[group]
	if !ok {
		return nil, fmt.Errorf("reader(%s) not found", group)
	}
	data, err := r.Read()
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
func (q *MessageQueue) DeclareGroup(group ConsumeGroup) (*protocol.ConsumeGroupInfo, error) {
	return nil, nil
}

//RemoveGroup remove a consume group
func (q *MessageQueue) RemoveGroup(group string) error {
	return nil
}

//TopicInfo returns message queue info
func (q *MessageQueue) TopicInfo() (*protocol.TopicInfo, error) {
	return nil, nil
}

//GroupInfo returns consume group info
func (q *MessageQueue) GroupInfo(group string) (*protocol.ConsumeGroupInfo, error) {
	return nil, nil
}

//Destroy MQ
func (q *MessageQueue) Destroy() {

}

func (q *MessageQueue) loadReaders() error {
	readerDir := q.index.ReaderDir()
	if err := os.MkdirAll(readerDir, 0644); err != nil {
		return err
	}
	files, err := ioutil.ReadDir(readerDir)
	if err != nil {
		return err
	}
	for _, file := range files {
		if file.IsDir() {
			continue //ignore
		}
		fileName := file.Name()
		if !strings.HasSuffix(fileName, diskq.ReaderSuffix) {
			continue
		}
		name := fileName[0 : len(fileName)-len(diskq.ReaderSuffix)]
		r, err := diskq.NewQueueReader(q.index, name)
		if err != nil {
			log.Printf("Reader %s load error: %s", fileName, err)
		}
		q.readers[name] = r
	}
	return nil
}

//SetMask update mask value
func (q *MessageQueue) SetMask(value int32) {
	q.index.SetMask(value)
}

//SetCreator update creator
func (q *MessageQueue) SetCreator(value string) {
	q.index.SetCreator(value)
}

//SetExt update ext
func (q *MessageQueue) SetExt(i int, value string) error {
	return q.index.SetExt(i, value)
}

//GetExt get ext
func (q *MessageQueue) GetExt(i int) (string, error) {
	return q.index.GetExt(i)
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
