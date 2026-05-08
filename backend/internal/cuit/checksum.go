package cuit

import "unicode"

// ValidChecksum returns true if s is exactly 11 ASCII digits and satisfies the Argentine
// CUIT/CUIL verifier algorithm (weights 5,4,3,2,7,6,5,4,3,2 mod 11).
func ValidChecksum(s string) bool {
	if len(s) != 11 {
		return false
	}
	var digits [11]byte
	for i := 0; i < 11; i++ {
		r := rune(s[i])
		if r < '0' || r > '9' {
			return false
		}
		digits[i] = byte(r - '0')
	}
	mult := [...]int{5, 4, 3, 2, 7, 6, 5, 4, 3, 2}
	sum := 0
	for i := 0; i < 10; i++ {
		sum += int(digits[i]) * mult[i]
	}
	mod := sum % 11
	dv := 11 - mod
	if dv == 11 {
		dv = 0
	}
	if dv == 10 {
		dv = 9
	}
	return dv == int(digits[10])
}

// DigitsOnly keeps ASCII digits; does not validate length or checksum.
func DigitsOnly(s string) string {
	var b []byte
	for _, r := range s {
		if unicode.IsDigit(r) && r < 128 {
			b = append(b, byte(r))
		}
	}
	return string(b)
}
