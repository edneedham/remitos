package mercadopago

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/hex"
	"net/http/httptest"
	"strconv"
	"testing"
	"time"
)

func TestVerifyWebhookSignature_KnownVector(t *testing.T) {
	const secret = "test-webhook-secret"
	dataID := "999999999"
	reqID := "rid-abc"
	ts := strconv.FormatInt(time.Now().Unix(), 10)
	manifest := "id:" + dataID + ";request-id:" + reqID + ";ts:" + ts + ";"
	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(manifest))
	wantSig := hex.EncodeToString(mac.Sum(nil))

	req := httptest.NewRequest("POST", "/webhooks/mercadopago?data.id="+dataID, nil)
	req.Header.Set("x-signature", "ts="+ts+",v1="+wantSig)
	req.Header.Set("x-request-id", reqID)

	if !VerifyWebhookSignature(req, secret) {
		t.Fatal("expected signature valid")
	}
}

func TestVerifyWebhookSignature_WrongSecret(t *testing.T) {
	req := httptest.NewRequest("POST", "/webhooks/mercadopago?data.id=1", nil)
	req.Header.Set("x-signature", "ts=1704908010,v1=deadbeef")
	req.Header.Set("x-request-id", "x")
	if VerifyWebhookSignature(req, "good-secret") {
		t.Fatal("expected invalid")
	}
}
