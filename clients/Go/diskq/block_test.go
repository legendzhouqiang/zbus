package diskq

import (
	"testing"
)

func TestDiskMsg_writeToBuffer(t *testing.T) {
	m := &DiskMsg{}
	m.Body = []byte("hello world")

	buf := NewFixedBuf(m.Size())
	m.writeToBuffer(buf)

	if buf.pos != m.Size() {
		t.Fail()
	}

	if err := buf.PutByte(byte(1)); err == nil {
		t.Fail()
	}
}

func TestBlock_Write(t *testing.T) {
	idx, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		t.Fail()
	}
	defer idx.Close()

	block, err := idx.LoadBlockToWrite()
	if err != nil {
		t.Fail()
	}
	defer block.Close()

	msg := &DiskMsg{}
	msg.Body = []byte("hello world")
	block.Write(msg)
}

func TestBlock_WriteBatch(t *testing.T) {
	idx, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		t.Fail()
	}
	defer idx.Close()

	block, err := idx.LoadBlockToWrite()
	if err != nil {
		t.Fail()
	}
	defer block.Close()

	msgs := make([]DiskMsg, 10)
	for i := 0; i < len(msgs); i++ {
		msg := &DiskMsg{}
		msg.Body = []byte("hello world")
		msgs[i] = *msg
	}
	block.WriteBatch(msgs)
}
