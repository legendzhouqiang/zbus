package main

import (
	"bytes"
	"fmt"
	"strconv"
	"strings"

	"./protocol"
)

var statusText = map[int]string{
	100: "Continue",
	101: "Switching Protocols",
	200: "OK",
	400: "Bad Request",
	401: "Unauthorized",
	403: "Forbidden",
	404: "Not Found",
	405: "Method Not Allowed",
	500: "Internal Server Error",
	502: "Bad Gateway",
	503: "Service Unavailable",
	504: "Gateway Timeout",
}

// StatusText returns a text for the HTTP status code.
func StatusText(code int) string {
	value, ok := statusText[code]
	if !ok {
		return "Unkown Status"
	}
	return value
}

//Message stands for HTTP message including Request and Response
type Message struct {
	Status int
	Url    string
	Method string
	Header map[string]string

	body []byte
}

//NewMessage creates a Message, args if provied format to body string
func NewMessage(args ...interface{}) *Message {
	m := new(Message)
	m.Url = "/"
	m.Method = "GET"
	m.Header = make(map[string]string)
	m.Header["connection"] = "Keep-Alive"
	m.SetBodyString(args...)
	return m
}

//NewMessageStatus create message with status and body
func NewMessageStatus(status int, args ...interface{}) *Message {
	m := NewMessage(args...)
	m.Status = status
	return m
}

//SetHeaderIfNone updates header by value if not set yet
func (m *Message) SetHeaderIfNone(key string, val string) {
	if _, ok := m.Header[key]; ok {
		return
	}
	m.Header[key] = val
}

//SetBody set binary body of Message
func (m *Message) SetBody(body []byte) {
	m.body = body
}

//SetBodyString set string body of Message
func (m *Message) SetBodyString(args ...interface{}) {
	if len(args) > 0 {
		format := args[0]
		switch format.(type) {
		case string:
			body := fmt.Sprintf(format.(string), args[1:]...)
			m.body = []byte(body)
			//ignore otherwise
		}
	}
}

//SetJsonBody set json body
func (m *Message) SetJsonBody(body string) {
	m.body = []byte(body)
	m.Header["content-type"] = "application/json"
}

//EncodeMessage encodes Message to []byte
func (m *Message) EncodeMessage(buf *bytes.Buffer) {
	if m.Status != 0 {
		buf.WriteString(fmt.Sprintf("HTTP/1.1 %d %s\r\n", m.Status, StatusText(m.Status)))
	} else {
		buf.WriteString(fmt.Sprintf("%s %s HTTP/1.1\r\n", m.Method, m.Url))
	}
	for k, v := range m.Header {
		k = strings.ToLower(k)
		if k == "content-length" {
			continue
		}
		buf.WriteString(fmt.Sprintf("%s: %s\r\n", k, v))
	}
	bodyLen := 0
	if m.body != nil {
		bodyLen = len(m.body)
	}
	buf.WriteString(fmt.Sprintf("content-length: %d\r\n", bodyLen))

	buf.WriteString("\r\n")
	if m.body != nil {
		buf.Write(m.body)
	}
}

//String convert message to string
func (m *Message) String() string {
	buf := new(bytes.Buffer)
	m.EncodeMessage(buf)
	return string(buf.Bytes())
}

//DecodeMessage decode Message from Buffer, nil returned if not enought in buffer
func DecodeMessage(buf *bytes.Buffer) *Message {
	bb := buf.Bytes()
	idx := bytes.Index(bb, []byte("\r\n\r\n"))
	if idx == -1 {
		return nil
	}
	m := NewMessage()
	header := bytes.Split(bb[:idx], []byte("\r\n"))
	meta := string(header[0])
	metaFields := strings.Fields(meta)
	if strings.HasPrefix(strings.ToUpper(metaFields[0]), "HTTP") {
		m.Status, _ = strconv.Atoi(metaFields[1])
	} else {
		m.Method = metaFields[0]
		m.Url = metaFields[1]
	}
	for i := 1; i < len(header); i++ {
		s := string(header[i])
		kv := strings.Split(s, ":")
		key := strings.ToLower(strings.TrimSpace(kv[0]))
		val := strings.TrimSpace(kv[1])
		m.Header[key] = val
	}
	bodyLen := 0
	if lenStr, ok := m.Header["content-length"]; ok {
		bodyLen, _ = strconv.Atoi(lenStr)
	}
	if (buf.Len() - idx - 4) < bodyLen {
		return nil
	}
	if bodyLen > 0 {
		m.SetBody(bb[idx+4 : idx+4+bodyLen])
	}
	data := make([]byte, idx+4+bodyLen)
	buf.Read(data)
	return m
}

//////////////////////////////The following are all helper Getter/Setter of Header///////////////////////////

//GetHeader key=value
func (m *Message) GetHeader(key string) string {
	return m.Header[key]
}

//SetHeader key=value
func (m *Message) SetHeader(key string, value string) {
	if value == "" {
		return
	}
	m.Header[key] = value
}

