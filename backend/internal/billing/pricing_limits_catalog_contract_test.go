package billing

import (
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"testing"
)

var webCatalogDocsPerMonthPattern = regexp.MustCompile(`Hasta ([\d.]+) documentos/mes`)

func parseEsThousandsInt(s string) int {
	s = strings.TrimSpace(strings.ReplaceAll(s, ".", ""))
	n, err := strconv.Atoi(s)
	if err != nil {
		return -1
	}
	return n
}

// TestWebPlanCatalogLimitsMatchPlanLimitsByID ensures marketing copy in planCatalog.ts
// stays aligned with billing.PlanLimitsByID (sync MTD enforcement uses the stored company row,
// which is derived from these caps when plans change).
func TestWebPlanCatalogLimitsMatchPlanLimitsByID(t *testing.T) {
	webCatalogPath := filepath.Join("..", "..", "..", "website", "src", "app", "lib", "planCatalog.ts")
	raw, err := os.ReadFile(webCatalogPath)
	if err != nil {
		t.Fatalf("read website plan catalog: %v", err)
	}

	body := string(raw)
	docMatches := webCatalogDocsPerMonthPattern.FindAllStringSubmatch(body, -1)
	if len(docMatches) < 2 {
		t.Fatalf("expected at least two Hasta … documentos/mes lines in %s", webCatalogPath)
	}

	maxWPyme, _, docsPyme := PlanLimitsByID("pyme")
	if docsPyme == nil {
		t.Fatal("pyme: expected documentsMonthlyLimit")
	}
	gotPyme := parseEsThousandsInt(docMatches[0][1])
	if gotPyme != *docsPyme {
		t.Fatalf("pyme documents in web catalog: got %d want %d", gotPyme, *docsPyme)
	}

	maxWEmpresa, maxUEmpresa, docsEmpresa := PlanLimitsByID("empresa")
	if docsEmpresa == nil {
		t.Fatal("empresa: expected documentsMonthlyLimit")
	}
	gotEmpresa := parseEsThousandsInt(docMatches[1][1])
	if gotEmpresa != *docsEmpresa {
		t.Fatalf("empresa documents in web catalog: got %d want %d", gotEmpresa, *docsEmpresa)
	}

	if maxWPyme == nil {
		t.Fatal("pyme: expected maxWarehouses")
	}
	if !strings.Contains(body, "'Hasta "+strconv.Itoa(*maxWPyme)+" depósitos'") {
		t.Fatalf("web catalog missing pyme warehouse cap %d", *maxWPyme)
	}

	if maxWEmpresa == nil || maxUEmpresa == nil {
		t.Fatal("empresa: expected maxWarehouses and maxUsers")
	}
	if !strings.Contains(body, "'"+strconv.Itoa(*maxWEmpresa)+" depósitos'") {
		t.Fatalf("web catalog missing empresa warehouse cap %d", *maxWEmpresa)
	}
	if !strings.Contains(body, "'Hasta "+strconv.Itoa(*maxUEmpresa)+" usuarios'") {
		t.Fatalf("web catalog missing empresa user cap %d", *maxUEmpresa)
	}
}
