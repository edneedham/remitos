package releases

import (
	"context"
	"fmt"
	"time"

	"cloud.google.com/go/storage"
)

// SignedGETURL returns a time-limited signed URL for downloading an object from GCS.
func SignedGETURL(ctx context.Context, client *storage.Client, bucketName, objectName string, expiry time.Duration) (url string, expiresAt time.Time, err error) {
	if client == nil || bucketName == "" || objectName == "" {
		return "", time.Time{}, fmt.Errorf("releases: missing client, bucket, or object")
	}
	expiresAt = time.Now().Add(expiry)
	bucket := client.Bucket(bucketName)
	u, err := bucket.SignedURL(objectName, &storage.SignedURLOptions{
		Method:  "GET",
		Expires: expiresAt,
	})
	if err != nil {
		return "", time.Time{}, err
	}
	return u, expiresAt, nil
}
