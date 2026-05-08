package cuit

import (
	"strconv"
	"testing"
)

func TestValidChecksum_knownVectors(t *testing.T) {
	t.Parallel()
	// Round-trip: construct valid CUIT from first 10 digits using same algorithm as production.
	base := "3070755227"
	dv := verifierDigit(base)
	full := base + strconv.Itoa(dv)
	if !ValidChecksum(full) {
		t.Fatalf("expected constructed CUIT valid: %s", full)
	}

	cases := []struct {
		cuit  string
		valid bool
	}{
		{full, true},
		{"30707552279", false}, // wrong check digit
		{"12345678901", false},
		{"", false},
		{"3070755227", false}, // too short
	}
	for _, tc := range cases {
		if got := ValidChecksum(tc.cuit); got != tc.valid {
			t.Errorf("ValidChecksum(%q)=%v want %v", tc.cuit, got, tc.valid)
		}
	}
}

func verifierDigit(firstTen string) int {
	if len(firstTen) != 10 {
		return -1
	}
	mult := [...]int{5, 4, 3, 2, 7, 6, 5, 4, 3, 2}
	sum := 0
	for i := 0; i < 10; i++ {
		sum += int(firstTen[i]-'0') * mult[i]
	}
	mod := sum % 11
	dv := 11 - mod
	if dv == 11 {
		dv = 0
	}
	if dv == 10 {
		dv = 9
	}
	return dv
}
