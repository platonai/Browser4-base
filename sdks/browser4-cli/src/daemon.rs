//! Server daemon management for the Browser4 CLI.
//!
//! Ensures a Browser4 server is running before executing commands.
//! Only manages localhost instances; remote servers are not touched.
//! When a local Browser4 checkout is available, startup prefers
//! `mvn spring-boot:run` from the repo root so the CLI uses the matching
//! server version. If no checkout can be found, it falls back to the packaged
//! Browser4 jar flow used by the standalone installer.

use std::env;
use std::fs;
use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::time::{Duration, Instant};

use reqwest::Client;

use crate::managed_processes::{register_managed_server_process, ManagedServerProcess};
use crate::state::read_state;

/// Ensure the Browser4 server is running, starting it if necessary.
///
/// Only acts on `localhost` / `127.0.0.1` URLs.
pub async fn ensure_server_running(base_url: &str) -> Result<(), String> {
    // Skip remote servers
    if !base_url.contains("localhost") && !base_url.contains("127.0.0.1") {
        return Ok(());
    }

    let client = Client::builder()
        .timeout(std::time::Duration::from_secs(5))
        .build()
        .map_err(|e| e.to_string())?;

    match probe_server_state(&client, base_url).await {
        ServerState::Ready => return Ok(()),
        ServerState::Starting(_) => {
            return wait_for_server_ready(&client, base_url, Duration::from_secs(60)).await;
        }
        ServerState::Unreachable(_) => {}
    }

    eprintln!("Browser4 server not running. Starting...");

    let port = extract_port(base_url);
    let launch_spec = resolve_server_launch_spec(port).await?;
    eprintln!("{}", launch_spec.description);

    start_server(&launch_spec, base_url, port).await
}

fn extract_port(base_url: &str) -> u16 {
    if let Ok(url) = reqwest::Url::parse(base_url) {
        url.port().unwrap_or(8182)
    } else {
        8182
    }
}

#[derive(Debug, Clone, PartialEq, Eq)]
struct ServerLaunchSpec {
    program: PathBuf,
    args: Vec<String>,
    working_dir: PathBuf,
    registry_target: PathBuf,
    description: String,
}

async fn resolve_server_launch_spec(port: u16) -> Result<ServerLaunchSpec, String> {
    if let Some(repo_root) = find_browser4_root() {
        return build_maven_launch_spec(&repo_root, port);
    }

    let jar_path = find_or_download_jar().await?;
    Ok(build_jar_launch_spec(&jar_path, port))
}

fn build_maven_launch_spec(repo_root: &Path, port: u16) -> Result<ServerLaunchSpec, String> {
    if !repo_root.is_dir() {
        return Err(format!(
            "Browser4 root does not exist: {}",
            repo_root.display()
        ));
    }

    let wrapper_name = if cfg!(windows) { "mvnw.cmd" } else { "mvnw" };
    let wrapper_path = repo_root.join(wrapper_name);
    let program = if wrapper_path.exists() {
        wrapper_path
    } else if cfg!(windows) {
        PathBuf::from("mvn.cmd")
    } else {
        PathBuf::from("mvn")
    };

    Ok(ServerLaunchSpec {
        program: program.clone(),
        args: vec![
            "-pl".to_string(),
            "pulsar-rest".to_string(),
            "-am".to_string(),
            "spring-boot:run".to_string(),
            format!("-Dspring-boot.run.arguments=--server.port={port}"),
        ],
        working_dir: repo_root.to_path_buf(),
        registry_target: program,
        description: format!(
            "Starting server via Maven spring-boot:run from {} on port {}...",
            repo_root.display(),
            port
        ),
    })
}

fn build_jar_launch_spec(jar_path: &Path, port: u16) -> ServerLaunchSpec {
    ServerLaunchSpec {
        program: PathBuf::from("java"),
        args: vec![
            "-jar".to_string(),
            jar_path.to_string_lossy().to_string(),
            format!("--server.port={port}"),
        ],
        working_dir: jar_path
            .parent()
            .map(Path::to_path_buf)
            .unwrap_or_else(|| PathBuf::from(".")),
        registry_target: jar_path.to_path_buf(),
        description: format!(
            "Starting server from Browser4.jar at {} on port {}...",
            jar_path.display(),
            port
        ),
    }
}

fn command_for_launch_spec(launch_spec: &ServerLaunchSpec) -> Command {
    let mut command = Command::new(&launch_spec.program);
    command
        .args(&launch_spec.args)
        .current_dir(&launch_spec.working_dir)
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null());
    command
}

fn find_browser4_root() -> Option<PathBuf> {
    for candidate in browser4_root_candidates() {
        if let Some(root) = find_browser4_root_from(&candidate) {
            return Some(root);
        }
    }
    None
}

fn browser4_root_candidates() -> Vec<PathBuf> {
    let mut candidates = Vec::new();

    if let Ok(current_dir) = env::current_dir() {
        candidates.push(current_dir);
    }

    if let Ok(current_exe) = env::current_exe() {
        if let Some(parent) = current_exe.parent() {
            candidates.push(parent.to_path_buf());
        }
    }

    candidates.push(PathBuf::from(env!("CARGO_MANIFEST_DIR")));
    candidates
}

