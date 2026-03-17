//! Persistent state management for the Browser4 CLI.
//!
//! State is stored in `~/.browser4/cli-state.json` and shared across all
//! `browser4-cli` invocations in the same session.

use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

/// Persistent CLI state stored on disk.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CliState {
    /// Active session ID returned by the server on `open`.
    #[serde(rename = "sessionId", skip_serializing_if = "Option::is_none")]
    pub session_id: Option<String>,
    /// Base URL of the Browser4 REST server.
    #[serde(rename = "baseUrl")]
    pub base_url: String,
    /// Reserved selector slot for future CLI workflows.
    #[serde(rename = "activeSelector", skip_serializing_if = "Option::is_none")]
    pub active_selector: Option<String>,
    /// Named session label for the `-s=<name>` flag.
    #[serde(rename = "sessionName", skip_serializing_if = "Option::is_none")]
    pub session_name: Option<String>,
}

impl Default for CliState {
    fn default() -> Self {
        Self {
            session_id: None,
            base_url: "http://localhost:8182".to_string(),
            active_selector: None,
            session_name: None,
        }
    }
}

/// Resolve the default state directory, honouring `BROWSER4_CLI_STATE_DIR`.
pub fn resolve_default_state_dir() -> PathBuf {
    if let Ok(override_dir) = std::env::var("BROWSER4_CLI_STATE_DIR") {
        let trimmed = override_dir.trim().to_string();
        if !trimmed.is_empty() {
            return PathBuf::from(&trimmed)
                .canonicalize()
                .unwrap_or(PathBuf::from(trimmed));
        }
    }
    dirs::home_dir()
        .unwrap_or_else(|| PathBuf::from("."))
        .join(".browser4")
}

fn state_file(state_dir: &Path) -> PathBuf {
    state_dir.join("cli-state.json")
}

/// Read the persisted CLI state from disk, falling back to defaults.
pub fn read_state(state_dir: Option<&Path>) -> CliState {
    let dir = state_dir
        .map(|p| p.to_path_buf())
        .unwrap_or_else(resolve_default_state_dir);
    let path = state_file(&dir);
    match fs::read_to_string(&path) {
        Ok(raw) => serde_json::from_str::<CliState>(&raw).unwrap_or_default(),
        Err(_) => CliState::default(),
    }
}

/// Write the CLI state to disk, creating the directory if necessary.
pub fn write_state(state: &CliState, state_dir: Option<&Path>) -> std::io::Result<()> {
    let dir = state_dir
        .map(|p| p.to_path_buf())
        .unwrap_or_else(resolve_default_state_dir);
    fs::create_dir_all(&dir)?;
    let json = serde_json::to_string_pretty(state).expect("state serialization should not fail");
    fs::write(state_file(&dir), json)
}

/// Clear all persisted CLI state (called on `close`).
pub fn clear_state(state_dir: Option<&Path>) {
    let dir = state_dir
        .map(|p| p.to_path_buf())
        .unwrap_or_else(resolve_default_state_dir);
    let path = state_file(&dir);
    let _ = fs::remove_file(path);
}

/// Convert a CLI element ref into the selector format expected by Browser4.
///
/// Supported forms:
/// - `e15`       → `backend:15`
/// - `backend:15` → `backend:15` (pass-through)
/// - CSS/XPath selectors are passed through unchanged
pub fn resolve_ref(raw_ref: &str) -> String {
    let trimmed = raw_ref.trim();
    // Match e<digits> (case-insensitive)
    let re = regex::Regex::new(r"(?i)^e(\d+)$").unwrap();
    if let Some(caps) = re.captures(trimmed) {
        return format!("backend:{}", &caps[1]);
    }
    trimmed.to_string()
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;

    #[test]
    fn test_resolve_ref_e_notation() {
        assert_eq!(resolve_ref("e15"), "backend:15");
        assert_eq!(resolve_ref("E42"), "backend:42");
        assert_eq!(resolve_ref("  e7  "), "backend:7");
    }

    #[test]
    fn test_resolve_ref_passthrough() {
        assert_eq!(resolve_ref("backend:15"), "backend:15");
        assert_eq!(resolve_ref(".my-class"), ".my-class");
        assert_eq!(resolve_ref("#some-id"), "#some-id");
    }

    #[test]
    fn test_read_write_state() {
        let tmp = TempDir::new().unwrap();
        let state = CliState {
            session_id: Some("abc123".to_string()),
            base_url: "http://localhost:8182".to_string(),
            active_selector: None,
            session_name: None,
        };
        write_state(&state, Some(tmp.path())).unwrap();
        let read = read_state(Some(tmp.path()));
        assert_eq!(read.session_id.as_deref(), Some("abc123"));
        assert_eq!(read.base_url, "http://localhost:8182");
    }

    #[test]
    fn test_read_state_missing_file() {
        let tmp = TempDir::new().unwrap();
        let state = read_state(Some(tmp.path()));
        assert_eq!(state.base_url, "http://localhost:8182");
        assert!(state.session_id.is_none());
    }

    #[test]
    fn test_clear_state() {
        let tmp = TempDir::new().unwrap();
        let state = CliState::default();
        write_state(&state, Some(tmp.path())).unwrap();
        assert!(state_file(tmp.path()).exists());
        clear_state(Some(tmp.path()));
        assert!(!state_file(tmp.path()).exists());
    }
}
