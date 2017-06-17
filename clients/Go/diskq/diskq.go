package diskq

import (
	"fmt"
	"path/filepath"
	"strings"
	"sync"
)

const (
	FilterPos    = 12
	FilterMaxLen = 127
)

//QueueWriter to write into Queue
type QueueWriter struct {
	index *Index
	block *Block
	mutex *sync.Mutex
}

//NewQueueWriter create writer
func NewQueueWriter(index *Index) *QueueWriter {
	w := &QueueWriter{index, nil, &sync.Mutex{}}
	w.block, _ = index.LoadBlockToWrite()
	return w
}

//Close the writer
func (w *QueueWriter) Close() {
	if w.block != nil {
		w.block.Close()
		w.block = nil
	}
}

func (w *QueueWriter) Write(m *DiskMsg) (int, error) {
	w.mutex.Lock()
	defer w.mutex.Unlock()
	count, err := w.block.Write(m)

	if count <= 0 && err == nil {
		w.block.Close()
		w.block, _ = w.index.LoadBlockToWrite()
		return w.block.Write(m)
	}

	return count, err
}

//WriteBatch write multiple msg in one batch
func (w *QueueWriter) WriteBatch(msgs []DiskMsg) (int, error) {
	w.mutex.Lock()
	defer w.mutex.Unlock()

	count, err := w.block.WriteBatch(msgs)

	if count <= 0 && err == nil {
		w.block.Close()
		w.block, _ = w.index.LoadBlockToWrite()
		return w.block.WriteBatch(msgs)
	}

	return count, err
}

//QueueReader to read from Queue
type QueueReader struct {
	*MappedFile

	group       string
	offset      int32
	msgNo       int64
	filter      string
	filterParts []string

	index   *Index
	blockNo int64
	block   *Block
}

//NewQueueReader create reader
func NewQueueReader(index *Index, group string) (*QueueReader, error) {
	fullPath := filepath.Join(index.dirPath, ReaderDir, fmt.Sprintf("%s%s", group, ReaderSuffix))
	m, err := NewMappedFile(fullPath, ReaderSize)
	if err != nil {
		return nil, err
	}
	r := &QueueReader{}
	r.MappedFile = m
	r.group = group
	r.index = index

	if r.newFile {
		r.blockNo = index.BlockStart()
		r.offset = 0
		r.writeOffset()
		r.buf.SetPos(FilterPos)
		r.buf.PutString(r.filter) //empty
	} else {
		r.buf.SetPos(0)
		r.blockNo, _ = r.buf.GetInt64()
		r.offset, _ = r.buf.GetInt32()
		r.filter, _ = r.buf.GetStringN(FilterMaxLen + 1)
		r.filterParts = strings.Split(r.filter, ".")
	}

	start := r.index.BlockStart()
	if r.blockNo < start { //if block number invalid
		r.blockNo = start
		r.offset = 0
		r.writeOffset()
	}
	if r.index.IsOverflow(r.blockNo) {
		r.blockNo = r.index.CurrBlockNo()
		r.offset = 0
		r.writeOffset()
	}

	r.block, err = r.index.LoadBlock(r.blockNo)
	if err != nil {
		r.Close()
		return nil, err
	}
	err = r.readMsgNo()
	if err != nil {
		r.Close()
		return nil, err
	}

	return r, nil
}

//Close destroy the queue reader
func (r *QueueReader) Close() {
	r.MappedFile.Close()
	if r.block != nil {
		r.block.Close()
	}
}

//Filter returns filter
func (r *QueueReader) Filter() string {
	return r.filter
}

func (r *QueueReader) Read() (*DiskMsg, error) {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	return r.read()
}

func (r *QueueReader) read() (*DiskMsg, error) {
	if r.block.IsBlockEnd(int(r.offset)) {
		if r.index.IsOverflow(r.blockNo + 1) {
			return nil, nil
		}
		r.blockNo++
		r.block.Close()
		var err error
		r.block, err = r.index.LoadBlock(r.blockNo)
		if err != nil {
			return nil, err
		}
		r.offset = 0
		r.writeOffset()
	}
	m, msgNo, bytesRead, err := r.block.read(int(r.offset), r.filterParts)
	if err != nil {
		return nil, err
	}
	r.offset += int32(bytesRead)
	if msgNo >= 0 {
		r.msgNo = msgNo
	}
	r.writeOffset()
	if m == nil {
		return r.read()
	}
	return m, nil
}

//SetFilter of reader
func (r *QueueReader) SetFilter(value string) {
	r.mutex.Lock()
	defer r.mutex.Unlock()

	r.filter = value
	r.filterParts = strings.Split(r.filter, ".")
	r.buf.SetPos(FilterPos)
	r.buf.PutStringN(r.filter, FilterMaxLen+1)
}

func (r *QueueReader) writeOffset() {
	r.buf.SetPos(0)
	r.buf.PutInt64(r.blockNo)
	r.buf.PutInt32(r.offset)
}

func (r *QueueReader) readMsgNo() error {
	if r.block.IsBlockEnd(int(r.offset)) { //just at the end of block
		if r.index.IsOverflow(r.blockNo + 1) { //no more block available
			r.msgNo = r.index.MsgCount() - 1
			return nil
		}
		r.blockNo++ //forward to next readable block
		r.block.Close()
		var err error
		r.block, err = r.index.LoadBlock(r.blockNo)
		if err != nil {
			return err
		}
		r.offset = 0
		r.writeOffset()
	}
	msg, _, _, err := r.block.Read(int(r.offset), nil)
	r.msgNo = msg.MsgNo - 1
	return err
}
