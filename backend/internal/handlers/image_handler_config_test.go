package handlers

import "testing"

func TestNewImageHandler_nilWhenBucketUnset(t *testing.T) {
	t.Setenv("GCS_BUCKET_NAME", "")
	h, err := NewImageHandler(nil, nil)
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if h != nil {
		t.Fatalf("expected nil handler when GCS_BUCKET_NAME is empty")
	}
}
