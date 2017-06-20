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

//LoadMqTable load MQ from based directory
func LoadMqTable(baseDir string) (map[string]*MessageQueue, error) {
	files, err := ioutil.ReadDir(baseDir)
	if err != nil {
		return nil, err
	}

	table := make(map[string]*MessageQueue)
	for _, file := range files {
		if !file.IsDir() {
			continue //ignore
		}
		fileName := file.Name()
		idxFilePath := filepath.Join(baseDir, fileName, fmt.Sprintf("%s%s", fileName, diskq.IndexSuffix))
		if _, err = os.Stat(idxFilePath); err != nil {
			continue //invalid directory
		}
		q, err := NewMessageQueue(baseDir, fileName)
		if err != nil {
			log.Printf("Load MQ(%s) failed, error: %s", fileName, err)
			continue
		}
		table[fileName] = q
	}

	return table, nil
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
	if q.index != nil {
		q.index.Close()
		q.index = nil
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

//WriteBatch write multiple message in on batch
func (q *MessageQueue) WriteBatch(msgs []*Message) error {
	if len(msgs) <= 0 {
		return nil
	}

	dmsgs := make([]*diskq.DiskMsg, len(msgs))
	for i := 0; i < len(msgs); i++ {
		msg := msgs[i]
		data := &diskq.DiskMsg{}
		data.Id = msg.Header[protocol.Id]
		data.Tag = msg.Header[protocol.Tag]
		buf := new(bytes.Buffer)
		msg.EncodeMessage(buf)
		data.Body = buf.Bytes()
		dmsgs[i] = data
	}
	_, err := q.writer.WriteBatch(dmsgs)
	return err
}

//Read a message from MQ' consume group
func (q *MessageQueue) Read(group string) (*Message, int, error) {
	if group == "" {
		group = q.name //default to topic name
	}
	r := q.readers[group]
	if r == nil {
		return nil, 404, fmt.Errorf("ConsumeGroup(%s) not found", group)
	}
	data, err := r.Read()
	if err != nil {
		return nil, 500, err
	}
	if data == nil {
		return nil, 200, nil
	}
	buf := bytes.NewBuffer(data.Body)
	return DecodeMessage(buf), 200, nil
}

//DeclareGroup create/update a consume group
func (q *MessageQueue) DeclareGroup(group *ConsumeGroup) (*protocol.ConsumeGroupInfo, error) {
	groupName := group.GroupName
	if groupName == "" {
		groupName = q.name
	}
	g, _ := q.readers[groupName]
	if g == nil { //Create new consume group reader
		var g2 *diskq.QueueReader
		var err error
		if group.StartCopy != nil {
			g2, _ = q.readers[*group.StartCopy]
		}
		if g2 == nil {
			g2 = q.findLatesReader()
		}
		if g2 == nil {
			g, err = diskq.NewQueueReader(q.index, groupName)
			if err != nil {
				return nil, err
			}
		} else { //copy g2
			g, err = diskq.NewQueueReaderCopy(g2, groupName)
			if err != nil {
				return nil, err
			}
		}
		q.readers[groupName] = g
	}

	if group.Filter != nil {
		g.SetFilter(*group.Filter)
	}
	if group.Mask != nil {
		g.SetMask(*group.Mask)
	}
	//TODO SEEK by start

	return q.groupInfo(g), nil
}

//RemoveGroup remove a consume group
func (q *MessageQueue) RemoveGroup(group string) error {
	g, _ := q.readers[group]
	if g == nil {
		return nil
	}
	delete(q.readers, group)
	groupFile := g.File()
	g.Close()
	return os.Remove(groupFile)
}

//Destroy MQ
func (q *MessageQueue) Destroy() error {
	dir := q.index.Dir()
	q.Close()
	return os.RemoveAll(dir)
}

//TopicInfo returns message queue info
func (q *MessageQueue) TopicInfo() *protocol.TopicInfo {
	info := &protocol.TopicInfo{}
	info.TopicName = q.name
	info.Mask = q.index.Mask()
	info.MessageDepth = q.index.MsgNo()
	info.Creator = q.index.Creator()
	info.CreatedTime = q.index.CreatedTime()
	info.LastUpdatedTime = q.index.UpdatedTime()
	for _, r := range q.readers {
		groupInfo := q.groupInfo(r)
		info.ConsumeGroupList = append(info.ConsumeGroupList, groupInfo)
	}
	//TODO ConsumerCount missing
	return info
}

//GroupInfo returns consume group info
func (q *MessageQueue) GroupInfo(group string) *protocol.ConsumeGroupInfo {
	g, _ := q.readers[group]
	if g != nil {
		return q.groupInfo(g)
	}
	return nil
}

func (q *MessageQueue) groupInfo(g *diskq.QueueReader) *protocol.ConsumeGroupInfo {
	info := &protocol.ConsumeGroupInfo{}
	info.TopicName = q.name
	info.GroupName = g.Name()
	info.Mask = g.Mask()
	info.Filter = g.Filter()
	info.MessageCount = g.MsgCount()
	info.Creator = g.Creator()
	info.CreatedTime = g.CreatedTime()
	info.LastUpdatedTime = g.UpdatedTime()

	//info.ConsumerCount and info.ConsumerList missing
	return info
}

func (q *MessageQueue) findLatesReader() *diskq.QueueReader {
	var t *diskq.QueueReader
	for _, r := range q.readers {
		if t == nil {
			t = r
			continue
		}

		if r.BlockNo() < t.BlockNo() {
			continue
		}
		if r.BlockNo() > t.BlockNo() {
			t = r
			continue
		}
		if r.MsgNo() >= t.MsgNo() {
			t = r
		}
	}
	return t
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

//Name return the name of MQ
func (q *MessageQueue) Name() string {
	return q.name
}

//ConsumeGroup returns the reader of the group
func (q *MessageQueue) ConsumeGroup(group string) *diskq.QueueReader {
	return q.readers[group]
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
	Filter    *string //filter on message'tag
	Mask      *int32

	StartCopy   *string //create group from another group
	StartOffset *int64
	StartMsgid  *string //create group start from offset, msgId to validate
	StartTime   *int64  //create group start from time

	//only used in server side, TODO
	Creator *string
}

//WriteTo message
func (g *ConsumeGroup) WriteTo(m *Message) {
	m.SetConsumeGroup(g.GroupName)
	if g.Filter != nil {
		m.SetGroupFilter(*g.Filter)
	}
	if g.Mask != nil {
		m.SetGroupMask(*g.Mask)
	}
	if g.StartCopy != nil {
		m.SetGroupStartCopy(*g.StartCopy)
	}
	if g.StartMsgid != nil {
		m.SetGroupStartMsgid(*g.StartMsgid)
	}
	if g.StartOffset != nil {
		m.SetGroupStartOffset(*g.StartOffset)
	}
	if g.StartTime != nil {
		m.SetGroupStartTime(*g.StartTime)
	}
}

//LoadFrom message
func (g *ConsumeGroup) LoadFrom(m *Message) {
	g.GroupName = m.ConsumeGroup()
	g.Filter = m.GroupFilter()
	g.Mask = m.GroupMask()
	g.StartCopy = m.GroupStartCopy()
	g.StartMsgid = m.GroupStartMsgid()
	g.StartOffset = m.GroupStartOffset()
	g.StartTime = m.GroupStartTime()
}
