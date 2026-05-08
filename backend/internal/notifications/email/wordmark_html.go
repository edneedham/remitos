package email

import (
	"fmt"
	"strings"
)

// WordmarkPNGPath is served from website/public (absolute path on the marketing site).
const WordmarkPNGPath = "/enpunto-wordmark.png"

// HTMLWordmarkBlock returns a header image when publicSiteURL is set so remote images load in clients.
func HTMLWordmarkBlock(publicSiteURL string) string {
	base := strings.TrimRight(strings.TrimSpace(publicSiteURL), "/")
	if base == "" {
		return ""
	}
	src := escapeHTML(base + WordmarkPNGPath)
	return fmt.Sprintf(`<p style="margin:0 0 20px 0;line-height:0;">
<img src="%s" alt="En Punto" width="180" style="display:block;border:0;outline:none;text-decoration:none;max-width:180px;height:auto;">
</p>`, src)
}
