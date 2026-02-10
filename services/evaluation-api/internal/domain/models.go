package domain

import (
	"time"

	"github.com/google/uuid"
)

type FlagType string

const (
	FlagTypeString  FlagType = "STRING"
	FlagTypeBoolean FlagType = "BOOLEAN"
	FlagTypeNumber  FlagType = "NUMBER"
)

type Flag struct {
	ID           uuid.UUID `db:"id"`
	Key          string    `db:"key"`
	Name         string    `db:"name"`
	Description  *string   `db:"description"`
	Type         FlagType  `db:"type"`
	DefaultValue string    `db:"default_value"`
	IsActive     bool      `db:"is_active"`
	CreatedAt    time.Time `db:"created_at"`
	UpdatedAt    time.Time `db:"updated_at"`
}

type Environment struct {
	ID          uuid.UUID `db:"id"`
	Key         string    `db:"key"`
	Name        string    `db:"name"`
	Description *string   `db:"description"`
	IsActive    bool      `db:"is_active"`
	APIKey      string    `db:"api_key"`
	CreatedAt   time.Time `db:"created_at"`
	UpdatedAt   time.Time `db:"updated_at"`
}

type FlagValue struct {
	ID            uuid.UUID `db:"id"`
	FlagID        uuid.UUID `db:"flag_id"`
	EnvironmentID uuid.UUID `db:"environment_id"`
	IsActive      bool      `db:"is_active"`
	CreatedAt     time.Time `db:"created_at"`
	UpdatedAt     time.Time `db:"updated_at"`
}

type FlagValueVariant struct {
	ID           uuid.UUID `db:"id"`
	FlagValueID  uuid.UUID `db:"flag_value_id"`
	Value        string    `db:"value"`
	Percentage   int       `db:"percentage"`
	VariantOrder int       `db:"variant_order"`
}

type EvaluationResult struct {
	FlagKey   string      `json:"flagKey"`
	Value     interface{} `json:"value"`
	Type      FlagType    `json:"type"`
	IsDefault bool        `json:"isDefault"`
	VariantID *uuid.UUID  `json:"variantId,omitempty"`
}

type BulkEvaluationResult struct {
	Flags map[string]EvaluationResult `json:"flags"`
}
