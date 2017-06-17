package diskq

//QueueWriter to write into Queue
type QueueWriter struct {
	index *Index
	block *Block
}

//NewQueueWriter create writer
func NewQueueWriter(index *Index) *QueueWriter {
	w := &QueueWriter{index, nil}
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

	count, err := w.block.Write(m)

	if count <= 0 && err == nil {
		w.block.Close()
		w.block, _ = w.index.LoadBlockToWrite()
		return w.block.Write(m)
	}

	return count, err
}