fn find_browser4_root_from(start: &Path) -> Option<PathBuf> {
    let mut current = Some(start);
    while let Some(path) = current {
        if is_browser4_root(path) {
            return Some(path.to_path_buf());
        }
        current = path.parent();
    }
    None
}

fn is_browser4_root(path: &Path) -> bool {
    path.join("pom.xml").is_file()
        && path.join("pulsar-rest").is_dir()
        && path
            .join("sdks")
            .join("browser4-cli")
            .join("Cargo.toml")
            .is_file()
}

async fn find_or_download_jar() -> Result<PathBuf, String> {
    // Check environment variable
    if let Ok(env_path) = std::env::var("BROWSER4_JAR_PATH") {
        let p = PathBuf::from(&env_path);
        if p.exists() {
            return Ok(p);
        }
    }

    // Common candidate locations
    let home = dirs::home_dir().unwrap_or_else(|| PathBuf::from("."));
    let candidates = vec![
        PathBuf::from("../../browser4/browser4-agents/target/Browser4.jar"),
        PathBuf::from("browser4/browser4-agents/target/Browser4.jar"),
        PathBuf::from("target/Browser4.jar"),
        home.join(".browser4").join("lib").join("Browser4.jar"),
    ];

    for candidate in candidates {
        if candidate.exists() {
            return Ok(candidate);
        }
    }

    // Download if not found
    let download_path = home.join(".browser4").join("lib").join("Browser4.jar");
    download_jar(&download_path).await?;
    Ok(download_path)
}

async fn download_jar(target_path: &Path) -> Result<(), String> {
    if let Some(dir) = target_path.parent() {
        fs::create_dir_all(dir).map_err(|e| e.to_string())?;
    }

    let url = "https://github.com/platonai/Browser4/releases/latest/download/Browser4.jar";
    eprintln!("Downloading Browser4.jar from {}...", url);

    let client = Client::builder()
        .timeout(std::time::Duration::from_secs(300))
        .build()
        .map_err(|e| e.to_string())?;

    let response = client
        .get(url)
        .send()
        .await
        .map_err(|e| format!("Download failed: {e}"))?;

    if !response.status().is_success() {
        return Err(format!(
            "Download failed with status: {}",
            response.status()
        ));
    }

    let bytes = response.bytes().await.map_err(|e| e.to_string())?;
    fs::write(target_path, &bytes).map_err(|e| e.to_string())?;

    eprintln!("Download complete.");
    Ok(())
}

async fn start_server(
    launch_spec: &ServerLaunchSpec,
    base_url: &str,
    port: u16,
) -> Result<(), String> {
    let mut child = command_for_launch_spec(launch_spec)
        .spawn()
        .map_err(|e| format!("Failed to start server: {e}"))?;

    let client = Client::builder()
        .timeout(std::time::Duration::from_secs(5))
        .build()
        .map_err(|e| e.to_string())?;

    if let Err(error) = wait_for_server_ready(&client, base_url, Duration::from_secs(60)).await {
        let exit_context = match child.try_wait() {
            Ok(Some(status)) => format!(" Process exited early with status {status}."),
            Ok(None) => String::new(),
            Err(wait_error) => format!(" Failed to inspect launcher process: {wait_error}."),
        };
        return Err(format!("{error}{exit_context}"));
    }

    let managed_pid = resolve_managed_server_pid(child.id());
    register_managed_server_process(
        ManagedServerProcess {
            pid: managed_pid,
            base_url: base_url.to_string(),
            port,
            // Keep the legacy registry field populated for backward compatibility.
            jar_path: launch_spec.registry_target.to_string_lossy().to_string(),
            started_at: chrono::Utc::now().to_rfc3339(),
        },
        None,
    );

    // Detach: we drop the Child handle here. The spawned process continues
    // running independently because we set all stdio to null and call drop().
    drop(child);

    eprintln!("Server is up and running.");
    Ok(())
}

fn resolve_managed_server_pid(launcher_pid: u32) -> u32 {
    #[cfg(windows)]
    {
        resolve_windows_managed_server_pid(launcher_pid).unwrap_or(launcher_pid)
    }

    #[cfg(not(windows))]
    {
        launcher_pid
    }
}

#[cfg(windows)]
fn resolve_windows_managed_server_pid(launcher_pid: u32) -> Option<u32> {
    let ps_command = format!(
        r#"
function Get-DescendantProcessIds([UInt32] $ProcessId) {{
    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" | Sort-Object CreationDate
    foreach ($child in $children) {{
        $child.ProcessId
        Get-DescendantProcessIds -ProcessId $child.ProcessId
    }}
}}
$ids = @(Get-DescendantProcessIds -ProcessId {launcher_pid})
if ($ids.Count -gt 0) {{ $ids[-1] }}
"#
    );

    let output = Command::new("powershell")
        .args(["-NoProfile", "-NonInteractive", "-Command", &ps_command])
        .output()
        .ok()?;
    if !output.status.success() {
        return None;
    }

    String::from_utf8_lossy(&output.stdout)
        .lines()
        .rev()
        .find_map(|line| line.trim().parse::<u32>().ok())
}

