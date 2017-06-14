package main

import (
	"fmt"
	"log"
	"os"
	"time"
)

const (
	MsgIdMaxLen  = 39
	MsgTagMaxLen = 127
	MsgBodyPos   = 8 + 8 + 40 + 8 + 8 + 128 //200

	MappedFileSize = 1024
	CreatorPos     = 512 - 128 //creator max length 127(another 1 byte for length)
	CreatedTimePos = CreatorPos - 8
	UpdatedTimePos = CreatorPos - 16
	MaskPos        = CreatorPos - 20

	ExtItemSize  = 128
	ExtItemCount = 4
	ExtOffset    = MappedFileSize - ExtItemSize*ExtItemCount
)

//DiskMessage to read and write in disk for DiskQ
type DiskMessage struct {
	Offset     int64
	Timestamp  int64
	Id         string // write 40 = 1_len + max 39
	CorrOffset int64
	MsgNo      int64
	Tag        string // write 128 = 1_len + max 127
	Body       []byte // write 4_len + body
}

//MappedFile mapping disk file for DiskQ index and reader index
type MappedFile struct {
	mask        int32
	creator     string
	createdTime int64
	updatedTime int64

	extensions []string

	mbuf *MBuf
}

//Mask get mask value
func (mfile *MappedFile) Mask() int32 {
	return mfile.mask
}

//SetMask set mask value
func (mfile *MappedFile) SetMask(value int32) {
	mfile.mask = value
	mfile.mbuf.SetPos(MaskPos)
	mfile.mbuf.PutInt32(value)
}

//CreatedTime get createdTime
func (mfile *MappedFile) CreatedTime() int64 {
	return mfile.createdTime
}

//SetCreatedTime set createdTime
func (mfile *MappedFile) SetCreatedTime(value int64) {
	mfile.createdTime = value
	mfile.mbuf.SetPos(CreatedTimePos)
	mfile.mbuf.PutInt64(value)
}

//UpdatedTime get updatedTime
func (mfile *MappedFile) UpdatedTime() int64 {
	return mfile.updatedTime
}

//SetUpdatedTime set updatedTime
func (mfile *MappedFile) SetUpdatedTime(value int64) {
	mfile.updatedTime = value
	mfile.mbuf.SetPos(UpdatedTimePos)
	mfile.mbuf.PutInt64(value)
}

//Creator get creator
func (mfile *MappedFile) Creator() string {
	return mfile.creator
}

//SetCreator set Creator
func (mfile *MappedFile) SetCreator(value string) error {
	n := len(value)
	if n > 127 {
		return fmt.Errorf("%s longer than 127", value)
	}
	mfile.creator = value
	mfile.mbuf.SetPos(CreatorPos)
	mfile.mbuf.PutByte(byte(n))
	mfile.mbuf.PutBytes([]byte(value))

	return nil
}

//NewMappedFile create mapped file
func NewMappedFile(fileName string) (*MappedFile, error) {
	newFile := true
	if _, err := os.Stat(fileName); err == nil {
		newFile = false
	}
	mbuf, err := NewMBuf(fileName, MappedFileSize)
	if err != nil {
		return nil, err
	}
	this := &MappedFile{}
	this.mbuf = mbuf
	if newFile {
		ts := time.Now().UnixNano() / int64(time.Millisecond)
		this.SetCreatedTime(ts)
		this.SetUpdatedTime(ts)
		this.extensions = make([]string, ExtItemCount)
	} else {
		mbuf.SetPos(CreatorPos)
		n, _ := mbuf.GetByte()
		data, _ := mbuf.GetBytes(int(n))
		this.creator = string(data)
		mbuf.SetPos(CreatedTimePos)
		this.createdTime, _ = mbuf.GetInt64()
		mbuf.SetPos(UpdatedTimePos)
		this.updatedTime, _ = mbuf.GetInt64()
		mbuf.SetPos(MaskPos)
		this.mask, _ = mbuf.GetInt32()
	}
	return this, nil
}

//Close mapped file
func (mfile *MappedFile) Close() error {
	return mfile.mbuf.Close()
}

func main() {
	m, err := NewMappedFile("/tmp/MyTopic.idx")
	if err != nil {
		log.Println(err)
	}
	defer m.Close()
	m.SetUpdatedTime(time.Now().UnixNano() / int64(time.Millisecond))

	println(m.Creator())
	println(m.CreatedTime())
	println(m.UpdatedTime())

	println("done")
}
