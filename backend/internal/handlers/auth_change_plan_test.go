package handlers

import (
	"testing"
)

func TestPlanTier(t *testing.T) {
	cases := []struct {
		plan string
		want int
	}{
		{"pyme", 1},
		{"PyME", 1},
		{"  empresa ", 2},
		{"empresa", 2},
		{"trial", 0},
		{"corporativo", 0},
		{"", 0},
	}
	for _, c := range cases {
		if got := planTier(c.plan); got != c.want {
			t.Errorf("planTier(%q) = %d, want %d", c.plan, got, c.want)
		}
	}
}

func TestPlanLimitsCatalog(t *testing.T) {
	maxW, maxU, maxD := planLimits("pyme")
	if maxW == nil || *maxW != 2 {
		t.Errorf("pyme: expected max_warehouses=2, got %v", maxW)
	}
	if maxU == nil || *maxU != 3 {
		t.Errorf("pyme: expected max_users=3, got %v", maxU)
	}
	if maxD == nil || *maxD != 500 {
		t.Errorf("pyme: expected documents_monthly_limit=500, got %v", maxD)
	}

	maxW, maxU, maxD = planLimits("empresa")
	if maxW == nil || *maxW != 3 {
		t.Errorf("empresa: expected max_warehouses=3, got %v", maxW)
	}
	if maxU == nil || *maxU != 10 {
		t.Errorf("empresa: expected max_users=10, got %v", maxU)
	}
	if maxD == nil || *maxD != 10000 {
		t.Errorf("empresa: expected documents_monthly_limit=10000, got %v", maxD)
	}

	maxW, maxU, maxD = planLimits("corporativo")
	if maxW != nil || maxU != nil || maxD != nil {
		t.Errorf("corporativo: expected nil limits, got %v %v %v", maxW, maxU, maxD)
	}
}
