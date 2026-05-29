package server_test

import (
	"bufio"
	"fmt"
	"net"
	"testing"
	"time"

	"airmouse-go/control"
	"airmouse-go/server"
)

type testMouse struct {
	control.MouseController
}

func (m *testMouse) Move(dx, dy float64)      {}
func (m *testMouse) Click(button string)        {}
func (m *testMouse) DoubleClick()               {}
func (m *testMouse) Scroll(delta int)           {}
func (m *testMouse) Stats() (int64, int64, int64, int64) { return 0,0,0,0 }
func (m *testMouse) SetSensitivity(s float64)   {}

func TestTCPServerStartStop(t *testing.T) {
	logFunc := func(s string) { fmt.Println(s) }
	statsFunc := func(clicks, dbl, right, scroll int) {}
	connCb := func(list []string) {}

	mouse := &testMouse{}
	srv := server.NewTCPServer("127.0.0.1", 0, mouse, logFunc, statsFunc, connCb)
	if err := srv.Start(); err != nil {
		t.Fatalf("start failed: %v", err)
	}
	time.Sleep(100 * time.Millisecond)
	srv.Stop()
}

func TestClientConnection(t *testing.T) {
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer ln.Close()
	go func() {
		conn, _ := ln.Accept()
		defer conn.Close()
		scanner := bufio.NewScanner(conn)
		if scanner.Scan() {
			line := scanner.Text()
			fmt.Println("received:", line)
			conn.Write([]byte(`{"type":"ack","id":"1"}` + "\n"))
		}
	}()

	conn, err := net.Dial("tcp", ln.Addr().String())
	if err != nil {
		t.Fatal(err)
	}
	defer conn.Close()
	fmt.Fprintf(conn, `{"type":"click","id":"1"}`+"\n")
	buf := make([]byte, 1024)
	n, _ := conn.Read(buf)
	if string(buf[:n]) != `{"type":"ack","id":"1"}`+"\n" {
		t.Errorf("unexpected ACK: %s", string(buf[:n]))
	}
}