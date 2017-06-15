package main

import (
	"encoding/binary"
	"fmt"
	"os"

	"path/filepath"

	"./mmap"
)

//MBuf is a memory mapped file buffer, read and writes to file like in memory
type MBuf struct {
	data mmap.MMap
	pos  int32
	len  int32
	bo   binary.ByteOrder

	file *os.File
}

//NewMBuf create MBuf from file
func NewMBuf(fileName string, length int) (*MBuf, error) {
	dir := filepath.Dir(fileName)
	if err := os.MkdirAll(dir, 0644); err != nil {
		return nil, err
	}
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
	return &MBuf{data: data, pos: int32(0), len: int32(len(data)), bo: binary.BigEndian, file: file}, nil
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

func (b *MBuf) check(forward int32) error {
	if b.pos+forward > b.len {
		return fmt.Errorf("pos=%d, invalid to forward %d byte", b.pos, forward)
	}
	return nil
}

//SetPos set position
func (b *MBuf) SetPos(pos int32) {
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
	n := int32(2)
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint16(b.data[b.pos : b.pos+n])
	b.pos += n
	return int16(value), nil
}

//GetInt32 read two bytes as int32
func (b *MBuf) GetInt32() (int32, error) {
	n := int32(4)
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint32(b.data[b.pos : b.pos+n])
	b.pos += n
	return int32(value), nil
}

//GetInt64 read two bytes as int64
func (b *MBuf) GetInt64() (int64, error) {
	n := int32(8)
	if err := b.check(n); err != nil {
		return 0, err
	}
	value := b.bo.Uint64(b.data[b.pos : b.pos+n])
	b.pos += n
	return int64(value), nil
}

//GetBytes read n bytes
func (b *MBuf) GetBytes(n int32) ([]byte, error) {
	if err := b.check(n); err != nil {
		return nil, err
	}
	value := b.data[b.pos : b.pos+n]
	b.pos += n
	return value, nil
}

//GetString read string(1_len + max 127 string)
func (b *MBuf) GetString() (string, error) {
	n, err := b.GetByte()
	if err != nil {
		return "", err
	}
	if n < 0 || n > 127 {
		return "", fmt.Errorf("GetString error, invalid length in first byte")
	}
	if err := b.check(int32(n)); err != nil {
		return "", err
	}

	value := string(b.data[b.pos : b.pos+int32(n)])
	b.pos += int32(n)
	return value, nil
}

//PutString write string(1_len + max 127 string)
func (b *MBuf) PutString(value string) error {
	n := len(value)
	if err := b.PutByte(byte(n)); err != nil {
		return err
	}
	return b.PutBytes([]byte(value))
}

//PutByte write one byte
func (b *MBuf) PutByte(value byte) error {
	n := int32(1)
	if err := b.check(n); err != nil {
		return err
	}
	b.data[b.pos] = value
	b.pos += n
	return nil
}

//PutInt16 write int16
func (b *MBuf) PutInt16(value int16) error {
	n := int32(2)
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint16(b.data[b.pos:b.pos+n], uint16(value))
	b.pos += n
	return nil
}

//PutInt32 write int32
func (b *MBuf) PutInt32(value int32) error {
	n := int32(24)
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint32(b.data[b.pos:b.pos+n], uint32(value))
	b.pos += n
	return nil
}

//PutInt64 write int64
func (b *MBuf) PutInt64(value int64) error {
	n := int32(24)
	if err := b.check(n); err != nil {
		return err
	}
	b.bo.PutUint64(b.data[b.pos:b.pos+n], uint64(value))
	b.pos += n
	return nil
}

//PutBytes write n bytes
func (b *MBuf) PutBytes(value []byte) error {
	n := int32(len(value))
	if err := b.check(n); err != nil {
		return err
	}
	copy(b.data[b.pos:], value)
	b.pos += n
	return nil
}
