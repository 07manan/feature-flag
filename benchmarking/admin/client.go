package admin

import (
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"
)

// Client communicates with the Admin API to discover environments, flags, and flag values.
type Client struct {
	baseURL    string
	token      string
	httpClient *http.Client
}

// NewClient creates a new Admin API client.
func NewClient(baseURL, token string) *Client {
	return &Client{
		baseURL: baseURL,
		token:   token,
		httpClient: &http.Client{
			Timeout: 15 * time.Second,
		},
	}
}

// Ping verifies the admin token is valid by making a lightweight request.
func (c *Client) Ping() error {
	req, err := http.NewRequest(http.MethodGet, c.baseURL+"/environments", nil)
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}
	c.setAuth(req)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("failed to connect to Admin API at %s: %w", c.baseURL, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusUnauthorized || resp.StatusCode == http.StatusForbidden {
		return fmt.Errorf("admin API authentication failed (HTTP %d) — check your --admin-token", resp.StatusCode)
	}
	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("admin API returned HTTP %d: %s", resp.StatusCode, string(body))
	}
	return nil
}

// FetchEnvironments retrieves all active environments from the Admin API.
func (c *Client) FetchEnvironments() ([]Environment, error) {
	var envs []Environment
	if err := c.getJSON("/environments", &envs); err != nil {
		return nil, fmt.Errorf("failed to fetch environments: %w", err)
	}

	// Filter to only active environments
	active := make([]Environment, 0, len(envs))
	for _, e := range envs {
		if e.IsActive {
			active = append(active, e)
		}
	}
	return active, nil
}

// FetchFlags retrieves all flags from the Admin API.
func (c *Client) FetchFlags() ([]Flag, error) {
	var flags []Flag
	if err := c.getJSON("/flags", &flags); err != nil {
		return nil, fmt.Errorf("failed to fetch flags: %w", err)
	}

	// Filter to only active flags
	active := make([]Flag, 0, len(flags))
	for _, f := range flags {
		if f.IsActive {
			active = append(active, f)
		}
	}
	return active, nil
}

// FetchFlagValues retrieves flag values, optionally filtered by flagID and/or environmentID.
func (c *Client) FetchFlagValues(flagID, envID string) ([]FlagValue, error) {
	path := "/flag-values"
	sep := "?"
	if flagID != "" {
		path += sep + "flagId=" + flagID
		sep = "&"
	}
	if envID != "" {
		path += sep + "environmentId=" + envID
	}

	var fvs []FlagValue
	if err := c.getJSON(path, &fvs); err != nil {
		return nil, fmt.Errorf("failed to fetch flag values: %w", err)
	}
	return fvs, nil
}

// Discover fetches all environments, flags, and flag values from the Admin API.
func (c *Client) Discover() (*DiscoveryResult, error) {
	if err := c.Ping(); err != nil {
		return nil, err
	}

	envs, err := c.FetchEnvironments()
	if err != nil {
		return nil, err
	}

	flags, err := c.FetchFlags()
	if err != nil {
		return nil, err
	}

	flagValues, err := c.FetchFlagValues("", "")
	if err != nil {
		return nil, err
	}

	return &DiscoveryResult{
		Environments: envs,
		Flags:        flags,
		FlagValues:   flagValues,
	}, nil
}

func (c *Client) setAuth(req *http.Request) {
	req.Header.Set("Authorization", "Bearer "+c.token)
	req.Header.Set("Accept", "application/json")
}

func (c *Client) getJSON(path string, target interface{}) error {
	req, err := http.NewRequest(http.MethodGet, c.baseURL+path, nil)
	if err != nil {
		return err
	}
	c.setAuth(req)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("request failed: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode == http.StatusUnauthorized || resp.StatusCode == http.StatusForbidden {
		return fmt.Errorf("authentication failed (HTTP %d)", resp.StatusCode)
	}
	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("HTTP %d: %s", resp.StatusCode, string(body))
	}

	return json.NewDecoder(resp.Body).Decode(target)
}