/// Resolve the base URL from CLI state + optional server override arg.
pub fn resolve_base_url(override_url: Option<&str>, session_name: Option<&str>) -> String {
    let state = read_state(None, session_name);
    let base = override_url
        .map(|s| s.to_string())
        .unwrap_or(state.base_url);
    base.trim_end_matches('/').to_string()
}

enum ServerState {
    Ready,
    Starting(String),
    Unreachable(String),
}

async fn probe_server_state(client: &Client, base_url: &str) -> ServerState {
    let trimmed = base_url.trim_end_matches('/');
    let health_url = format!("{trimmed}/actuator/health");
    let tools_url = format!("{trimmed}/mcp/tools");

    let health_response = match client.get(&health_url).send().await {
        Ok(response) => response,
        Err(error) => return ServerState::Unreachable(error.to_string()),
    };
    let health_body = match health_response.text().await {
        Ok(body) => body,
        Err(error) => return ServerState::Starting(error.to_string()),
    };
    if !health_body.contains("\"status\":\"UP\"") {
        return ServerState::Starting(health_body);
    }

    let tools_response = match client.get(&tools_url).send().await {
        Ok(response) => response,
        Err(error) => return ServerState::Starting(error.to_string()),
    };
    let tools_body = match tools_response.text().await {
        Ok(body) => body,
        Err(error) => return ServerState::Starting(error.to_string()),
    };
    if tools_body.contains("open_session") && tools_body.contains("browser_navigate") {
        ServerState::Ready
    } else {
        ServerState::Starting(format!("MCP tools endpoint not ready: {tools_body}"))
    }
}

async fn wait_for_server_ready(
    client: &Client,
    base_url: &str,
    timeout: Duration,
) -> Result<(), String> {
    let start = Instant::now();
    let mut last_error = String::from("unknown");

    while start.elapsed() <= timeout {
        match probe_server_state(client, base_url).await {
            ServerState::Ready => return Ok(()),
            ServerState::Starting(error) | ServerState::Unreachable(error) => {
                last_error = error;
            }
        }
        tokio::time::sleep(Duration::from_secs(1)).await;
    }

    Err(format!(
        "Server failed to become MCP-ready within {}s: {}",
        timeout.as_secs(),
        last_error
    ))
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs::{create_dir_all, write};
    use tempfile::TempDir;

    fn create_browser4_root(tmp: &TempDir) -> PathBuf {
        let root = tmp.path().join("Browser4");
        create_dir_all(root.join("pulsar-rest")).unwrap();
        create_dir_all(root.join("sdks").join("browser4-cli")).unwrap();
        write(root.join("pom.xml"), "<project />").unwrap();
        write(
            root.join("sdks").join("browser4-cli").join("Cargo.toml"),
            "[package]\nname = \"browser4-cli\"\n",
        )
        .unwrap();
        root
    }

    #[test]
    fn test_find_browser4_root_from_nested_cli_dir() {
        let tmp = TempDir::new().unwrap();
        let root = create_browser4_root(&tmp);
        let nested = root.join("sdks").join("browser4-cli").join("src");
        create_dir_all(&nested).unwrap();

        assert_eq!(find_browser4_root_from(&nested), Some(root));
    }

    #[test]
    fn test_find_browser4_root_from_non_repo_path() {
        let tmp = TempDir::new().unwrap();
        let outside = tmp.path().join("not-browser4");
        create_dir_all(&outside).unwrap();

        assert_eq!(find_browser4_root_from(&outside), None);
    }

    #[cfg(windows)]
    #[test]
    fn test_build_maven_launch_spec_prefers_windows_wrapper() {
        let tmp = TempDir::new().unwrap();
        let root = create_browser4_root(&tmp);
        let wrapper = root.join("mvnw.cmd");
        write(&wrapper, "@echo off\r\n").unwrap();

        let spec = build_maven_launch_spec(&root, 8199).unwrap();

        assert_eq!(spec.program, wrapper);
        assert_eq!(spec.working_dir, root);
        assert_eq!(
            spec.args,
            vec![
                "-pl",
                "pulsar-rest",
                "-am",
                "spring-boot:run",
                "-Dspring-boot.run.arguments=--server.port=8199",
            ]
        );
    }

    #[cfg(not(windows))]
    #[test]
    fn test_build_maven_launch_spec_prefers_unix_wrapper() {
        let tmp = TempDir::new().unwrap();
        let root = create_browser4_root(&tmp);
        let wrapper = root.join("mvnw");
        write(&wrapper, "#!/usr/bin/env sh\n").unwrap();

        let spec = build_maven_launch_spec(&root, 8199).unwrap();

        assert_eq!(spec.program, wrapper);
        assert_eq!(spec.working_dir, root);
        assert_eq!(
            spec.args,
            vec![
                "-pl",
                "pulsar-rest",
                "-am",
                "spring-boot:run",
                "-Dspring-boot.run.arguments=--server.port=8199",
            ]
        );
    }
}
