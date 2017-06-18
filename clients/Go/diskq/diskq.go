package diskq

import (
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
	"strings"
	"sync"
)

const (
	FilterPos    = 12
	FilterMaxLen = 127
)

//DiskQueue writer + N readers
type DiskQueue struct {
	index   *Index
	name    string
	writer  *QueueWriter
	readers map[string]*QueueReader
}

//NewDiskQueue create or load disk queue
func NewDiskQueue(dirPath string) (*DiskQueue, error) {
	index, err := NewIndex(dirPath)
	if err != nil {
		return nil, err
	}
	q := &DiskQueue{}
	q.index = index
	q.name = q.index.Name()
	q.writer, err = NewQueueWriter(index)
	if err != nil {
		q.Close()
		return nil, err
	}
	q.readers = make(map[string]*QueueReader)
	err = q.loadReaders()
	if err != nil {
		q.Close()
		return nil, err
	}
	return q, nil
}

//Close disk queue
func (q *DiskQueue) Close() {
	if q.writer != nil {
		q.writer.Close()
		q.writer = nil
	}
	for _, r := range q.readers {
		r.Close()
	}
	q.readers = make(map[string]*QueueReader)
}

//Write to disk queue
func (q *DiskQueue) Write(m *DiskMsg) (int, error) {
	return q.writer.Write(m)
}

//WriteBatch messages to disk queue
func (q *DiskQueue) WriteBatch(msgs []DiskMsg) (int, error) {
	return q.writer.WriteBatch(msgs)
}

//Read message from disk queue
func (q *DiskQueue) Read(readerName string) (*DiskMsg, error) {
	r, ok := q.readers[readerName]
	if !ok {
		return nil, fmt.Errorf("reader(%s) not found", readerName)
	}
	return r.Read()
}

func (q *DiskQueue) loadReaders() error {
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
		if !strings.HasSuffix(fileName, ReaderSuffix) {
			continue
		}
		name := fileName[0 : len(fileName)-len(ReaderSuffix)]
		r, err := NewQueueReader(q.index, name)
		if err != nil {
			log.Printf("Reader %s load error: %s", fileName, err)
		}
		q.readers[name] = r
	}
	return nil
}

//SetMask update mask value
func (q *DiskQueue) SetMask(value int32) {
	q.index.SetMask(value)
}

//SetCreator update creator
func (q *DiskQueue) SetCreator(value string) {
	q.index.SetCreator(value)
}

//SetExt update ext
func (q *DiskQueue) SetExt(i int, value string) error {
	return q.index.SetExt(i, value)
}

//GetExt get ext
func (q *DiskQueue) GetExt(i int) (string, error) {
	return q.index.GetExt(i)
}

//QueueWriter to write into Queue
type QueueWriter struct {
	index *Index
	block *Block
	mutex *sync.Mutex
}

//NewQueueWriter create writer
func NewQueueWriter(index *Index) (*QueueWriter, error) {
	w := &QueueWriter{index, nil, &sync.Mutex{}}
	var err error
	w.block, err = index.LoadBlockToWrite()
	if err != nil {
		return nil, err
	}
	return w, nil
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
