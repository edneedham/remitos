package handlers

import "testing"

func TestMultipartFormDataContentType(t *testing.T) {
	tests := []struct {
		ct   string
		want bool
	}{
		{"multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW", true},
		{"Multipart/Form-Data; boundary=something", true},
		{"application/json", false},
		{"", false},
		{"bogus", false},
	}
	for _, tt := range tests {
		if got := MultipartFormDataContentType(tt.ct); got != tt.want {
			t.Errorf("MultipartFormDataContentType(%q) = %v, want %v", tt.ct, got, tt.want)
		}
	}
}
