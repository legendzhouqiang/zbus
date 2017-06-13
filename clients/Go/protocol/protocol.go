package protocol

const (
	VersionValue = "0.8.0"
	//MQ Produce/Consume
	Produce = "produce"
	Consume = "consume"
	Rpc     = "rpc"
	Route   = "route" //route back message to sender, designed for RPC

	//Topic/ConsumeGroup control
	Declare = "declare"
	Query   = "query"
	Remove  = "remove"
	Empty   = "empty"

	//Tracker
	TrackPub = "track_pub"
	TrackSub = "track_sub"
	Tracker  = "tracker"
	Server   = "server"

	Cmd       = "cmd"
	Topic     = "topic"
	TopicMask = "topic_mask"
	Tag       = "tag"
	Offset    = "offset"

	ConsumeGroup     = "consume_group"
	GroupStartCopy   = "group_start_copy"
	GroupStartOffset = "group_start_offset"
	GroupStartMsgid  = "group_start_msgid"
	GroupStartTime   = "group_start_time"
	GroupFilter      = "group_filter"
	GroupMask        = "group_mask"
	ConsumeWindow    = "consume_window"

	Sender = "sender"
	Recver = "recver"
	Id     = "id"
	Host   = "host"

	Ack      = "ack"
	Encoding = "encoding"

	OriginId     = "origin_id"
	OriginUrl    = "origin_url"
	OriginStatus = "origin_status"

	//Security

	Token = "token"

	MaskPause        = 1 << 0
	MaskRpc          = 1 << 1
	MaskExclusive    = 1 << 2
	MaskDeleteOnExit = 1 << 3

	//Pages

	Home = ""
	Js   = "js"
	Css  = "css"
	Img  = "img"
	Page = "page"

	Heartbeat = "heartbeat"
)
