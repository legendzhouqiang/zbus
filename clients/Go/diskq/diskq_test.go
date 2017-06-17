package diskq

import (
	"testing"
)

func newIndex() *Index {
	idx, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		return nil
	}
	return idx
}

var index = newIndex()
var w = NewQueueWriter(index)

func TestNewQueueWriter(t *testing.T) {

}

func TestQueueWriter_Write(t *testing.T) {
	if index == nil {
		t.Fail()
	}
	msg := &DiskMsg{}
	msg.Body = []byte("hello world")
	w.Write(msg)
}

func BenchmarkQueueWriter_Write(b *testing.B) {
	if index == nil {
		b.Fail()
	}
	for i := 0; i < b.N; i++ {
		msg := &DiskMsg{}
		msg.Body = make([]byte, 102400)
		w.Write(msg)
	}
}
