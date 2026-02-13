package client

import (
	"context"
	"crypto/tls"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptrace"
	"time"
)

// RequestResult holds the timing and status information for a single HTTP request.
type RequestResult struct {
	StatusCode    int
	Latency       time.Duration
	TTFB          time.Duration
	DNSLookup     time.Duration
	TCPConnect    time.Duration
	TLSHandshake  time.Duration
	BytesReceived int64
	Error         error
	ErrorType     string
	Timestamp     time.Time
	Environment   string
	IsBulk        bool
}

// TestTarget defines a single evaluation API target to stress.
type TestTarget struct {
	EnvironmentKey string
	APIKey         string
	FlagKey        string
	IsBulk         bool
}

// Client is an HTTP client optimized for stress testing the Evaluation API.
type Client struct {
	httpClient *http.Client
	baseURL    string
}

// New creates a new evaluation API client with tuned transport settings.
func New(baseURL string, timeout time.Duration, maxIdleConns int) *Client {
	transport := &http.Transport{
		DialContext: (&net.Dialer{
			Timeout:   5 * time.Second,
			KeepAlive: 30 * time.Second,
		}).DialContext,
		MaxIdleConns:        maxIdleConns,
		MaxIdleConnsPerHost: maxIdleConns,
		IdleConnTimeout:     90 * time.Second,
		TLSHandshakeTimeout: 5 * time.Second,
		DisableKeepAlives:   false,
		ForceAttemptHTTP2:   true,
	}

	return &Client{
		httpClient: &http.Client{
			Transport: transport,
			Timeout:   timeout,
		},
		baseURL: baseURL,
	}
}

// Do executes a single request to the evaluation API and returns detailed timing metrics.
func (c *Client) Do(ctx context.Context, target TestTarget, userID string) RequestResult {
	result := RequestResult{
		Timestamp:   time.Now(),
		Environment: target.EnvironmentKey,
		IsBulk:      target.IsBulk,
	}

	// Build URL
	var url string
	if target.IsBulk {
		url = fmt.Sprintf("%s/evaluate", c.baseURL)
	} else {
		url = fmt.Sprintf("%s/evaluate/%s", c.baseURL, target.FlagKey)
	}
	if userID != "" {
		url += "?user=" + userID
	}

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, url, nil)
	if err != nil {
		result.Error = err
		result.ErrorType = "request_creation"
		return result
	}
	req.Header.Set("X-API-Key", target.APIKey)
	req.Header.Set("Accept", "application/json")

	// Tracing for connection-level timing
	var dnsStart, connStart, tlsStart time.Time
	var gotFirstByte time.Time
	requestStart := time.Now()

	trace := &httptrace.ClientTrace{
		DNSStart: func(_ httptrace.DNSStartInfo) {
			dnsStart = time.Now()
		},
		DNSDone: func(_ httptrace.DNSDoneInfo) {
			if !dnsStart.IsZero() {
				result.DNSLookup = time.Since(dnsStart)
			}
		},
		ConnectStart: func(_, _ string) {
			connStart = time.Now()
		},
		ConnectDone: func(_, _ string, connErr error) {
			if !connStart.IsZero() && connErr == nil {
				result.TCPConnect = time.Since(connStart)
			}
		},
		TLSHandshakeStart: func() {
			tlsStart = time.Now()
		},
		TLSHandshakeDone: func(_ tls.ConnectionState, _ error) {
			if !tlsStart.IsZero() {
				result.TLSHandshake = time.Since(tlsStart)
			}
		},
		GotFirstResponseByte: func() {
			gotFirstByte = time.Now()
		},
	}

	req = req.WithContext(httptrace.WithClientTrace(req.Context(), trace))

	resp, err := c.httpClient.Do(req)
	result.Latency = time.Since(requestStart)

	if err != nil {
		result.Error = err
		result.ErrorType = classifyError(err)
		return result
	}
	defer resp.Body.Close()

	if !gotFirstByte.IsZero() {
		result.TTFB = gotFirstByte.Sub(requestStart)
	}

	result.StatusCode = resp.StatusCode

	// Read and discard body to measure full transfer + allow connection reuse
	n, _ := io.Copy(io.Discard, resp.Body)
	result.BytesReceived = n + estimateHeaderSize(resp)

	if resp.StatusCode >= 400 {
		result.Error = fmt.Errorf("HTTP %d", resp.StatusCode)
		result.ErrorType = fmt.Sprintf("http_%d", resp.StatusCode)
	}

	return result
}

// CheckReady verifies the evaluation API is ready to accept traffic.
func (c *Client) CheckReady(ctx context.Context) error {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.baseURL+"/ready", nil)
	if err != nil {
		return err
	}

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return fmt.Errorf("evaluation API not reachable at %s: %w", c.baseURL, err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("evaluation API not ready (HTTP %d): %s", resp.StatusCode, string(body))
	}
	return nil
}

func classifyError(err error) string {
	if err == nil {
		return ""
	}
	if netErr, ok := err.(net.Error); ok {
		if netErr.Timeout() {
			return "timeout"
		}
		return "network"
	}
	return "unknown"
}

func estimateHeaderSize(resp *http.Response) int64 {
	var size int64
	for k, vals := range resp.Header {
		for _, v := range vals {
			size += int64(len(k) + len(v) + 4)
		}
	}
	return size
}