//Ack return whether ack header set or not, default to true
func (m *Message) Ack() bool {
	ack := m.GetHeader(protocol.Ack)
	if ack == "" {
		return true //default to ack if not set
	}
	boolAck, err := strconv.ParseBool(ack)
	if err != nil {
		return false
	}
	return boolAck
}

//SetAck set ack value to header
func (m *Message) SetAck(ack bool) {
	m.SetHeader(protocol.Ack, fmt.Sprintf("%v", ack))
}

//Id key=id
func (m *Message) Id() string {
	return m.GetHeader(protocol.Id)
}

//SetId key=id
func (m *Message) SetId(value string) {
	m.SetHeader(protocol.Id, value)
}

//OriginId key=origin_id
func (m *Message) OriginId() string {
	return m.GetHeader(protocol.OriginId)
}

//SetOriginId key=origin_id
func (m *Message) SetOriginId(value string) {
	m.SetHeader(protocol.OriginId, value)
}

//Topic key=topic
func (m *Message) Topic() string {
	return m.GetHeader(protocol.Topic)
}

//SetTopic key=topic
func (m *Message) SetTopic(value string) {
	m.SetHeader(protocol.Topic, value)
}

//ConsumeGroup key=consume_group
func (m *Message) ConsumeGroup() string {
	return m.GetHeader(protocol.ConsumeGroup)
}

//SetConsumeGroup key=consume_group
func (m *Message) SetConsumeGroup(value string) {
	m.SetHeader(protocol.ConsumeGroup, value)
}

//GroupFilter key=group_filter
func (m *Message) GroupFilter() *string {
	s, ok := m.Header[protocol.GroupFilter]
	if !ok {
		return nil
	}
	return &[]string{s}[0]
}

//SetGroupFilter key=group_filter
func (m *Message) SetGroupFilter(value string) {
	m.SetHeader(protocol.GroupFilter, value)
}

//GroupMask key=group_mask
func (m *Message) GroupMask() *int32 {
	s, ok := m.Header[protocol.GroupMask]
	if !ok {
		return nil
	}
	value, _ := strconv.Atoi(s)
	return &[]int32{int32(value)}[0]
}

//SetGroupMask key=group_mask
func (m *Message) SetGroupMask(value int32) {
	m.SetHeader(protocol.GroupMask, strconv.Itoa(int(value)))
}

//GroupStartCopy key=group_start_copy
func (m *Message) GroupStartCopy() *string {
	s, ok := m.Header[protocol.GroupStartCopy]
	if !ok {
		return nil
	}
	return &[]string{s}[0]
}

//SetGroupStartCopy key=group_start_copy
func (m *Message) SetGroupStartCopy(value string) {
	m.SetHeader(protocol.GroupStartCopy, value)
}

//GroupStartOffset group_start_offset
func (m *Message) GroupStartOffset() *int64 {
	s, ok := m.Header[protocol.GroupStartOffset]
	if !ok {
		return nil
	}
	value, _ := strconv.ParseInt(s, 10, 64)
	return &[]int64{value}[0]
}

//SetGroupStartOffset group_start_offset
func (m *Message) SetGroupStartOffset(value int64) {
	m.SetHeader(protocol.GroupStartOffset, fmt.Sprintf("%d", value))
}

//GroupStartMsgid key=group_start_msgid
func (m *Message) GroupStartMsgid() *string {
	s, ok := m.Header[protocol.GroupStartMsgid]
	if !ok {
		return nil
	}
	return &[]string{s}[0]
}

//SetGroupStartMsgid key=group_start_msgid
func (m *Message) SetGroupStartMsgid(value string) {
	m.SetHeader(protocol.GroupStartMsgid, value)
}

//GroupStartTime group_start_time
func (m *Message) GroupStartTime() *int64 {
	s, ok := m.Header[protocol.GroupStartTime]
	if !ok {
		return nil
	}
	value, _ := strconv.ParseInt(s, 10, 64)
	return &[]int64{value}[0]
}

//SetGroupStartTime group_start_time
func (m *Message) SetGroupStartTime(value int64) {
	m.SetHeader(protocol.GroupStartTime, fmt.Sprintf("%d", value))
}

//OriginUrl key=origin_url
func (m *Message) OriginUrl() string {
	return m.GetHeader(protocol.OriginUrl)
}

//SetOriginUrl key=origin_url
func (m *Message) SetOriginUrl(value string) {
	m.SetHeader(protocol.OriginUrl, value)
}

//Token key=token
func (m *Message) Token() string {
	return m.GetHeader(protocol.Token)
}

//SetToken key=token
func (m *Message) SetToken(value string) {
	m.SetHeader(protocol.Token, value)
}

//TopicMask key=topic_mask
func (m *Message) TopicMask() *int32 {
	s := m.GetHeader(protocol.TopicMask)
	if s == "" {
		return nil
	}
	value, _ := strconv.Atoi(s)
	return &[]int32{int32(value)}[0]
}

//SetTopicMask key=topic_mask
func (m *Message) SetTopicMask(value int32) {
	m.SetHeader(protocol.TopicMask, strconv.Itoa(int(value)))
}
