## Protocol
/**
 * 
 * JSON format
 * 
 * Request message format
 * 
 * cmd:       pub|sub|create|remove|ping
 * mq:        <mqName>
 * channel:   <channelName> 
 * id:        <messageId>
 * data:      <messageBody>  business related data
 * apiKey:    <apiKey>
 * secretKey: <secretKey>
 * 
 * Response message format(Compared to Request)
 * cmd removed
 * status added 
 * 
 * status:  200|400|404|500 ... similar to HTTP status 
 * 
 * 1.1) Create MQ
 * [R] cmd: create
 * [R] mq:  <mqName> 
 * [O] mqType: mem|disk|db (default to mem)
 * [O] mqMask: <Long> MQ's mask
 * 
 * 1.2) Create Channel
 * [R] cmd: create
 * [R] mq: <mqName>
 * [R] channel: <channelName> 
 * [O] channelOffset: <longOffset> (default to the end of mq)
 * [O] channelMask: <Long> channel's Mask
 * 
 * 2.1) Remove MQ
 * [R] cmd: remove
 * [R] mq: <mqName>
 * 
 * 2.2) Remove Channel
 * [R] cmd: remove
 * [R] mq: <mqName>
 * [R] channel: <channelName>
 * 
 * 3) Publish
 * [R] cmd: pub
 * [R] mq: <mqName>
 * [R] data: <data>
 * 
 * 4) Subscribe
 * [R] cmd: sub
 * [R] mq: <mqName>
 * [R] channel: <channelName>
 * [O] window: <integer> 
 * 
 * 5) Ping
 * cmd: ping
 * 
 * 
 * @author leiming.hong
 *
 */ 
