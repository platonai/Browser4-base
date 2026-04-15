//! Argument parsing helpers for the Browser4 CLI.
//!
//! Parses raw command-line arguments into:
//! - Global flags (`-s=<session>`, `--server=<url>`)
//! - Positional arguments (stored in `_`)
//! - Named options (`--key=value`, `--flag`)

use serde_json::{json, Value};
use std::collections::HashMap;

/// Parsed global flags that appear before the command name.
#[derive(Debug, Default, Clone)]
pub struct GlobalFlags {
    /// `-s=<name>` session name
    pub session_name: Option<String>,
    /// `--server=<url>` or `--server <url>` server override
    pub server_url: Option<String>,
    /// Remaining arguments (command + its args/options)
    pub args: Vec<String>,
}

#[derive(Debug, Default, Clone, PartialEq, Eq)]
pub struct BatchArgs {
    pub bail: bool,
    pub json: bool,
    pub commands: Vec<String>,
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
            if let Ok(n) = positional[i].parse::<i64>() {
                result.insert(name.to_string(), json!(n));
            } else if let Ok(n) = positional[i].parse::<f64>() {
                result.insert(name.to_string(), json!(n));
            } else {
                result.insert(name.to_string(), json!(positional[i]));
            }
        }
    }

    Ok(result)
}

/// Parse `browser4-cli batch` flags and positional command strings.
pub fn parse_batch_args(raw_args: &[String]) -> Result<BatchArgs, String> {
    let mut parsed = BatchArgs::default();
    let mut parsing_options = true;

    for arg in raw_args {
        if parsing_options {
            match arg.as_str() {
                "--" => {
                    parsing_options = false;
                    continue;
                }
                "--bail" => {
                    parsed.bail = true;
                    continue;
                }
                "--json" => {
                    parsed.json = true;
                    continue;
                }
                _ => parsing_options = false,
            }
        }
        parsed.commands.push(arg.clone());
    }

    if parsed.json && !parsed.commands.is_empty() {
        return Err("Batch --json mode does not accept positional command arguments.".to_string());
    }

    if !parsed.json && parsed.commands.is_empty() {
        return Err(
            "Batch requires at least one command argument or JSON input via --json.".to_string(),
        );
    }

    Ok(parsed)
}

/// Split a single batch command string into CLI tokens, honoring simple shell-style
/// single quotes, double quotes, and backslash escaping.
pub fn parse_command_string(command: &str) -> Result<Vec<String>, String> {
    let mut tokens = Vec::new();
    let mut current = String::new();
    let mut chars = command.chars().peekable();
    let mut in_single = false;
    let mut in_double = false;
    let mut escaped = false;
    let mut token_started = false;

    while let Some(ch) = chars.next() {
        if escaped {
            current.push(ch);
            escaped = false;
            token_started = true;
            continue;
        }

        match ch {
            '\\' if !in_single => escaped = true,
            '\'' if !in_double => {
                in_single = !in_single;
                token_started = true;
            }
            '"' if !in_single => {
                in_double = !in_double;
                token_started = true;
            }
            c if c.is_whitespace() && !in_single && !in_double => {
                if token_started {
                    tokens.push(std::mem::take(&mut current));
                    token_started = false;
                }
                while let Some(next) = chars.peek() {
                    if next.is_whitespace() {
                        chars.next();
                    } else {
                        break;
                    }
                }
            }
            _ => {
                current.push(ch);
                token_started = true;
            }
        }
    }

    if escaped {
        return Err("Command ends with an unfinished escape sequence.".to_string());
    }
    if in_single || in_double {
        return Err("Command has an unclosed quote.".to_string());
    }
    if token_started {
        tokens.push(current);
    }
    if tokens.is_empty() {
        return Err("Batch command entries cannot be empty.".to_string());
    }

    Ok(tokens)
}

