//! HTTP helpers for calling MCP tools on the Browser4 server.

use reqwest::Client;
use serde_json::{json, Value};

use crate::state::resolve_ref;

/// Build a `reqwest::Client` configured for Browser4 MCP calls.
pub fn make_client() -> Client {
    Client::builder()
        .timeout(std::time::Duration::from_secs(30))
        .build()
        .expect("HTTP client construction should not fail")
}

/// Resolve element ref fields inside the tool arguments.
///
/// The following keys are normalised: `selector`, `ref`, `startRef`, `endRef`.
fn normalize_refs(args: &mut Value) {
    let ref_keys = ["selector", "ref", "startRef", "endRef"];
    if let Value::Object(map) = args {
        for key in &ref_keys {
            if let Some(Value::String(val)) = map.get(*key) {
                let resolved = resolve_ref(val);
                map.insert(key.to_string(), json!(resolved));
            }
        }
    }
}

/// Call an MCP tool on the Browser4 server.
///
/// Makes a `POST /mcp/call-tool` request and returns the text of the first
/// content block, or an error message from the server.
pub async fn call_tool(
    client: &Client,
    base_url: &str,
    tool: &str,
    mut args: Value,
) -> Result<String, String> {
    normalize_refs(&mut args);

    let url = format!("{}/mcp/call-tool", base_url.trim_end_matches('/'));
    let body = json!({ "tool": tool, "arguments": args });

    let response = client
        .post(&url)
        .header("Content-Type", "application/json")
        .json(&body)
        .send()
        .await
        .map_err(|e| format!("HTTP request failed: {e}"))?;

    let data: Value = response
        .json()
        .await
        .map_err(|e| format!("Failed to parse response JSON: {e}"))?;

    if data.get("isError").and_then(|v| v.as_bool()).unwrap_or(false) {
        let msg = data
            .get("content")
            .and_then(|c| c.as_array())
            .and_then(|arr| arr.first())
            .and_then(|item| item.get("text"))
            .and_then(|t| t.as_str())
            .unwrap_or("Unknown MCP error");
        return Err(msg.to_string());
    }

    let text = data
        .get("content")
        .and_then(|c| c.as_array())
        .and_then(|arr| arr.first())
        .and_then(|item| item.get("text"))
        .and_then(|t| t.as_str())
        .unwrap_or("")
        .to_string();

    Ok(text)
}

/// Check whether a server error message indicates a stale/expired session.
pub fn is_stale_session_error(message: &str) -> bool {
    let lower = message.to_lowercase();
    lower.contains("cannot find context with specified id")
        || lower.contains("invalid session id")
        || lower.contains("session not found")
        || lower.contains("session does not exist")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_normalize_refs_e_notation() {
        let mut args = json!({ "ref": "e15", "selector": "e42" });
        normalize_refs(&mut args);
        assert_eq!(args["ref"], "backend:15");
        assert_eq!(args["selector"], "backend:42");
    }

    #[test]
    fn test_normalize_refs_passthrough() {
        let mut args = json!({ "ref": ".my-class", "startRef": "backend:7" });
        normalize_refs(&mut args);
        assert_eq!(args["ref"], ".my-class");
        assert_eq!(args["startRef"], "backend:7");
    }

    #[test]
    fn test_is_stale_session_error() {
        assert!(is_stale_session_error("Cannot find context with specified id"));
        assert!(is_stale_session_error("Invalid session ID"));
        assert!(is_stale_session_error("Session not found"));
        assert!(!is_stale_session_error("Connection refused"));
    }
}
