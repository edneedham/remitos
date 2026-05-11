package handlers

import (
	"encoding/json"
	"net/http"
	"strconv"
	"strings"
	"time"

	"github.com/google/uuid"
	"server/internal/billing"
	"server/internal/httputil"
	"server/internal/logger"
	"server/internal/middleware"
	"server/internal/models"
	"server/internal/notifications/inapp"
	"server/internal/repository"
)

type SyncHandler struct {
	syncRepo         *repository.SyncRepository
	companyRepo      *repository.CompanyRepository
	notificationRepo *repository.UserNotificationRepository
	inApp            *inapp.Broadcaster
}

func projectedNewInboundNotes(totalIncoming int, existingByCloudID int64, newWithoutCloudID int) int64 {
	projected := int64(totalIncoming) - existingByCloudID + int64(newWithoutCloudID)
	if projected < 0 {
		return 0
	}
	return projected
}

func NewSyncHandler(
	syncRepo *repository.SyncRepository,
	companyRepo *repository.CompanyRepository,
	notificationRepo *repository.UserNotificationRepository,
	inApp *inapp.Broadcaster,
) *SyncHandler {
	return &SyncHandler{
		syncRepo:         syncRepo,
		companyRepo:      companyRepo,
		notificationRepo: notificationRepo,
		inApp:            inApp,
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
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}

	httputil.LimitRequestBody(w, r, httputil.MaxSyncRequestBody)
	var req models.SyncRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		if httputil.IsMaxBytesError(err) {
			RespondWithError(w, r, ErrCodeInvalidRequest, "La solicitud de sincronización es demasiado grande.", http.StatusRequestEntityTooLarge, err)
			return
		}
		RespondWithError(w, r, ErrCodeInvalidRequest, "Cuerpo de solicitud inválido", http.StatusBadRequest, err)
		return
	}

	logger.Log.Debug().
		Str("user_id", userClaims.UserID).
		Str("company_id", userClaims.CompanyID).
		Int("inbound_notes", len(req.InboundNotes)).
		Int("outbound_lists", len(req.OutboundLists)).
		Msg("sync request")

	ctx := r.Context()

	companyID, err := uuid.Parse(userClaims.CompanyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Empresa inválida", http.StatusBadRequest)
		return
	}
	company, err := h.companyRepo.GetByIDForBilling(ctx, companyID)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error al validar límites del plan", http.StatusInternalServerError, err)
		return
	}
	if company == nil {
		RespondWithError(w, r, ErrCodeNotFound, "Empresa no encontrada", http.StatusNotFound)
		return
	}

	if company.DocumentsMonthlyLimit != nil && len(req.InboundNotes) > 0 {
		mtdTotal, err := h.syncRepo.InboundNotesMTDCount(ctx, companyID)
		if err != nil {
			RespondWithError(w, r, ErrCodeInternalError, "Error al validar límites del plan", http.StatusInternalServerError, err)
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
			RespondWithError(w, r, ErrCodeInternalError, "Error al validar límites del plan", http.StatusInternalServerError, err)
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
				r,
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
			RespondWithError(w, r, ErrCodeInternalError, "Error al sincronizar remitos de ingreso", http.StatusInternalServerError, err)
			return
		}

		outboundListMappings, outboundLineMappings, err = h.syncRepo.UpsertOutboundLists(ctx, userClaims.CompanyID, req.OutboundLists)
		if err != nil {
			RespondWithError(w, r, ErrCodeInternalError, "Error al sincronizar listas de reparto", http.StatusInternalServerError, err)
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

		if h.inApp != nil && h.notificationRepo != nil {
			_, lifetime, _, errM := h.syncRepo.InboundNoteEntitlementMetrics(ctx, companyID)
			if errM == nil && lifetime >= 1 {
				dup, errD := h.notificationRepo.ExistsCompanyKind(ctx, companyID, string(models.UserNotificationKindFirstScanCompleted))
				if errD == nil && !dup {
					h.inApp.FirstScanCompleted(ctx, companyID)
				}
			}
			if company.DocumentsMonthlyLimit != nil {
				mtdTotal, _, errMTD := h.syncRepo.InboundNotesMTDCumulativeSeries(ctx, companyID)
				if errMTD == nil {
					limit := int64(*company.DocumentsMonthlyLimit)
					if limit > 0 && mtdTotal >= (limit*9)/10 && mtdTotal < limit {
						dupM, errDup := h.notificationRepo.ExistsCompanyKindInUTCMonth(ctx, companyID, string(models.UserNotificationKindDocumentsUsageWarning))
						if errDup == nil && !dupM {
							h.inApp.DocumentsUsageWarning(ctx, companyID, mtdTotal, limit)
						}
					}
				}
			}
		}
	}

	serverInboundNotes, err := h.syncRepo.GetInboundNotesSince(ctx, userClaims.CompanyID, time.Unix(req.LastSyncTimestamp, 0))
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error al obtener cambios del servidor", http.StatusInternalServerError, err)
		return
	}

	serverOutboundLists, err := h.syncRepo.GetOutboundListsSince(ctx, userClaims.CompanyID, time.Unix(req.LastSyncTimestamp, 0))
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error al obtener cambios del servidor", http.StatusInternalServerError, err)
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
		Conflicts:      []interface{}{},
		UploadsApplied: uploadsAllowed,
	}

	RespondWithJSON(w, http.StatusOK, response)
}

func (h *SyncHandler) GetSyncStatus(w http.ResponseWriter, r *http.Request) {
	userClaims := middleware.GetUserClaims(r)
	if userClaims.UserID == "" {
		RespondWithError(w, r, ErrCodeUnauthorized, "No autorizado", http.StatusUnauthorized)
		return
	}

	lastSyncStr := r.URL.Query().Get("last_sync")
	if lastSyncStr == "" {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Falta el parámetro last_sync", http.StatusBadRequest)
		return
	}

	var lastSync time.Time
	if ts, err := strconv.ParseInt(lastSyncStr, 10, 64); err == nil {
		lastSync = time.Unix(ts, 0)
	} else {
		RespondWithError(w, r, ErrCodeInvalidRequest, "Formato de marca de tiempo inválido", http.StatusBadRequest)
		return
	}

	ctx := r.Context()

	inboundCount, err := h.syncRepo.GetInboundNotesCountSince(ctx, userClaims.CompanyID, lastSync)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error al consultar el estado de sincronización", http.StatusInternalServerError, err)
		return
	}

	outboundCount, err := h.syncRepo.GetOutboundListsCountSince(ctx, userClaims.CompanyID, lastSync)
	if err != nil {
		RespondWithError(w, r, ErrCodeInternalError, "Error al consultar el estado de sincronización", http.StatusInternalServerError, err)
		return
	}

	RespondWithJSON(w, http.StatusOK, map[string]interface{}{
		"pending_inbound_notes":  inboundCount,
		"pending_outbound_lists": outboundCount,
		"server_timestamp":       time.Now().Unix(),
	})
}
