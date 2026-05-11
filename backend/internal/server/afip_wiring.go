package server

import (
	"context"

	"server/config"
	"server/internal/logger"
	"server/internal/payments/afip"
	"server/internal/payments/afip/certprovider"
	"server/internal/payments/afip/wsaa"
	"server/internal/repository"
)

// initAfipClient builds the AFIP/ARCA client when AFIP_BILLING_ENABLED or AFIP_PADRON_ENABLED is set.
func initAfipClient(cfg *config.Config) (*afip.Client, bool, bool) {
	billingOn := cfg.AfipBillingEnabled
	padronOn := cfg.AfipPadronEnabled
	if !billingOn && !padronOn {
		return nil, false, false
	}
	if cfg.AfipCUIT == "" || cfg.AfipPuntoVenta <= 0 {
		logger.Log.Warn().Msg("AFIP feature flag set but AFIP_CUIT / AFIP_PUNTO_VENTA missing; AFIP integration disabled")
		return nil, false, false
	}

	var provider certprovider.CertProvider
	if cfg.AfipCertSecretName != "" && cfg.AfipKeySecretName != "" {
		provider = certprovider.NewGCPSecret(cfg.AfipCertSecretName, cfg.AfipKeySecretName)
	} else {
		provider = certprovider.NewEnv(cfg.AfipCertPEM, cfg.AfipCertPath, cfg.AfipKeyPEM, cfg.AfipKeyPath)
	}

	client, err := afip.New(afip.Config{
		Env:             cfg.AfipEnv,
		CUIT:            cfg.AfipCUIT,
		PuntoVenta:      cfg.AfipPuntoVenta,
		IssuerCondicion: cfg.AfipIssuerCondicionIVA,
		Concepto:        cfg.AfipConcepto,
		DefaultAlicuota: cfg.AfipDefaultAlicuotaIVA,
		Certs:           provider,
		WSAAURL:         cfg.AfipWSAAURL,
		WSFEv1URL:       cfg.AfipWSFEv1URL,
		PadronURL:       cfg.AfipPadronURL,
	})
	if err != nil {
		logger.Log.Warn().Err(err).Msg("AFIP client init failed; integration disabled")
		return nil, false, false
	}
	logger.Log.Info().
		Str("env", string(client.Env)).
		Str("cuit", client.CUIT).
		Int("pto_vta", client.PuntoVenta).
		Bool("billing", billingOn).
		Bool("padron", padronOn).
		Msg("AFIP/ARCA integration enabled")
	return client, billingOn, padronOn
}

func newAfipTAManager(client *afip.Client, repo *repository.AfipTicketRepository) *afip.TAManager {
	wsaaClient := wsaa.New(client.WSAAURL(), nil, client.Certs)
	store := &afip.FuncTAStore{
		GetFn: func(ctx context.Context, service string) (*afip.StoredTA, error) {
			row, err := repo.Get(ctx, service)
			if err != nil || row == nil {
				return nil, err
			}
			return &afip.StoredTA{
				Service:        row.Service,
				Token:          row.Token,
				Sign:           row.Sign,
				GenerationTime: row.GenerationTime,
				ExpirationTime: row.ExpirationTime,
			}, nil
		},
		UpsertFn: func(ctx context.Context, ta afip.StoredTA) error {
			return repo.Upsert(ctx, repository.AfipTicket{
				Service:        ta.Service,
				Token:          ta.Token,
				Sign:           ta.Sign,
				GenerationTime: ta.GenerationTime,
				ExpirationTime: ta.ExpirationTime,
			})
		},
	}
	return afip.NewTAManager(wsaaClient, store)
}
