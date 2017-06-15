package main

import (
	"testing"
)

func TestNewIndex(t *testing.T) {
	idx, err := NewIndex("/tmp/IndexExample")
	if err != nil {
		t.Fail()
	}
	defer idx.Close()
	if idx.version != IndexVersion {
		t.Fail()
	}
}
