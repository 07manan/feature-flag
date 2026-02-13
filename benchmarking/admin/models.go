package admin

import "time"

// Environment represents an environment from the Admin API.
type Environment struct {
	ID          string    `json:"id"`
	Key         string    `json:"key"`
	Name        string    `json:"name"`
	Description string    `json:"description"`
	IsActive    bool      `json:"isActive"`
	APIKey      string    `json:"apiKey"`
	CreatedAt   time.Time `json:"createdAt"`
	UpdatedAt   time.Time `json:"updatedAt"`
}

// Flag represents a feature flag from the Admin API.
type Flag struct {
	ID           string    `json:"id"`
	Key          string    `json:"key"`
	Name         string    `json:"name"`
	Description  string    `json:"description"`
	Type         string    `json:"type"`
	DefaultValue string    `json:"defaultValue"`
	IsActive     bool      `json:"isActive"`
	CreatedAt    time.Time `json:"createdAt"`
	UpdatedAt    time.Time `json:"updatedAt"`
}

// FlagValue represents a flag value configuration for a specific environment.
type FlagValue struct {
	ID             string    `json:"id"`
	FlagID         string    `json:"flagId"`
	FlagKey        string    `json:"flagKey"`
	FlagType       string    `json:"flagType"`
	EnvironmentID  string    `json:"environmentId"`
	EnvironmentKey string    `json:"environmentKey"`
	Variants       []Variant `json:"variants"`
	IsActive       bool      `json:"isActive"`
	CreatedAt      time.Time `json:"createdAt"`
	UpdatedAt      time.Time `json:"updatedAt"`
}

// Variant represents a percentage-based rollout variant.
type Variant struct {
	ID         string  `json:"id"`
	Value      string  `json:"value"`
	Percentage float64 `json:"percentage"`
}

// DiscoveryResult holds the data discovered from the Admin API.
type DiscoveryResult struct {
	Environments []Environment
	Flags        []Flag
	FlagValues   []FlagValue
}
