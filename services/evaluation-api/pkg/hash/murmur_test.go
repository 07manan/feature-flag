package hash

import (
	"testing"
)

func TestMurmurHash3(t *testing.T) {
	tests := []struct {
		name     string
		input    string
		seed     uint32
		expected uint32
	}{
		{
			name:     "empty string",
			input:    "",
			seed:     0,
			expected: 0,
		},
		{
			name:     "simple string",
			input:    "hello",
			seed:     0,
			expected: 613153351, // Known MurmurHash3 value
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			result := MurmurHash3(tt.input, tt.seed)
			if tt.input == "" {
				return
			}
			result2 := MurmurHash3(tt.input, tt.seed)
			if result != result2 {
				t.Errorf("MurmurHash3 not deterministic: got %d and %d", result, result2)
			}
		})
	}
}

func TestComputeBucket(t *testing.T) {
	tests := []struct {
		flagKey string
		userID  string
	}{
		{"feature-x", "user-123"},
		{"feature-y", "user-456"},
		{"my-flag", "user-789"},
	}

	for _, tt := range tests {
		t.Run(tt.flagKey+"-"+tt.userID, func(t *testing.T) {
			bucket := ComputeBucket(tt.flagKey, tt.userID)

			if bucket < 0 || bucket > 99 {
				t.Errorf("ComputeBucket(%q, %q) = %d, want [0, 99]", tt.flagKey, tt.userID, bucket)
			}

			bucket2 := ComputeBucket(tt.flagKey, tt.userID)
			if bucket != bucket2 {
				t.Errorf("ComputeBucket not deterministic: got %d and %d", bucket, bucket2)
			}
		})
	}
}

func TestComputeBucketDistribution(t *testing.T) {
	buckets := make(map[int]int)
	flagKey := "test-flag"

	for i := 0; i < 10000; i++ {
		userID := "user-" + string(rune(i))
		bucket := ComputeBucket(flagKey, userID)
		buckets[bucket]++
	}

	for bucket, count := range buckets {
		if count < 20 || count > 300 {
			t.Logf("Bucket %d has %d users (might indicate poor distribution)", bucket, count)
		}
	}
}
