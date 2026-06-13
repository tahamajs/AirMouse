package utils

import "testing"

func TestGenerateID(t *testing.T) {
	id1 := GenerateID()
	id2 := GenerateID()
	if len(id1) != 32 {
		t.Fatalf("id length = %d, want 32", len(id1))
	}
	if len(id2) != 32 {
		t.Fatalf("id length = %d, want 32", len(id2))
	}
	if id1 == id2 {
		t.Fatalf("expected unique ids, got same value %q", id1)
	}
}
