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
	Timestamp  *int64
	Id         string // write 40 = 1_len + max 39
	CorrOffset *int64
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
	return 0, nil
}

//WriteBatch write message in batch mode
func (b *Block) WriteBatch(msgs []DiskMsg) (int, error) {
	return 0, nil
}

//Size return the size of the message in bytes
func (m *DiskMsg) Size() int32 {
	bodySize := 0
	if m.Body != nil {
		bodySize = len(m.Body)
	}
	return int32(4 + bodySize + MsgBodyPos)
}

func (m *DiskMsg) writeToBuffer(buf *FixedBuf, endOffset int32, msgNo int64) error {
	if m.Size() > buf.Remaining() {
		return fmt.Errorf("buffer size not enough")
	}
	buf.PutInt64(int64(endOffset))
	if m.Timestamp == nil {
		buf.PutInt64(time.Now().UnixNano() / int64(time.Millisecond))
	} else {
		buf.PutInt64(*m.Timestamp)
	}

	return nil
}
