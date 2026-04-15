package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"
	"time"

	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/repository"
)

type SyncHandler struct {
	syncRepo *repository.SyncRepository
}

func NewSyncHandler(syncRepo *repository.SyncRepository) *SyncHandler {
	return &SyncHandler{
		syncRepo: syncRepo,
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

	inboundNoteMappings, err := h.syncRepo.UpsertInboundNotes(ctx, userClaims.CompanyID, req.InboundNotes)
	if err != nil {
		logger.Log.Error().Err(err).Msg("Failed to upsert inbound notes")
		RespondWithError(w, ErrCodeInternalError, "Failed to sync inbound notes", http.StatusInternalServerError)
		return
	}

	outboundListMappings, outboundLineMappings, err := h.syncRepo.UpsertOutboundLists(ctx, userClaims.CompanyID, req.OutboundLists)
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
