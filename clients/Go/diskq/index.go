package diskq

import (
	"fmt"
	"os"
	"path/filepath"
	"time"
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

//Offset points to the block
type Offset struct {
	BaseOffset  int64
	CreatedTime int64
	EndOffset   int32
	UpdatedTime int64
}

//Index manages block files in DiskQ, all private functions are NOT threadsafe
//all public functions are threadsafe
type Index struct {
	*MappedFile
	name    string
	dirPath string

	version    int32
	blockCount int32
	blockStart int64
	msgCount   int64
}

//NewIndex create index file
func NewIndex(dirPath string) (*Index, error) {
	_, name := filepath.Split(dirPath)
	fullPath := filepath.Join(dirPath, fmt.Sprintf("%s%s", name, IndexSuffix))
	m, err := NewMappedFile(fullPath, IndexSize)
	if err != nil {
		return nil, err
	}
	index := &Index{m, name, dirPath, IndexVersion, 0, 0, 0}

	if m.newFile {
		m.buf.SetPos(0)
		m.buf.PutInt32(index.version)
	} else {
		m.buf.SetPos(0)
		index.version, _ = m.buf.GetInt32()
		if index.version != IndexVersion {
			m.Close()
			return nil, fmt.Errorf("Index version unmatched")
		}
		index.blockCount, _ = m.buf.GetInt32()
		index.blockStart, _ = m.buf.GetInt64()
		index.msgCount, _ = m.buf.GetInt64()
	}
	return index, nil
}

//LoadBlockToWrite create Block by block number
func (idx *Index) LoadBlockToWrite() (*Block, error) {
	idx.mutex.Lock()
	defer idx.mutex.Unlock()
	if idx.blockCount < 1 || idx.isCurrBlockFull() {
		idx.addNewOffset()
	}
	return idx.loadBlock(idx.currBlockNo())
}

//LoadBlock create Block by block number
func (idx *Index) LoadBlock(blockNo int64) (*Block, error) {
	idx.mutex.Lock()
	defer idx.mutex.Unlock()
	return idx.loadBlock(blockNo)
}

//loadBlock NOT thread safe
func (idx *Index) loadBlock(blockNo int64) (*Block, error) {
	if blockNo < 0 {
		return nil, fmt.Errorf("Block(%d) not found", blockNo)
	}
	if blockNo-idx.blockStart >= int64(idx.blockCount) {
		return nil, fmt.Errorf("blockNo should >=%d and <%d, but was %d",
			idx.blockStart, idx.blockStart+int64(idx.blockCount), blockNo)
	}

	offset := idx.readOffset(blockNo)
	blockName := fmt.Sprintf("%020d%s", offset.BaseOffset, BlockSuffix)
	blockDir := filepath.Join(idx.dirPath, BlockDir)
	if err := os.MkdirAll(blockDir, 0644); err != nil {
		return nil, err
	}
	file, err := os.OpenFile(filepath.Join(blockDir, blockName), os.O_RDWR|os.O_CREATE, 0644)
	if err != nil {
		return nil, err
	}
	return newBlock(idx, blockNo, file), nil
}

//BlockCount return block count
func (idx *Index) BlockCount() int32 {
	return idx.blockCount
}

//BlockStart returns block start
func (idx *Index) BlockStart() int64 {
	return idx.blockStart
}

//MsgCount returns message count
func (idx *Index) MsgCount() int64 {
	return idx.msgCount
}

//IsOverflow test if block number is larger than the last block
func (idx *Index) IsOverflow(blockNo int64) bool {
	return blockNo >= (idx.blockStart + int64(idx.blockCount))
}

//CurrWriteOffset return current write block offset
func (idx *Index) CurrWriteOffset() int32 {
	idx.mutex.Lock()
	defer idx.mutex.Unlock()

	return idx.readOffset(idx.currBlockNo()).EndOffset
}

//ReadOffset find out the offset by block number
func (idx *Index) ReadOffset(blockNo int64) *Offset {
	idx.mutex.Lock()
	defer idx.mutex.Unlock()

	return idx.readOffset(blockNo)
}

//IncreaseMsgCount with delta value
func (idx *Index) IncreaseMsgCount(delta int) {
	idx.mutex.Lock()
	defer idx.mutex.Unlock()

	idx.msgCount += int64(delta)
	idx.buf.SetPos(MessageCountPos)
	idx.buf.PutInt64(idx.msgCount)
}

func (idx *Index) isCurrBlockFull() bool {
	if idx.blockCount < 1 {
		return false
	}

	idx.buf.SetPos(idx.currBlockPos() + 16)
	endOffset, _ := idx.buf.GetInt32()
	return endOffset >= BlockMaxSize
}

func (idx *Index) addNewOffset() (*Offset, error) {
	if idx.blockCount >= BlockMaxCount {
		return nil, fmt.Errorf("Offset table full")
	}
	baseOffset := int64(0)
	if idx.blockCount > 0 {
		offset := idx.readOffset(idx.currBlockNo())
		baseOffset = offset.BaseOffset + int64(offset.EndOffset)
	}

	offset := &Offset{}
	offset.CreatedTime = time.Now().UnixNano() / int64(time.Millisecond)
	offset.UpdatedTime = offset.CreatedTime
	offset.BaseOffset = baseOffset
	offset.EndOffset = 0

	idx.blockCount++
	idx.writeBlockCount(idx.blockCount)
	idx.writeOffset(idx.currBlockNo(), offset)

	return offset, nil
}

func (idx *Index) writeBlockCount(value int32) {
	idx.buf.SetPos(BlockCountPos)
	idx.buf.PutInt32(value)
}

func (idx *Index) currBlockNo() int64 {
	return idx.blockStart + int64(idx.blockCount) - 1
}

func (idx *Index) currBlockPos() int32 {
	return idx.blockPos(idx.currBlockNo())
}

func (idx *Index) blockPos(blockNo int64) int32 {
	return int32(HeaderSize + (blockNo%BlockMaxCount)*OffsetSize)
}

func (idx *Index) readOffset(blockNo int64) *Offset {
	idx.buf.SetPos(idx.blockPos(blockNo))

	offset := &Offset{}
	offset.CreatedTime, _ = idx.buf.GetInt64()
	offset.BaseOffset, _ = idx.buf.GetInt64()
	offset.EndOffset, _ = idx.buf.GetInt32()
	offset.UpdatedTime, _ = idx.buf.GetInt64()
	return offset
}

func (idx *Index) writeOffset(blockNo int64, offset *Offset) {
	idx.buf.SetPos(idx.blockPos(blockNo))

	idx.buf.PutInt64(offset.CreatedTime)
	idx.buf.PutInt64(offset.BaseOffset)
	idx.buf.PutInt32(offset.EndOffset)
	idx.buf.PutInt64(offset.UpdatedTime)
}