/// Parse JSON stdin for `browser4-cli batch --json`.
///
/// Accepts a JSON array where each entry is either:
/// - a command string, e.g. `"open https://example.com"`
/// - an array of string arguments, e.g. `["open", "https://example.com"]`
pub fn parse_batch_json_commands(input: &str) -> Result<Vec<Vec<String>>, String> {
    let value: Value =
        serde_json::from_str(input).map_err(|e| format!("Invalid batch JSON input: {e}"))?;
    let entries = value
        .as_array()
        .ok_or_else(|| "Batch JSON input must be an array.".to_string())?;

    let mut commands = Vec::with_capacity(entries.len());
    for (index, entry) in entries.iter().enumerate() {
        let tokens = match entry {
            Value::String(command) => parse_command_string(command)
                .map_err(|e| format!("Invalid batch command at index {index}: {e}"))?,
            Value::Array(parts) => {
                let mut tokens = Vec::with_capacity(parts.len());
                for part in parts {
                    let part = part.as_str().ok_or_else(|| {
                        format!(
                            "Batch JSON command at index {index} must contain only string arguments."
                        )
                    })?;
                    tokens.push(part.to_string());
                }
                if tokens.is_empty() {
                    return Err(format!(
                        "Batch JSON command at index {index} must not be empty."
                    ));
                }
                tokens
            }
            _ => {
                return Err(format!(
                    "Batch JSON command at index {index} must be a string or string array."
                ));
            }
        };
        commands.push(tokens);
    }

    if commands.is_empty() {
        return Err("Batch JSON input must contain at least one command.".to_string());
    }

    Ok(commands)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_parse_global_flags_session_name() {
        let argv = vec![
            "-s=mysession".to_string(),
            "goto".to_string(),
            "https://example.com".to_string(),
        ];
        let flags = parse_global_flags(&argv);
        assert_eq!(flags.session_name.as_deref(), Some("mysession"));
        assert_eq!(flags.args, vec!["goto", "https://example.com"]);
    }

    #[test]
    fn test_parse_global_flags_server_equals() {
        let argv = vec![
            "--server=http://localhost:9090".to_string(),
            "open".to_string(),
        ];
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
        let raw = vec![
            "click".to_string(),
            "e15".to_string(),
            "--submit=true".to_string(),
        ];
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
        assert_eq!(result.get("x"), Some(&json!(100)));
        assert_eq!(result.get("y"), Some(&json!(200)));
    }

    #[test]
    fn test_build_command_args_decimal_numeric_coercion() {
        let mut raw = HashMap::new();
        raw.insert("_".to_string(), json!(["mousewheel", "1.5", "-2.25"]));
        let result = build_command_args(&raw, &["dx", "dy"]).unwrap();
        assert_eq!(result.get("dx"), Some(&json!(1.5)));
        assert_eq!(result.get("dy"), Some(&json!(-2.25)));
    }

    #[test]
    fn test_parse_batch_args_argument_mode() {
        let args = vec![
            "--bail".to_string(),
            "open https://example.com".to_string(),
            "snapshot".to_string(),
        ];
        let parsed = parse_batch_args(&args).unwrap();
        assert_eq!(
            parsed,
            BatchArgs {
                bail: true,
                json: false,
                commands: vec![
                    "open https://example.com".to_string(),
                    "snapshot".to_string()
                ],
            }
        );
    }

    #[test]
    fn test_parse_batch_args_rejects_positional_with_json() {
        let args = vec!["--json".to_string(), "snapshot".to_string()];
        let err = parse_batch_args(&args).unwrap_err();
        assert!(err.contains("--json"));
    }

    #[test]
    fn test_parse_batch_args_treats_dash_prefixed_command_as_command() {
        let args = vec![
            "--server=http://example.com open https://example.com".to_string(),
            "snapshot".to_string(),
        ];
        let parsed = parse_batch_args(&args).unwrap();
        assert_eq!(
            parsed.commands,
            vec![
                "--server=http://example.com open https://example.com".to_string(),
                "snapshot".to_string()
            ]
        );
    }

    #[test]
    fn test_parse_command_string_supports_quotes() {
        let parsed = parse_command_string(r##"type "#search-input" "hello world""##).unwrap();
        assert_eq!(parsed, vec!["type", "#search-input", "hello world"]);
    }

    #[test]
    fn test_parse_command_string_supports_single_quotes_and_escapes() {
        let parsed = parse_command_string("type '#search input' it\\ works").unwrap();
        assert_eq!(parsed, vec!["type", "#search input", "it works"]);
    }

    #[test]
    fn test_parse_command_string_rejects_unclosed_quotes() {
        let err = parse_command_string(r##"type "#search"##).unwrap_err();
        assert!(err.contains("unclosed quote"));
    }

    #[test]
    fn test_parse_command_string_preserves_empty_quoted_argument() {
        let parsed = parse_command_string(r#"open "" "#).unwrap();
        assert_eq!(parsed, vec!["open", ""]);
    }

    #[test]
    fn test_parse_batch_json_commands_array_entries() {
        let parsed =
            parse_batch_json_commands(r#"[["open","https://example.com"],["snapshot"]]"#).unwrap();
        assert_eq!(
            parsed,
            vec![
                vec!["open".to_string(), "https://example.com".to_string()],
                vec!["snapshot".to_string()],
            ]
        );
    }

    #[test]
    fn test_parse_batch_json_commands_string_entries() {
        let parsed =
            parse_batch_json_commands(r#"["open https://example.com","snapshot"]"#).unwrap();
        assert_eq!(
            parsed,
            vec![
                vec!["open".to_string(), "https://example.com".to_string()],
                vec!["snapshot".to_string()],
            ]
        );
    }

    #[test]
    fn test_parse_batch_json_commands_rejects_non_strings() {
        let err = parse_batch_json_commands(r#"[["open",1]]"#).unwrap_err();
        assert!(err.contains("string arguments"));
    }
}
