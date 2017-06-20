package main

import (
	"crypto/rand"
	"fmt"
	"io/ioutil"
	"net"
	"sort"
	"strings"
	"time"
)

//CurrMillis returns current milliseconds of Unix time
func CurrMillis() int64 {
	return time.Now().UnixNano() / int64(time.Millisecond)
}

var fileMap map[string][]byte

//ReadAssetFile read asset file via go binary data or direct io
func ReadAssetFile(file string) ([]byte, error) {
	return ioutil.ReadFile(fmt.Sprintf("asset/%s", file))

	if fileMap == nil {
		fileMap = make(map[string][]byte)
	}
	fileData, ok := fileMap[file]
	if !ok {
		fileData, err := Asset(fmt.Sprintf("asset/%s", file))
		if err == nil {
			fileMap[file] = fileData
		}
		return fileData, err
	}
	return fileData, nil
}

//SplitClean splits string without empty
func SplitClean(s string, sep string) []string {
	bb := strings.Split(s, sep)
	var r []string
	for _, str := range bb {
		if str != "" {
			r = append(r, strings.TrimSpace(str))
		}
	}
	return r
}

//UUID generate psudo uuid string
func uuid() string {
	b := make([]byte, 16)
	rand.Read(b)
	return fmt.Sprintf("%X-%X-%X-%X-%X", b[0:4], b[4:6], b[6:8], b[8:10], b[10:])
}

//ServerAddress combines host and port to form a valid server address, if host is "0.0.0.0",
//local ip address is replaced
func ServerAddress(host string, port int) string {
	if host == "0.0.0.0" {
		ip, err := LocalIPAddress()
		if err == nil {
			host = ip.String()
		}
	}
	return fmt.Sprintf("%s:%d", host, port)
}

type byIP []net.IP

func (s byIP) Len() int {
	return len(s)
}
func (s byIP) Swap(i, j int) {
	s[i], s[j] = s[j], s[i]
}

func rank(ip net.IP) int {
	prefix := []string{"10.", "172.", "192.", "127."}
	str := ip.String()
	for i := 0; i < len(prefix); i++ {
		if strings.HasPrefix(str, prefix[i]) {
			return i
		}
	}
	return 0
}

func (s byIP) Less(i, j int) bool {
	return rank(s[i]) < rank(s[j])
}

//LocalIPAddress get local IP by preference PublicIP > 10.*> 172.* > 192.* > 127.*
func LocalIPAddress() (net.IP, error) {
	ifaces, err := net.Interfaces()
	if err != nil {
		return nil, err
	}
	addresses := []net.IP{}
	for _, iface := range ifaces {
		if iface.Flags&net.FlagUp == 0 {
			continue // interface down
		}
		addrs, err := iface.Addrs()
		if err != nil {
			continue
		}

		for _, addr := range addrs {
			var ip net.IP
			switch v := addr.(type) {
			case *net.IPNet:
				ip = v.IP
			case *net.IPAddr:
				ip = v.IP
			}
			if ip == nil {
				continue
			}
			ip = ip.To4()
			if ip == nil {
				continue // not an ipv4 address
			}
			addresses = append(addresses, ip)
		}
	}
	if len(addresses) == 0 {
		return nil, fmt.Errorf("no address Found, net.InterfaceAddrs: %v", addresses)
	}
	sort.Sort(byIP(addresses))
	return addresses[0], nil
}
