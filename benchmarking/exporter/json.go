package exporter

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/manan/feature-flag/benchmarking/metrics"
)

// Export writes the test result as a pretty-printed JSON file.
// If the output file already exists, it is rotated to an archive/ subdirectory.
func Export(result *metrics.TestResult, outputPath string) error {
	dir := filepath.Dir(outputPath)
	if err := os.MkdirAll(dir, 0o755); err != nil {
		return fmt.Errorf("create output dir: %w", err)
	}

	// Rotate existing file
	if err := rotateExisting(outputPath); err != nil {
		return fmt.Errorf("rotate existing: %w", err)
	}

	data, err := json.MarshalIndent(result, "", "  ")
	if err != nil {
		return fmt.Errorf("marshal result: %w", err)
	}

	if err := os.WriteFile(outputPath, data, 0o644); err != nil {
		return fmt.Errorf("write result: %w", err)
	}

	return nil
}

func rotateExisting(path string) error {
	if _, err := os.Stat(path); os.IsNotExist(err) {
		return nil
	}

	archiveDir := filepath.Join(filepath.Dir(path), "archive")
	if err := os.MkdirAll(archiveDir, 0o755); err != nil {
		return err
	}

	base := filepath.Base(path)
	ext := filepath.Ext(base)
	name := base[:len(base)-len(ext)]

	ts := time.Now().Format("20060102_150405")
	archiveName := fmt.Sprintf("%s_%s%s", name, ts, ext)
	archivePath := filepath.Join(archiveDir, archiveName)

	return os.Rename(path, archivePath)
}
