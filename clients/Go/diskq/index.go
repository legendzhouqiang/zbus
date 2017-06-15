package main

import (
	"fmt"
	"path/filepath"
)

const (
	IndexVersion  = 0x01
	IndexSuffix   = ".idx"
	ReaderSuffix  = ".rdx"
	BlockSuffix   = ".zbus"
	BlockDir      = "data"
	ReaderDir     = "reader"
	BlockMaxCount = 10240
	BlockMaxSize  = 64 * 1024 * 1024 // default to 64M

	OffsetSize = 28
	IndexSize  = HeaderSize + BlockMaxCount*OffsetSize

	BlockCountPos   = 4
	MessageCountPos = 16
)

//Index manages block files in DiskQ
type Index struct {
	*MappedFile
	name string

	version    int32
	blockCount int32
	blockStart int64
	msgCount   int64
}

//NewIndex create index file
func NewIndex(dirPath string) (*Index, error) {
	_, name := filepath.Split(dirPath)
	fullPath := filepath.Join(dirPath, fmt.Sprintf("%s.idx", name))
	m, err := NewMappedFile(fullPath)
	if err != nil {
		return nil, err
	}
	index := &Index{m, name, IndexVersion, 0, 0, 0}

	if m.fileExists {
		m.buf.SetPos(0)
		index.version, _ = m.buf.GetInt32()
		if index.version != IndexVersion {
			m.Close()
			return nil, fmt.Errorf("Index version unmatched")
		}
		index.blockCount, _ = m.buf.GetInt32()
		index.blockStart, _ = m.buf.GetInt64()
		index.msgCount, _ = m.buf.GetInt64()
	} else {
		m.buf.SetPos(0)
		m.buf.PutInt32(index.version)
	}
	return index, nil
}
