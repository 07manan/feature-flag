package service

import (
	"context"
	"encoding/json"
	"errors"
	"strconv"

	"github.com/google/uuid"

	"github.com/manan/feature-flag/evaluation-api/internal/domain"
	"github.com/manan/feature-flag/evaluation-api/internal/repository"
	"github.com/manan/feature-flag/evaluation-api/pkg/hash"
)

var (
	ErrInvalidAPIKey   = errors.New("invalid or missing API key")
	ErrFlagNotFound    = errors.New("flag not found")
	ErrInvalidFlagType = errors.New("invalid flag type")
)

type EvaluationService struct {
	repo *repository.Repository
}

func New(repo *repository.Repository) *EvaluationService {
	return &EvaluationService{repo: repo}
}

func (s *EvaluationService) AuthenticateAPIKey(ctx context.Context, apiKey string) (*domain.Environment, error) {
	if apiKey == "" {
		return nil, ErrInvalidAPIKey
	}

	env, err := s.repo.GetEnvironmentByAPIKey(ctx, apiKey)
	if err != nil {
		if errors.Is(err, repository.ErrNotFound) {
			return nil, ErrInvalidAPIKey
		}
		return nil, err
	}

	return env, nil
}

func (s *EvaluationService) EvaluateFlag(ctx context.Context, env *domain.Environment, flagKey string, userID string) (*domain.EvaluationResult, error) {
	flag, err := s.repo.GetFlagByKey(ctx, flagKey)
	if err != nil {
		if errors.Is(err, repository.ErrNotFound) {
			return nil, ErrFlagNotFound
		}
		return nil, err
	}

	flagValue, err := s.repo.GetFlagValue(ctx, flag.ID, env.ID)
	if err != nil {
		if errors.Is(err, repository.ErrNotFound) {
			// No environment-specific override, use default value
			return s.createResult(flag.Key, flag.DefaultValue, flag.Type, true, nil)
		}
		return nil, err
	}

	variants, err := s.repo.GetFlagValueVariants(ctx, flagValue.ID)
	if err != nil {
		return nil, err
	}

	if len(variants) == 0 {
		return s.createResult(flag.Key, flag.DefaultValue, flag.Type, true, nil)
	}

	selectedVariant := s.selectVariant(variants, flagKey, userID)
	if selectedVariant == nil {
		// User falls outside all variant percentages, use default
		return s.createResult(flag.Key, flag.DefaultValue, flag.Type, true, nil)
	}

	return s.createResult(flag.Key, selectedVariant.Value, flag.Type, false, &selectedVariant.ID)
}

func (s *EvaluationService) EvaluateAllFlags(ctx context.Context, env *domain.Environment, userID string) (*domain.BulkEvaluationResult, error) {
	flags, err := s.repo.GetAllActiveFlags(ctx)
	if err != nil {
		return nil, err
	}

	flagValues, err := s.repo.GetFlagValuesForEnvironment(ctx, env.ID)
	if err != nil {
		return nil, err
	}

	results := make(map[string]domain.EvaluationResult)

	for _, flag := range flags {
		flagValue, hasOverride := flagValues[flag.Key]

		if !hasOverride {
			// No environment-specific override, use default value
			result, err := s.createResult(flag.Key, flag.DefaultValue, flag.Type, true, nil)
			if err != nil {
				continue // Skip flags with parsing errors
			}
			results[flag.Key] = *result
			continue
		}

		variants, err := s.repo.GetFlagValueVariants(ctx, flagValue.ID)
		if err != nil {
			continue // Skip on error
		}

		if len(variants) == 0 {
			result, err := s.createResult(flag.Key, flag.DefaultValue, flag.Type, true, nil)
			if err != nil {
				continue
			}
			results[flag.Key] = *result
			continue
		}

		selectedVariant := s.selectVariant(variants, flag.Key, userID)
		if selectedVariant == nil {
			result, err := s.createResult(flag.Key, flag.DefaultValue, flag.Type, true, nil)
			if err != nil {
				continue
			}
			results[flag.Key] = *result
			continue
		}

		result, err := s.createResult(flag.Key, selectedVariant.Value, flag.Type, false, &selectedVariant.ID)
		if err != nil {
			continue
		}
		results[flag.Key] = *result
	}

	return &domain.BulkEvaluationResult{Flags: results}, nil
}

func (s *EvaluationService) selectVariant(variants []domain.FlagValueVariant, flagKey, userID string) *domain.FlagValueVariant {
	if len(variants) == 0 {
		return nil
	}

	// If there's only one variant with 100%, return it directly
	if len(variants) == 1 && variants[0].Percentage == 100 {
		return &variants[0]
	}

	// Compute user's bucket (0-99)
	bucket := hash.ComputeBucket(flagKey, userID)

	// Find which variant the user falls into
	// Variants are ordered, and we accumulate percentages
	cumulative := 0
	for i := range variants {
		cumulative += variants[i].Percentage
		if bucket < cumulative {
			return &variants[i]
		}
	}

	// User falls outside all percentages (e.g., total < 100%)
	return nil
}

func (s *EvaluationService) createResult(flagKey, rawValue string, flagType domain.FlagType, isDefault bool, variantID *uuid.UUID) (*domain.EvaluationResult, error) {
	var value interface{}

	switch flagType {
	case domain.FlagTypeBoolean:
		boolVal, err := strconv.ParseBool(rawValue)
		if err != nil {
			return nil, ErrInvalidFlagType
		}
		value = boolVal
	case domain.FlagTypeNumber:
		// Try parsing as float first (handles both int and float)
		floatVal, err := strconv.ParseFloat(rawValue, 64)
		if err != nil {
			return nil, ErrInvalidFlagType
		}
		// If it's a whole number, return as int
		if floatVal == float64(int64(floatVal)) {
			value = int64(floatVal)
		} else {
			value = floatVal
		}
	case domain.FlagTypeString:
		value = rawValue
	default:
		// Try to parse as JSON for complex types
		var jsonVal interface{}
		if err := json.Unmarshal([]byte(rawValue), &jsonVal); err != nil {
			// Fall back to string
			value = rawValue
		} else {
			value = jsonVal
		}
	}

	return &domain.EvaluationResult{
		FlagKey:   flagKey,
		Value:     value,
		Type:      flagType,
		IsDefault: isDefault,
		VariantID: variantID,
	}, nil
}

func (s *EvaluationService) CheckHealth(ctx context.Context) error {
	return s.repo.Ping(ctx)
}
