package email

import (
	"strconv"
	"strings"
)

// FormatARSWholeWithDots formats integer pesos with Argentine thousands separators (e.g. 13200 → "13.200").
func FormatARSWholeWithDots(n int64) string {
	if n < 0 {
		n = -n
	}
	s := strconv.FormatInt(n, 10)
	if len(s) <= 3 {
		return s
	}
	var b strings.Builder
	lead := len(s) % 3
	if lead == 0 {
		lead = 3
	}
	b.WriteString(s[:lead])
	for i := lead; i < len(s); i += 3 {
		b.WriteString(".")
		b.WriteString(s[i : i+3])
	}
	return b.String()
}
