package websocket

import (
	"crypto/md5"
	"encoding/hex"
	"os"
	"testing"
)

func TestFileTransferWorkflow(t *testing.T) {
	// Setup server
	server := &Server{
		fileSessions: make(map[string]*fileSession),
	}

	client := &WSClient{
		ID:   "test-client-123",
		Send: make(chan []byte, 10),
	}

	content := []byte("Hello, this is a test file for verification.")
	hasher := md5.New()
	hasher.Write(content)
	expectedMD5 := hex.EncodeToString(hasher.Sum(nil))

	// 1. Start File Upload Session
	startPayload := map[string]any{
		"action": "start",
		"id":     "transfer-id-001",
		"name":   "testfile.txt",
		"size":   int64(len(content)),
		"md5":    expectedMD5,
	}

	server.processFileMessage(client, startPayload)

	// Verify session was created
	session := server.fileSessions[client.ID]
	if session == nil {
		t.Fatalf("Expected file session to be created, got nil")
	}
	if session.id != "transfer-id-001" {
		t.Errorf("Expected session id to be 'transfer-id-001', got %s", session.id)
	}

	// 2. Upload correct chunks
	chunkOk := server.handleBinaryFileChunk(client, content)
	if !chunkOk {
		t.Errorf("Expected chunk write to succeed")
	}

	// 3. Complete session with valid MD5
	completePayload := map[string]any{
		"action": "complete",
		"id":     "transfer-id-001",
	}

	server.processFileMessage(client, completePayload)

	// Verify session was deleted on completion
	if _, exists := server.fileSessions[client.ID]; exists {
		t.Errorf("Expected session to be cleaned up after completion")
	}

	// Verify file exists at destination path and contents match
	if _, err := os.Stat(session.path); os.IsNotExist(err) {
		t.Errorf("Expected uploaded file to exist at %s", session.path)
	} else {
		data, err := os.ReadFile(session.path)
		if err != nil {
			t.Fatalf("Failed to read created file: %v", err)
		}
		if string(data) != string(content) {
			t.Errorf("File content mismatch: expected %q, got %q", content, data)
		}
		// Cleanup final file
		_ = os.Remove(session.path)
	}
}

func TestFileTransferWorkflowMD5Mismatch(t *testing.T) {
	// Setup server
	server := &Server{
		fileSessions: make(map[string]*fileSession),
	}

	client := &WSClient{
		ID:   "test-client-456",
		Send: make(chan []byte, 10),
	}

	content := []byte("Genuine content.")
	wrongMD5 := "wrong_md5_hash_value"

	// 1. Start File Upload Session with wrong MD5
	startPayload := map[string]any{
		"action": "start",
		"id":     "transfer-id-002",
		"name":   "testmismatch.txt",
		"size":   int64(len(content)),
		"md5":    wrongMD5,
	}

	server.processFileMessage(client, startPayload)

	session := server.fileSessions[client.ID]
	if session == nil {
		t.Fatalf("Expected file session to be created, got nil")
	}

	// 2. Write content chunks
	server.handleBinaryFileChunk(client, content)

	// 3. Complete session (should fail MD5 verification)
	completePayload := map[string]any{
		"action": "complete",
		"id":     "transfer-id-002",
	}

	server.processFileMessage(client, completePayload)

	// Verify file is NOT created at destination path
	if _, err := os.Stat(session.path); !os.IsNotExist(err) {
		t.Errorf("Expected target file to NOT be created on MD5 mismatch")
		_ = os.Remove(session.path)
	}

	// Verify temp part file was removed
	if _, err := os.Stat(session.tempPath); !os.IsNotExist(err) {
		t.Errorf("Expected temp part file to be removed on mismatch")
		_ = os.Remove(session.tempPath)
	}
}
