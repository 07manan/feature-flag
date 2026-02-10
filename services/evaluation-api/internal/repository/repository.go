package repository

import (
	"context"
	"errors"
	"fmt"

	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"

	"github.com/manan/feature-flag/evaluation-api/internal/domain"
)

var (
	ErrNotFound = errors.New("not found")
)

type Repository struct {
	pool *pgxpool.Pool
}

func New(pool *pgxpool.Pool) *Repository {
	return &Repository{pool: pool}
}

func (r *Repository) GetEnvironmentByAPIKey(ctx context.Context, apiKey string) (*domain.Environment, error) {
	query := `
		SELECT id, key, name, description, is_active, api_key, created_at, updated_at
		FROM environments
		WHERE api_key = $1 AND is_active = true
	`

	var env domain.Environment
	err := r.pool.QueryRow(ctx, query, apiKey).Scan(
		&env.ID,
		&env.Key,
		&env.Name,
		&env.Description,
		&env.IsActive,
		&env.APIKey,
		&env.CreatedAt,
		&env.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrNotFound
		}
		return nil, fmt.Errorf("query environment by api key: %w", err)
	}

	return &env, nil
}

func (r *Repository) GetFlagByKey(ctx context.Context, flagKey string) (*domain.Flag, error) {
	query := `
		SELECT id, key, name, description, type, default_value, is_active, created_at, updated_at
		FROM flags
		WHERE key = $1 AND is_active = true
	`

	var flag domain.Flag
	err := r.pool.QueryRow(ctx, query, flagKey).Scan(
		&flag.ID,
		&flag.Key,
		&flag.Name,
		&flag.Description,
		&flag.Type,
		&flag.DefaultValue,
		&flag.IsActive,
		&flag.CreatedAt,
		&flag.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrNotFound
		}
		return nil, fmt.Errorf("query flag by key: %w", err)
	}

	return &flag, nil
}

func (r *Repository) GetAllActiveFlags(ctx context.Context) ([]domain.Flag, error) {
	query := `
		SELECT id, key, name, description, type, default_value, is_active, created_at, updated_at
		FROM flags
		WHERE is_active = true
		ORDER BY key
	`

	rows, err := r.pool.Query(ctx, query)
	if err != nil {
		return nil, fmt.Errorf("query all active flags: %w", err)
	}
	defer rows.Close()

	var flags []domain.Flag
	for rows.Next() {
		var flag domain.Flag
		err := rows.Scan(
			&flag.ID,
			&flag.Key,
			&flag.Name,
			&flag.Description,
			&flag.Type,
			&flag.DefaultValue,
			&flag.IsActive,
			&flag.CreatedAt,
			&flag.UpdatedAt,
		)
		if err != nil {
			return nil, fmt.Errorf("scan flag row: %w", err)
		}
		flags = append(flags, flag)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate flag rows: %w", err)
	}

	return flags, nil
}

func (r *Repository) GetFlagValue(ctx context.Context, flagID, environmentID interface{}) (*domain.FlagValue, error) {
	query := `
		SELECT id, flag_id, environment_id, is_active, created_at, updated_at
		FROM flag_values
		WHERE flag_id = $1 AND environment_id = $2 AND is_active = true
	`

	var fv domain.FlagValue
	err := r.pool.QueryRow(ctx, query, flagID, environmentID).Scan(
		&fv.ID,
		&fv.FlagID,
		&fv.EnvironmentID,
		&fv.IsActive,
		&fv.CreatedAt,
		&fv.UpdatedAt,
	)
	if err != nil {
		if errors.Is(err, pgx.ErrNoRows) {
			return nil, ErrNotFound
		}
		return nil, fmt.Errorf("query flag value: %w", err)
	}

	return &fv, nil
}

func (r *Repository) GetFlagValueVariants(ctx context.Context, flagValueID interface{}) ([]domain.FlagValueVariant, error) {
	query := `
		SELECT id, flag_value_id, value, percentage, variant_order
		FROM flag_value_variants
		WHERE flag_value_id = $1
		ORDER BY variant_order
	`

	rows, err := r.pool.Query(ctx, query, flagValueID)
	if err != nil {
		return nil, fmt.Errorf("query flag value variants: %w", err)
	}
	defer rows.Close()

	var variants []domain.FlagValueVariant
	for rows.Next() {
		var v domain.FlagValueVariant
		err := rows.Scan(
			&v.ID,
			&v.FlagValueID,
			&v.Value,
			&v.Percentage,
			&v.VariantOrder,
		)
		if err != nil {
			return nil, fmt.Errorf("scan variant row: %w", err)
		}
		variants = append(variants, v)
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate variant rows: %w", err)
	}

	return variants, nil
}

func (r *Repository) GetFlagValuesForEnvironment(ctx context.Context, environmentID interface{}) (map[string]*domain.FlagValue, error) {
	query := `
		SELECT fv.id, fv.flag_id, fv.environment_id, fv.is_active, fv.created_at, fv.updated_at, f.key
		FROM flag_values fv
		JOIN flags f ON f.id = fv.flag_id
		WHERE fv.environment_id = $1 AND fv.is_active = true AND f.is_active = true
	`

	rows, err := r.pool.Query(ctx, query, environmentID)
	if err != nil {
		return nil, fmt.Errorf("query flag values for environment: %w", err)
	}
	defer rows.Close()

	result := make(map[string]*domain.FlagValue)
	for rows.Next() {
		var fv domain.FlagValue
		var flagKey string
		err := rows.Scan(
			&fv.ID,
			&fv.FlagID,
			&fv.EnvironmentID,
			&fv.IsActive,
			&fv.CreatedAt,
			&fv.UpdatedAt,
			&flagKey,
		)
		if err != nil {
			return nil, fmt.Errorf("scan flag value row: %w", err)
		}
		fvCopy := fv
		result[flagKey] = &fvCopy
	}

	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate flag value rows: %w", err)
	}

	return result, nil
}

func (r *Repository) Ping(ctx context.Context) error {
	return r.pool.Ping(ctx)
}
