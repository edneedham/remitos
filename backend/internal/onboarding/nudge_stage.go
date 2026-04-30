package onboarding

import (
	"database/sql"
	"time"
)

// PickNudgeStage returns which email to send: "day3", "day1", "setup", or "".
// Priority: if multiple thresholds apply, the highest stage wins (e.g. after downtime, send day3 not three emails).
func PickNudgeStage(now, activation time.Time, setupSent, day1Sent, day3Sent sql.NullTime) string {
	age := now.Sub(activation)
	const (
		setupMin = 10 * time.Minute
		day1Min  = 24 * time.Hour
		day3Min  = 72 * time.Hour
	)

	if !day3Sent.Valid && age >= day3Min {
		return "day3"
	}
	if !day1Sent.Valid && age >= day1Min {
		return "day1"
	}
	if !setupSent.Valid && age >= setupMin {
		return "setup"
	}
	return ""
}
