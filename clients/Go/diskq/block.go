package diskq

import (
	"fmt"
	"os"
	"sync"
	"time"
)

const (
	MsgIdMaxLen  = 39
	MsgTagMaxLen = 127
	MsgBodyPos   = 8 + 8 + 40 + 8 + 8 + 128 //200
)

//DiskMsg to read and write in disk for DiskQ
type DiskMsg struct {
	Offset     int64
	Timestamp  int64
	Id         string // write 40 = 1_len + max 39
	CorrOffset int64
	MsgNo      int64
	Tag        string // write 128 = 1_len + max 127
	Body       []byte // write 4_len + body
}

//Block is a read/write block file
type Block struct {
	index   *Index
	blockNo int64
	file    *os.File
	mutex   *sync.Mutex
}

//NewBlock create block of an Index
func newBlock(index *Index, blockNo int64, file *os.File) *Block {
	b := &Block{}
	b.index = index
	b.blockNo = blockNo
	b.file = file
	b.mutex = &sync.Mutex{}
	return b
}

//Close the block file
func (b *Block) Close() {
	b.file.Close()
}

//Write message to disk
func (b *Block) Write(msg *DiskMsg) (int, error) {
	b.mutex.Lock()
	defer b.mutex.Unlock()

	startOffset := b.index.ReadOffset(b.blockNo).EndOffset
	if startOffset >= BlockMaxSize {
		return 0, fmt.Errorf("Block full")
	}

	msg.Offset = int64(startOffset)
	msg.MsgNo = b.index.GetAndAddMsgCount(1)
	msgSize := msg.Size()
	buf := NewFixedBuf(msgSize)
	msg.writeToBuffer(buf)
	b.file.Seek(int64(startOffset), 0)
	n, err := b.file.Write(buf.Bytes())
	if err != nil {
		return n, err
	}
	b.index.WriteEndOffset(startOffset + int32(msgSize))
	return n, err
}

//WriteBatch write message in batch mode
func (b *Block) WriteBatch(msgs []DiskMsg) (int, error) {
	if len(msgs) == 0 {
		return 0, fmt.Errorf("WriteBatch msgs parameter length should >= 1")
	}

	b.mutex.Lock()
	defer b.mutex.Unlock()

	startOffset := b.index.ReadOffset(b.blockNo).EndOffset
	if startOffset >= BlockMaxSize {
		return 0, fmt.Errorf("Block full")
	}
	totalSize := 0
	offset := startOffset
	msgNo := b.index.GetAndAddMsgCount(len(msgs))
	for _, msg := range msgs {
		msg.Offset = int64(offset)
		msg.MsgNo = msgNo
		msgNo++
		msgSize := msg.Size()
		totalSize += msgSize
		offset += int32(msgSize)
	}
	buf := NewFixedBuf(totalSize)
	for _, msg := range msgs {
		msg.writeToBuffer(buf)
	}

	b.file.Seek(int64(startOffset), 0)
	n, err := b.file.Write(buf.Bytes())
	if err != nil {
		return n, err
	}
	b.index.WriteEndOffset(startOffset + int32(totalSize))
	return n, err
}

//Size return the size of the message in bytes
func (m *DiskMsg) Size() int {
	bodySize := len(m.Body)
	return 4 + bodySize + MsgBodyPos
}

func (m *DiskMsg) writeToBuffer(buf *FixedBuf) error {
	if m.Size() > buf.Remaining() {
		return fmt.Errorf("buffer size not enough")
	}
	buf.PutInt64(m.Offset)
	if m.Timestamp <= 0 {
		buf.PutInt64(time.Now().UnixNano() / int64(time.Millisecond))
	} else {
		buf.PutInt64(m.Timestamp)
	}
	buf.PutStringN(m.Id, MsgIdMaxLen+1)
	if m.CorrOffset <= 0 {
		buf.PutInt64(0)
	} else {
		buf.PutInt64(m.CorrOffset)
	}
	buf.PutInt64(m.MsgNo)
	buf.PutStringN(m.Tag, MsgTagMaxLen+1)

	buf.PutInt32(int32(len(m.Body)))
	if m.Body != nil {
		buf.PutBytes(m.Body)
	}
	return nil
}
