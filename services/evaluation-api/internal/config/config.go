package config

import (
	"fmt"
	"os"
	"strconv"
	"time"
)

type Config struct {
	Server      ServerConfig
	Database    DatabaseConfig
	Redis       RedisConfig
	MemoryCache MemoryCacheConfig
}

type ServerConfig struct {
	Port            int
	ReadTimeout     time.Duration
	WriteTimeout    time.Duration
	ShutdownTimeout time.Duration
}

type DatabaseConfig struct {
	Host            string
	Port            int
	User            string
	Password        string
	Database        string
	SSLMode         string
	MaxConns        int
	MinConns        int
	MaxConnLifetime time.Duration
	MaxConnIdleTime time.Duration
}

type RedisConfig struct {
	URL          string // Full Redis URL (redis://user:password@host:port/db)
	Host         string // Fallback: individual host
	Port         int    // Fallback: individual port
	Password     string // Fallback: individual password
	DB           int    // Fallback: individual DB
	TTL          time.Duration
	PoolSize     int
	DialTimeout  time.Duration
	ReadTimeout  time.Duration
	WriteTimeout time.Duration
}

type MemoryCacheConfig struct {
	MaxSize     int64         // Maximum size in bytes (default 100MB)
	TTL         time.Duration // TTL for memory cache entries (shorter than Redis)
	NumCounters int64         // Number of keys to track for frequency (10x expected items)
}

func Load() (*Config, error) {
	cfg := &Config{
		Server: ServerConfig{
			Port:            getEnvInt("PORT", 8081),
			ReadTimeout:     getEnvDuration("SERVER_READ_TIMEOUT", 5*time.Second),
			WriteTimeout:    getEnvDuration("SERVER_WRITE_TIMEOUT", 10*time.Second),
			ShutdownTimeout: getEnvDuration("SERVER_SHUTDOWN_TIMEOUT", 30*time.Second),
		},
		Database: DatabaseConfig{
			Host:            getEnvString("DB_HOST", "localhost"),
			Port:            getEnvInt("DB_PORT", 5432),
			User:            getEnvString("DB_USER", "postgres"),
			Password:        getEnvString("DB_PASSWORD", "postgres"),
			Database:        getEnvString("DB_NAME", "featureflags"),
			SSLMode:         getEnvString("DB_SSL_MODE", "disable"),
			MaxConns:        getEnvInt("DB_MAX_CONNS", 25),
			MinConns:        getEnvInt("DB_MIN_CONNS", 5),
			MaxConnLifetime: getEnvDuration("DB_MAX_CONN_LIFETIME", 1*time.Hour),
			MaxConnIdleTime: getEnvDuration("DB_MAX_CONN_IDLE_TIME", 30*time.Minute),
		},
		Redis: RedisConfig{
			URL:          getEnvString("REDIS_URL", ""),
			Host:         getEnvString("REDIS_HOST", "localhost"),
			Port:         getEnvInt("REDIS_PORT", 6379),
			Password:     getEnvString("REDIS_PASSWORD", ""),
			DB:           getEnvInt("REDIS_DB", 0),
			TTL:          getEnvDuration("REDIS_TTL", 5*time.Minute),
			PoolSize:     getEnvInt("REDIS_POOL_SIZE", 10),
			DialTimeout:  getEnvDuration("REDIS_DIAL_TIMEOUT", 10*time.Second),
			ReadTimeout:  getEnvDuration("REDIS_READ_TIMEOUT", 10*time.Second),
			WriteTimeout: getEnvDuration("REDIS_WRITE_TIMEOUT", 10*time.Second),
		},
		MemoryCache: MemoryCacheConfig{
			MaxSize:     getEnvInt64("MEMORY_CACHE_MAX_SIZE", 100*1024*1024), // 100MB
			TTL:         getEnvDuration("MEMORY_CACHE_TTL", 30*time.Second),
			NumCounters: getEnvInt64("MEMORY_CACHE_NUM_COUNTERS", 100000),
		},
	}

	return cfg, nil
}

func (c *DatabaseConfig) ConnectionString() string {
	return fmt.Sprintf(
		"postgres://%s:%s@%s:%d/%s?sslmode=%s",
		c.User,
		c.Password,
		c.Host,
		c.Port,
		c.Database,
		c.SSLMode,
	)
}

func getEnvString(key, defaultValue string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return defaultValue
}

func getEnvInt(key string, defaultValue int) int {
	if value := os.Getenv(key); value != "" {
		if intVal, err := strconv.Atoi(value); err == nil {
			return intVal
		}
	}
	return defaultValue
}

func getEnvInt64(key string, defaultValue int64) int64 {
	if value := os.Getenv(key); value != "" {
		if intVal, err := strconv.ParseInt(value, 10, 64); err == nil {
			return intVal
		}
	}
	return defaultValue
}

func getEnvDuration(key string, defaultValue time.Duration) time.Duration {
	if value := os.Getenv(key); value != "" {
		if duration, err := time.ParseDuration(value); err == nil {
			return duration
		}
	}
	return defaultValue
}
