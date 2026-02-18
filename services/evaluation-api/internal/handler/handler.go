package handler

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"

	"github.com/go-chi/chi/v5"

	"github.com/manan/feature-flag/evaluation-api/internal/service"
)

const (
	headerAPIKey     = "X-API-Key"
	queryParamUserID = "user"
)

type Handler struct {
	svc    *service.EvaluationService
	logger *slog.Logger
}

func New(svc *service.EvaluationService, logger *slog.Logger) *Handler {
	return &Handler{
		svc:    svc,
		logger: logger,
	}
}

type ErrorResponse struct {
	Error   string `json:"error"`
	Message string `json:"message,omitempty"`
}

func (h *Handler) EvaluateFlag(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	apiKey := r.Header.Get(headerAPIKey)
	env, err := h.svc.AuthenticateAPIKey(ctx, apiKey)
	if err != nil {
		if errors.Is(err, service.ErrInvalidAPIKey) {
			h.respondError(w, http.StatusUnauthorized, "unauthorized", "Invalid or missing API key")
			return
		}
		h.logger.Error("failed to authenticate API key", "error", err)
		h.respondError(w, http.StatusInternalServerError, "internal_error", "Internal server error")
		return
	}

	flagKey := chi.URLParam(r, "flagKey")
	if flagKey == "" {
		h.respondError(w, http.StatusBadRequest, "bad_request", "Flag key is required")
		return
	}

	userID := r.URL.Query().Get(queryParamUserID)

	result, err := h.svc.EvaluateFlag(ctx, env, flagKey, userID)
	if err != nil {
		if errors.Is(err, service.ErrFlagNotFound) {
			h.respondError(w, http.StatusNotFound, "not_found", "Flag not found")
			return
		}
		h.logger.Error("failed to evaluate flag", "flagKey", flagKey, "error", err)
		h.respondError(w, http.StatusInternalServerError, "internal_error", "Internal server error")
		return
	}

	h.respondJSON(w, http.StatusOK, result)
}

func (h *Handler) EvaluateAllFlags(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	apiKey := r.Header.Get(headerAPIKey)
	env, err := h.svc.AuthenticateAPIKey(ctx, apiKey)
	if err != nil {
		if errors.Is(err, service.ErrInvalidAPIKey) {
			h.respondError(w, http.StatusUnauthorized, "unauthorized", "Invalid or missing API key")
			return
		}
		h.logger.Error("failed to authenticate API key", "error", err)
		h.respondError(w, http.StatusInternalServerError, "internal_error", "Internal server error")
		return
	}

	userID := r.URL.Query().Get(queryParamUserID)

	result, err := h.svc.EvaluateAllFlags(ctx, env, userID)
	if err != nil {
		h.logger.Error("failed to evaluate all flags", "error", err)
		h.respondError(w, http.StatusInternalServerError, "internal_error", "Internal server error")
		return
	}

	h.respondJSON(w, http.StatusOK, result)
}

func (h *Handler) Health(w http.ResponseWriter, r *http.Request) {
	h.respondJSON(w, http.StatusOK, map[string]string{"status": "ok"})
}

func (h *Handler) Ready(w http.ResponseWriter, r *http.Request) {
	ctx := r.Context()

	if err := h.svc.CheckHealth(ctx); err != nil {
		h.logger.Warn("readiness check failed", "error", err)
		h.respondJSON(w, http.StatusServiceUnavailable, map[string]string{
			"status": "unavailable",
			"reason": err.Error(),
		})
		return
	}

	h.respondJSON(w, http.StatusOK, map[string]string{"status": "ready"})
}

func (h *Handler) respondJSON(w http.ResponseWriter, status int, data interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)

	if err := json.NewEncoder(w).Encode(data); err != nil {
		h.logger.Error("failed to encode response", "error", err)
	}
}

func (h *Handler) respondError(w http.ResponseWriter, status int, errorCode, message string) {
	h.respondJSON(w, status, ErrorResponse{
		Error:   errorCode,
		Message: message,
	})
}
