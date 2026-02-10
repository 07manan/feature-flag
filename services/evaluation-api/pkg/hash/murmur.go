package hash

import (
	"encoding/binary"
)

const (
	c1 uint32 = 0xcc9e2d51
	c2 uint32 = 0x1b873593
)

// MurmurHash3 computes a 32-bit MurmurHash3 hash of the input string
// This is used for deterministic percentage-based rollout bucketing
func MurmurHash3(key string, seed uint32) uint32 {
	data := []byte(key)
	length := len(data)
	nblocks := length / 4

	h1 := seed

	for i := 0; i < nblocks; i++ {
		k1 := binary.LittleEndian.Uint32(data[i*4:])

		k1 *= c1
		k1 = rotl32(k1, 15)
		k1 *= c2

		h1 ^= k1
		h1 = rotl32(h1, 13)
		h1 = h1*5 + 0xe6546b64
	}

	tail := data[nblocks*4:]
	var k1 uint32

	switch length & 3 {
	case 3:
		k1 ^= uint32(tail[2]) << 16
		fallthrough
	case 2:
		k1 ^= uint32(tail[1]) << 8
		fallthrough
	case 1:
		k1 ^= uint32(tail[0])
		k1 *= c1
		k1 = rotl32(k1, 15)
		k1 *= c2
		h1 ^= k1
	}

	h1 ^= uint32(length)
	h1 = fmix32(h1)

	return h1
}

// ComputeBucket calculates a bucket (0-99) for percentage-based rollout
// Using flagKey + userID ensures consistent bucketing per user per flag
func ComputeBucket(flagKey, userID string) int {
	combined := flagKey + ":" + userID
	hash := MurmurHash3(combined, 0)
	return int(hash % 100)
}

func rotl32(x uint32, r uint8) uint32 {
	return (x << r) | (x >> (32 - r))
}

func fmix32(h uint32) uint32 {
	h ^= h >> 16
	h *= 0x85ebca6b
	h ^= h >> 13
	h *= 0xc2b2ae35
	h ^= h >> 16
	return h
}
