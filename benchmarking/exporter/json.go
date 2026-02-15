package exporter

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"time"

	"github.com/manan/feature-flag/benchmarking/metrics"
)

// Export writes the test result as a pretty-printed JSON file named after the
// test mode (e.g. results/constant.json, results/rampup.json). If a result for
// the same mode already exists, it is rotated to an archive/ subdirectory.
// outputDir is the directory to write into, and mode is the test mode string.
func Export(result *metrics.TestResult, outputDir, mode string) error {
	if err := os.MkdirAll(outputDir, 0o755); err != nil {
		return fmt.Errorf("create output dir: %w", err)
	}

	outputPath := filepath.Join(outputDir, mode+".json")

	// Rotate existing file for this mode
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
