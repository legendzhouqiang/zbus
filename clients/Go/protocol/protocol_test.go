package protocol

import (
	"encoding/json"
	"testing"
)

func TestJson(t *testing.T) {
	g := &ConsumeGroupInfo{}
	g.GroupName = "MyGroup"
	g.MessageCount = 0

	data, err := json.Marshal(g)
	if err != nil {
		t.Fail()
	}
	println(string(data))

	track := &TrackerInfo{}
	data, err = json.Marshal(track)
	if err != nil {
		t.Fail()
	}
	println(string(data))

}
