package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/google/uuid"
	"server/internal/billing"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/repository"
)

type SyncHandler struct {
	syncRepo    *repository.SyncRepository
	companyRepo *repository.CompanyRepository
}

func projectedNewInboundNotes(totalIncoming int, existingByCloudID int64, newWithoutCloudID int) int64 {
	projected := int64(totalIncoming) - existingByCloudID + int64(newWithoutCloudID)
	if projected < 0 {
		return 0
	}
	return projected
}

func NewSyncHandler(syncRepo *repository.SyncRepository, companyRepo *repository.CompanyRepository) *SyncHandler {
	return &SyncHandler{
		syncRepo:    syncRepo,
		companyRepo: companyRepo,
	}
}

func (h *SyncHandler) Routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("POST /", h.Sync)
	mux.HandleFunc("GET /status", h.GetSyncStatus)
	return mux
}

func (h *SyncHandler) Sync(w http.ResponseWriter, r *http.Request) {
	userClaims := middleware.GetUserClaims(r)
	if userClaims.UserID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "Unauthorized", http.StatusUnauthorized)
		return
	}

	var req models.SyncRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Invalid request body", http.StatusBadRequest)
		return
	}

	logger.Log.Info().
		Str("user_id", userClaims.UserID).
		Str("company_id", userClaims.CompanyID).
		Int("inbound_notes", len(req.InboundNotes)).
		Int("outbound_lists", len(req.OutboundLists)).
		Msg("Processing sync request")

	ctx := r.Context()

	companyID, err := uuid.Parse(userClaims.CompanyID)
	if err != nil {
		RespondWithError(w, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	company, err := h.companyRepo.GetByIDForBilling(ctx, companyID)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Sync: company lookup")
		RespondWithError(w, ErrCodeInternalError, "Failed to validate limits", http.StatusInternalServerError)
		return
	}
	if company == nil {
		RespondWithError(w, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	if company.DocumentsMonthlyLimit != nil && len(req.InboundNotes) > 0 {
		mtdTotal, _, err := h.syncRepo.InboundNotesMTDCumulativeSeries(ctx, companyID)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Sync: MTD usage lookup")
			RespondWithError(w, ErrCodeInternalError, "Failed to validate limits", http.StatusInternalServerError)
			return
		}

		cloudIDs := make([]string, 0, len(req.InboundNotes))
		newWithoutCloudID := 0
		for _, note := range req.InboundNotes {
			if strings.TrimSpace(note.CloudID) == "" {
				newWithoutCloudID++
				continue
			}
			cloudIDs = append(cloudIDs, note.CloudID)
		}
		existingByCloudID, err := h.syncRepo.CountInboundNotesByCloudIDs(ctx, companyID, cloudIDs)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Sync: existing inbound notes by cloud id")
			RespondWithError(w, ErrCodeInternalError, "Failed to validate limits", http.StatusInternalServerError)
			return
		}

		projectedNewInbound := projectedNewInboundNotes(
			len(cloudIDs),
			existingByCloudID,
			newWithoutCloudID,
		)

		projectedTotal := mtdTotal + projectedNewInbound
		if projectedTotal > int64(*company.DocumentsMonthlyLimit) {
			RespondWithError(
				w,
				ErrCodeForbidden,
				"Límite de documentos de tu plan alcanzado para este mes.",
				http.StatusForbidden,
			)
			return
		}
	}

	uploadsAllowed := billing.CompanyHasAppDownloadAccess(time.Now(), company)
	inboundNoteMappings := make([]models.IdMapping, 0)
	outboundListMappings := make([]models.IdMapping, 0)
	outboundLineMappings := make([]models.IdMapping, 0)

	if uploadsAllowed {
		inboundNoteMappings, err = h.syncRepo.UpsertInboundNotes(ctx, userClaims.CompanyID, req.InboundNotes)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Failed to upsert inbound notes")
			RespondWithError(w, ErrCodeInternalError, "Failed to sync inbound notes", http.StatusInternalServerError)
			return
		}

		outboundListMappings, outboundLineMappings, err = h.syncRepo.UpsertOutboundLists(ctx, userClaims.CompanyID, req.OutboundLists)
		if err != nil {
			logger.Log.Error().Err(err).Msg("Failed to upsert outbound lists")
			RespondWithError(w, ErrCodeInternalError, "Failed to sync outbound lists", http.StatusInternalServerError)
			return
		}

		if len(req.StatusHistory) > 0 {
			if err := h.syncRepo.UpsertStatusHistory(ctx, userClaims.CompanyID, req.StatusHistory); err != nil {
				logger.Log.Error().Err(err).Msg("Failed to upsert status history")
			}
		}

		if len(req.EditHistory) > 0 {
			if err := h.syncRepo.UpsertEditHistory(ctx, userClaims.CompanyID, req.EditHistory); err != nil {
				logger.Log.Error().Err(err).Msg("Failed to upsert edit history")
			}
		}
	}

	serverInboundNotes, err := h.syncRepo.GetInboundNotesSince(ctx, userClaims.CompanyID, time.Unix(req.LastSyncTimestamp, 0))
	if err != nil {
		logger.Log.Error().Err(err).Msg("Failed to get inbound notes since timestamp")
		RespondWithError(w, ErrCodeInternalError, "Failed to fetch server changes", http.StatusInternalServerError)
		return
	}

	serverOutboundLists, err := h.syncRepo.GetOutboundListsSince(ctx, userClaims.CompanyID, time.Unix(req.LastSyncTimestamp, 0))
	if err != nil {
		logger.Log.Error().Err(err).Msg("Failed to get outbound lists since timestamp")
		RespondWithError(w, ErrCodeInternalError, "Failed to fetch server changes", http.StatusInternalServerError)
		return
	}

	response := models.SyncResponse{
		ServerTimestamp: time.Now().Unix(),
		InboundNotes:    serverInboundNotes,
		OutboundLists:   serverOutboundLists,
		IdMappings: models.SyncIdMappings{
			InboundNotes:  inboundNoteMappings,
			OutboundLists: outboundListMappings,
			OutboundLines: outboundLineMappings,
		},
		Conflicts: []interface{}{},
	}

	RespondWithJSON(w, http.StatusOK, response)
}

