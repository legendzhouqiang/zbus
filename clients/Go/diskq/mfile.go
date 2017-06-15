package main

import (
	"fmt"
	"os"
	"time"
)

const (
	MsgIdMaxLen  = 39
	MsgTagMaxLen = 127
	MsgBodyPos   = 8 + 8 + 40 + 8 + 8 + 128 //200

	HeaderSize     = 1024
	CreatorPos     = 512 - 128 //creator max length 127(another 1 byte for length)
	CreatedTimePos = CreatorPos - 8
	UpdatedTimePos = CreatorPos - 16
	MaskPos        = CreatorPos - 20

	ExtItemSize  = 128
	ExtItemCount = 4
	ExtOffset    = HeaderSize - ExtItemSize*ExtItemCount
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

	buf        *MBuf
	fileExists bool
}

//NewMappedFile create mapped file
func NewMappedFile(fileName string) (*MappedFile, error) {
	newFile := true
	if _, err := os.Stat(fileName); err == nil {
		newFile = false
	}
	buf, err := NewMBuf(fileName, HeaderSize)
	if err != nil {
		return nil, err
	}
	m := &MappedFile{}
	m.buf = buf
	m.extensions = make([]string, ExtItemCount)
	m.fileExists = !newFile
	if newFile {
		ts := time.Now().UnixNano() / int64(time.Millisecond)
		m.SetCreatedTime(ts)
		m.SetUpdatedTime(ts)
	} else {
		buf.SetPos(CreatorPos)
		m.creator, _ = buf.GetString()
		buf.SetPos(CreatedTimePos)
		m.createdTime, _ = buf.GetInt64()
		buf.SetPos(UpdatedTimePos)
		m.updatedTime, _ = buf.GetInt64()
		buf.SetPos(MaskPos)
		m.mask, _ = buf.GetInt32()

		for i := 0; i < ExtItemCount; i++ {
			m.buf.SetPos(int32(ExtOffset + ExtItemSize*i))
			m.extensions[i], _ = buf.GetString()
		}
	}
	return m, nil
}

//Close mapped file
func (m *MappedFile) Close() error {
	return m.buf.Close()
}

//Mask get mask value
func (m *MappedFile) Mask() int32 {
	return m.mask
}

//SetMask set mask value
func (m *MappedFile) SetMask(value int32) {
	m.mask = value
	m.buf.SetPos(MaskPos)
	m.buf.PutInt32(value)
}

//CreatedTime get createdTime
func (m *MappedFile) CreatedTime() int64 {
	return m.createdTime
}

//SetCreatedTime set createdTime
func (m *MappedFile) SetCreatedTime(value int64) {
	m.createdTime = value
	m.buf.SetPos(CreatedTimePos)
	m.buf.PutInt64(value)
}

//UpdatedTime get updatedTime
func (m *MappedFile) UpdatedTime() int64 {
	return m.updatedTime
}

//SetUpdatedTime set updatedTime
func (m *MappedFile) SetUpdatedTime(value int64) {
	m.updatedTime = value
	m.buf.SetPos(UpdatedTimePos)
	m.buf.PutInt64(value)
}

//Creator get creator
func (m *MappedFile) Creator() string {
	return m.creator
}

//SetCreator set Creator
func (m *MappedFile) SetCreator(value string) error {
	n := len(value)
	if n > 127 {
		return fmt.Errorf("%s longer than 127", value)
	}
	m.creator = value
	m.buf.SetPos(CreatorPos)
	m.buf.PutByte(byte(n))
	m.buf.PutBytes([]byte(value))

	return nil
}

//GetExt get extension value
func (m *MappedFile) GetExt(idx int) (string, error) {
	if idx < 0 || idx >= ExtItemCount {
		return "", fmt.Errorf("index(%d) out of range [0, %d)", idx, ExtItemCount)
	}
	return m.extensions[idx], nil
}

//SetExt set extension value
func (m *MappedFile) SetExt(idx int, value string) error {
	if idx < 0 || idx >= ExtItemCount {
		return fmt.Errorf("index(%d) out of range [0, %d)", idx, ExtItemCount)
	}
	n := len(value)
	if n >= ExtItemSize {
		return fmt.Errorf("%s longer than %d", value, ExtItemSize-1)
	}

	m.buf.SetPos(int32(ExtOffset + ExtItemSize*idx))
	m.buf.PutString(value)
	return nil
}
