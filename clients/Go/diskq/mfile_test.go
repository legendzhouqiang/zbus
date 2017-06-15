package main

import "testing"

func TestMappedFile_GetExt(t *testing.T) {
	m, err := NewMappedFile("/tmp/MyTopic.idx")
	if err != nil {
		t.Fail()
	}
	defer m.Close()

	m.SetExt(0, "MyExtension")
	value, _ := m.GetExt(0)
	if value != "MyExtension" {
		t.Fail()
	}
}
