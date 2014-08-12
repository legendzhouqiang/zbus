package main
import(
	"fmt"
	"bytes"
)  

var HttpMethod = map[string]bool{
	"GET": true,
	"POST": true,
	"PUT": true,
	"DELETE": true,
	"HEAD": true,
	"OPTIONS": true,
}

var HttpStatus = map[string]string{
    "200": "OK",
    "201": "Created",
    "202": "Accepted",
    "204": "No Content",
    "206": "Partial Content",
    "301": "Moved Permanently",
    "304": "Not Modified", 
    "400": "Bad Request",
    "401": "Unauthorized", 
    "403": "Forbidden",
    "404": "Not Found",
    "405": "Method Not Allowed", 
    "416": "Requested Range Not Satisfiable",
    "500": "Internal Server Error",
}



type Meta struct{ 
	Method string
	Command string
	Params map[string] string
}

func (m *Meta) encodeCommand() string{
	var buf bytes.Buffer
	buf.WriteString(m.Command) 
	if m.Params == nil {
		return buf.String()
	}
	if len(m.Params) > 0 {
		buf.WriteString("?")
	}
	for key,val := range m.Params{
		buf.WriteString(key + "=" + val + "&");
	} 
	var res = buf.Bytes()
	res = bytes.TrimRight(res, "&")
	return string(res)
}

func (m *Meta) GetParam(key string) string{
	if m.Params == nil{
		return ""
	}
	return m.Params[key]
}
func (m *Meta) SetParam(key string, val string){
	if m.Params == nil{
		m.Params = make(map[string] string)
	}
	m.Params[key] = val
} 



func main(){    
	var m = Meta{Method: "GET"}
	m.SetParam("hong", "leiming")
	m.SetParam("key2", "val2")
	fmt.Println(m.encodeCommand())
	fmt.Println("ok")
}

