package main

import (
	"encoding/binary"
	"fmt"
	"os"

	"./mmap"
)

//MBuf is a memory mapped file buffer, read and writes to file like in memory
type MBuf struct {
	data mmap.MMap
	pos  int
	len  int
	bo   binary.ByteOrder

	file *os.File
}

//NewMBuf create MBuf from file
func NewMBuf(fileName string, length int) (*MBuf, error) {
	file, err := os.OpenFile(fileName, os.O_RDWR|os.O_CREATE, 0644)
	if err != nil {
		return nil, err
	}
	fi, err := file.Stat()
	if err != nil {
		return nil, err
	}
	fsize := fi.Size()
	nLen := int64(length)
	if fsize > nLen {
		file.Truncate(nLen)
	}
	if fsize < nLen {
		zeros := make([]byte, nLen-fsize)
		if _, err := file.WriteAt(zeros, fsize); err != nil {
			return nil, err
		}
	}
	data, err := mmap.MapRegion(file, length, mmap.RDWR, 0, 0)
	return &MBuf{data: data, pos: 0, len: len(data), bo: binary.BigEndian, file: file}, nil
}

//Close the MappedByteBuffer
func (b *MBuf) Close() error {
	err := b.data.Unmap()
	err2 := b.file.Close()
	if err != nil {
		return err
	}
	if err2 != nil {
		return err2
	}
	return nil
}

func (b *MBuf) check(forward int) error {
	if b.pos+forward > b.len {
		return fmt.Errorf("pos=%d, invalid to forward %d byte", b.pos, forward)
	}
	return nil
}

//SetPos set position
func (b *MBuf) SetPos(pos int) {
	b.pos = pos
}

//GetByte read one byte
func (b *MBuf) GetByte() (byte, error) {
	if err := b.check(1); err != nil {
		return 0, err
	}
	value := b.data[b.pos]
	b.pos++
	return value, nil
}

//GetInt16 read two bytes as int16
func (b *MBuf) GetInt16() (int16, error) {
	n := 2
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint16(b.data[b.pos : b.pos+n])
	b.pos += n
	return int16(value), nil
}

//GetInt32 read two bytes as int32
func (b *MBuf) GetInt32() (int32, error) {
	n := 4
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint32(b.data[b.pos : b.pos+n])
	b.pos += n
	return int32(value), nil
}

//GetInt64 read two bytes as int64
func (b *MBuf) GetInt64() (int64, error) {
	n := 8
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint64(b.data[b.pos : b.pos+n])
	b.pos += n
	return int64(value), nil
}

//GetBytes read n bytes
func (b *MBuf) GetBytes(n int) ([]byte, error) {
	if err := b.check(n); err != nil {
		return nil, err
	}
	value := b.data[b.pos : b.pos+n]
	b.pos += n
	return value, nil
}

//PutByte write one byte
func (b *MBuf) PutByte(value byte) error {
	n := 1
	if err := b.check(n); err != nil {
		return err
	}
	b.data[b.pos] = value
	b.pos += n
	return nil
}

//PutInt16 write int16
func (b *MBuf) PutInt16(value int16) error {
	n := 2
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint16(b.data[b.pos:b.pos+n], uint16(value))
	b.pos += n
	return nil
}

//PutInt32 write int32
func (b *MBuf) PutInt32(value int32) error {
	n := 24
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint32(b.data[b.pos:b.pos+n], uint32(value))
	b.pos += n
	return nil
}

//PutInt64 write int64
func (b *MBuf) PutInt64(value int64) error {
	n := 24
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint64(b.data[b.pos:b.pos+n], uint64(value))
	b.pos += n
	return nil
}

//PutBytes write n bytes
func (b *MBuf) PutBytes(value []byte) error {
	n := len(value)
	if err := b.check(n); err != nil {
		return err
	}
	copy(b.data[b.pos:], value)
	b.pos += n
	return nil
}