func (h *SyncHandler) GetSyncStatus(w http.ResponseWriter, r *http.Request) {
	userClaims := middleware.GetUserClaims(r)
	if userClaims.UserID == "" {
		RespondWithError(w, ErrCodeUnauthorized, "Unauthorized", http.StatusUnauthorized)
		return
	}

	lastSyncStr := r.URL.Query().Get("last_sync")
	if lastSyncStr == "" {
		RespondWithError(w, ErrCodeInvalidRequest, "last_sync parameter required", http.StatusBadRequest)
		return
	}

	var lastSync time.Time
	if ts, err := strconv.ParseInt(lastSyncStr, 10, 64); err == nil {
		lastSync = time.Unix(ts, 0)
	} else {
		RespondWithError(w, ErrCodeInvalidRequest, "Invalid timestamp format", http.StatusBadRequest)
		return
	}

	ctx := r.Context()

	inboundCount, err := h.syncRepo.GetInboundNotesCountSince(ctx, userClaims.CompanyID, lastSync)
	if err != nil {
		RespondWithError(w, ErrCodeInternalError, "Failed to query sync status", http.StatusInternalServerError)
		return
	}

	outboundCount, err := h.syncRepo.GetOutboundListsCountSince(ctx, userClaims.CompanyID, lastSync)
	if err != nil {
		RespondWithError(w, ErrCodeInternalError, "Failed to query sync status", http.StatusInternalServerError)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]interface{}{
		"pending_inbound_notes":  inboundCount,
		"pending_outbound_lists": outboundCount,
		"server_timestamp":       time.Now().Unix(),
	})
}
