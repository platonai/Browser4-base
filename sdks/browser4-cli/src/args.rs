//! Argument parsing helpers for the Browser4 CLI.
//!
//! Parses raw command-line arguments into:
//! - Global flags (`-s=<session>`, `--server=<url>`)
//! - Positional arguments (stored in `_`)
//! - Named options (`--key=value`, `--flag`)

use serde_json::{json, Value};
use std::collections::HashMap;

/// Parsed global flags that appear before the command name.
#[derive(Debug, Default)]
pub struct GlobalFlags {
    /// `-s=<name>` session name
    pub session_name: Option<String>,
    /// `--server=<url>` or `--server <url>` server override
    pub server_url: Option<String>,
    /// Remaining arguments (command + its args/options)
    pub args: Vec<String>,
}

/// Parse global flags that may appear before the command.
///
/// Recognises:
/// - `-s=<name>` → session name
/// - `--server=<url>` or `--server <url>` → server URL override
/// - `--version` / `-v` → version flag (returned in `args`)
/// - Everything else is forwarded unchanged in `args`
pub fn parse_global_flags(argv: &[String]) -> GlobalFlags {
    let mut flags = GlobalFlags::default();

    // Default session name from environment variable
    if let Ok(env_session) = std::env::var("BROWSER4_CLI_SESSION") {
        if !env_session.is_empty() {
            flags.session_name = Some(env_session);
        }
    }

    let mut i = 0;
    while i < argv.len() {
        let arg = &argv[i];
        if arg.starts_with("-s=") {
            flags.session_name = Some(arg["-s=".len()..].to_string());
        } else if arg.starts_with("--server=") {
            flags.server_url = Some(arg["--server=".len()..].to_string());
        } else if arg == "--server" {
            if i + 1 < argv.len() && !argv[i + 1].starts_with('-') {
                i += 1;
                flags.server_url = Some(argv[i].clone());
            }
        } else {
            flags.args.push(arg.clone());
        }
        i += 1;
    }
    flags
}

/// Parse raw CLI arguments into a map suitable for command dispatch.
///
/// - Positional arguments go into `_` as a JSON array.
/// - `--key=value` → key: string value
/// - `--flag` (no value) → key: true (boolean)
/// - Values `"true"` / `"false"` are coerced to booleans.
pub fn parse_raw_args(raw_args: &[String]) -> HashMap<String, Value> {
    let mut result: HashMap<String, Value> = HashMap::new();
    let mut positional: Vec<Value> = Vec::new();

    for arg in raw_args {
        if let Some(rest) = arg.strip_prefix("--") {
            if let Some(eq) = rest.find('=') {
                let key = rest[..eq].to_string();
                let val = &rest[eq + 1..];
                let value = match val {
                    "true" => Value::Bool(true),
                    "false" => Value::Bool(false),
                    other => Value::String(other.to_string()),
                };
                result.insert(key, value);
            } else {
                result.insert(rest.to_string(), Value::Bool(true));
            }
        } else {
            positional.push(json!(arg));
        }
    }
    result.insert("_".to_string(), Value::Array(positional));
    result
}

/// Build a flat argument map from parsed raw args for use in command dispatch.
///
/// Positional arguments are mapped to their named positions as defined in
/// `arg_names` (starting from index 1 since index 0 is the command name).
/// Returns an error string if too many positional arguments are supplied.
pub fn build_command_args(
    raw: &HashMap<String, Value>,
    arg_names: &[&str],
) -> Result<HashMap<String, Value>, String> {
    let mut result = raw.clone();

    let positional: Vec<String> = match raw.get("_") {
        Some(Value::Array(arr)) => arr
            .iter()
            .skip(1) // skip command name
            .map(|v| v.as_str().unwrap_or("").to_string())
            .collect(),
        _ => vec![],
    };

    if positional.len() > arg_names.len() {
        return Err(format!(
            "error: too many arguments: expected {}, received {}",
            arg_names.len(),
            positional.len()
        ));
    }

    for (i, name) in arg_names.iter().enumerate() {
        if i < positional.len() {
            // Try to parse as number
            if let Ok(n) = positional[i].parse::<f64>() {
                result.insert(name.to_string(), json!(n));
            } else {
                result.insert(name.to_string(), json!(positional[i]));
            }
        }
    }

    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_global_flags_session_name() {
        let argv = vec!["-s=mysession".to_string(), "goto".to_string(), "https://example.com".to_string()];
        let flags = parse_global_flags(&argv);
        assert_eq!(flags.session_name.as_deref(), Some("mysession"));
        assert_eq!(flags.args, vec!["goto", "https://example.com"]);
    }

    #[test]
    fn test_parse_global_flags_server_equals() {
        let argv = vec!["--server=http://localhost:9090".to_string(), "open".to_string()];
        let flags = parse_global_flags(&argv);
        assert_eq!(flags.server_url.as_deref(), Some("http://localhost:9090"));
        assert_eq!(flags.args, vec!["open"]);
    }

    #[test]
    fn test_parse_global_flags_server_space() {
        let argv = vec![
            "--server".to_string(),
            "http://localhost:9090".to_string(),
            "open".to_string(),
        ];
        let flags = parse_global_flags(&argv);
        assert_eq!(flags.server_url.as_deref(), Some("http://localhost:9090"));
        assert_eq!(flags.args, vec!["open"]);
    }

    #[test]
    fn test_parse_raw_args_positional() {
        let raw = vec!["goto".to_string(), "https://example.com".to_string()];
        let map = parse_raw_args(&raw);
        let pos = map["_"].as_array().unwrap();
        assert_eq!(pos[0].as_str(), Some("goto"));
        assert_eq!(pos[1].as_str(), Some("https://example.com"));
    }

    #[test]
    fn test_parse_raw_args_options() {
        let raw = vec!["click".to_string(), "e15".to_string(), "--submit=true".to_string()];
        let map = parse_raw_args(&raw);
        assert_eq!(map.get("submit"), Some(&json!(true)));
    }

    #[test]
    fn test_parse_raw_args_bool_flag() {
        let raw = vec!["snapshot".to_string(), "--headed".to_string()];
        let map = parse_raw_args(&raw);
        assert_eq!(map.get("headed"), Some(&json!(true)));
    }

    #[test]
    fn test_build_command_args_too_many() {
        let mut raw = HashMap::new();
        raw.insert("_".to_string(), json!(["cmd", "a", "b", "c"]));
        let result = build_command_args(&raw, &["x"]);
        assert!(result.is_err());
        assert!(result.unwrap_err().contains("too many arguments"));
    }

    #[test]
    fn test_build_command_args_numeric_coercion() {
        let mut raw = HashMap::new();
        raw.insert("_".to_string(), json!(["mousemove", "100", "200"]));
        let result = build_command_args(&raw, &["x", "y"]).unwrap();
        assert_eq!(result.get("x"), Some(&json!(100.0)));
        assert_eq!(result.get("y"), Some(&json!(200.0)));
    }
}
