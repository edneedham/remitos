package mercadopago

import (
	"crypto/hmac"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/hex"
	"net/http"
	"strconv"
	"strings"
	"time"
)

// VerifyWebhookSignature checks Mercado Pago's x-signature header for notifications
// configured through "Your integrations" (HMAC-SHA256 over a manifest). See:
// https://www.mercadopago.com.ar/developers/en/docs/your-integrations/notifications/webhooks
func VerifyWebhookSignature(r *http.Request, secret string) bool {
	secret = strings.TrimSpace(secret)
	if secret == "" {
		return false
	}
	sigHeader := strings.TrimSpace(r.Header.Get("x-signature"))
	if sigHeader == "" {
		return false
	}
	reqID := strings.TrimSpace(r.Header.Get("x-request-id"))
	dataID := strings.TrimSpace(r.URL.Query().Get("data.id"))

	var ts, wantHex string
	for _, part := range strings.Split(sigHeader, ",") {
		kv := strings.SplitN(strings.TrimSpace(part), "=", 2)
		if len(kv) != 2 {
			continue
		}
		switch strings.TrimSpace(kv[0]) {
		case "ts":
			ts = strings.TrimSpace(kv[1])
		case "v1":
			wantHex = strings.TrimSpace(kv[1])
		}
	}
	if ts == "" || wantHex == "" {
		return false
	}

	manifest := "id:" + dataID + ";request-id:" + reqID + ";ts:" + ts + ";"
	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(manifest))
	gotHex := hex.EncodeToString(mac.Sum(nil))

	wantBytes, err := hex.DecodeString(wantHex)
	if err != nil {
		return false
	}
	gotBytes, err := hex.DecodeString(gotHex)
	if err != nil || len(wantBytes) != len(gotBytes) {
		return false
	}
	if subtle.ConstantTimeCompare(wantBytes, gotBytes) != 1 {
		return false
	}

	// Replay window: allow modest clock skew and delayed delivery.
	sec, err := strconv.ParseInt(ts, 10, 64)
	if err == nil {
		delta := time.Now().Unix() - sec
		if delta < -120 || delta > 600 {
			return false
		}
	}
	return true
}
