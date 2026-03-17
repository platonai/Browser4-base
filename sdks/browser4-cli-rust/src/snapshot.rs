//! Snapshot and screenshot file helpers for the Browser4 CLI.

use std::fs;
use std::path::{Path, PathBuf};

use chrono::Utc;

/// Default directory for snapshot and screenshot outputs.
pub const SNAPSHOT_DIR: &str = ".browser4-cli/snapshot";

/// Ensure a directory exists, creating it recursively if needed.
pub fn ensure_dir(dir: &Path) -> std::io::Result<()> {
    fs::create_dir_all(dir)
}

/// Generate a timestamped filename (e.g., `page-2026-01-15T10-30-00.yml`).
pub fn timestamped_filename(prefix: &str, ext: &str) -> String {
    let now = Utc::now()
        .format("%Y-%m-%dT%H-%M-%S")
        .to_string();
    format!("{}-{}.{}", prefix, now, ext)
}

/// Resolve the output path for a snapshot or screenshot, creating the directory
/// if necessary. Returns the absolute path as a string.
pub fn resolve_output_path(filename: Option<&str>, prefix: &str, ext: &str) -> PathBuf {
    let name = filename
        .map(|f| f.to_string())
        .unwrap_or_else(|| timestamped_filename(prefix, ext));

    let out = PathBuf::from(SNAPSHOT_DIR).join(&name);
    let canonical = std::env::current_dir()
        .unwrap_or_else(|_| PathBuf::from("."))
        .join(&out);
    canonical
}

/// Save a text snapshot to disk.
pub fn save_snapshot(path: &Path, content: &str) -> std::io::Result<()> {
    ensure_dir(path.parent().unwrap_or(Path::new(".")))?;
    fs::write(path, content)
}

/// Save binary data (e.g., screenshot PNG) to disk.
pub fn save_binary(path: &Path, data: &[u8]) -> std::io::Result<()> {
    ensure_dir(path.parent().unwrap_or(Path::new(".")))?;
    fs::write(path, data)
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;

    #[test]
    fn test_timestamped_filename_format() {
        let name = timestamped_filename("page", "yml");
        assert!(name.starts_with("page-"));
        assert!(name.ends_with(".yml"));
    }

    #[test]
    fn test_save_snapshot() {
        let tmp = TempDir::new().unwrap();
        let path = tmp.path().join("sub").join("snap.yml");
        save_snapshot(&path, "content: here").unwrap();
        let content = fs::read_to_string(&path).unwrap();
        assert_eq!(content, "content: here");
    }
}
