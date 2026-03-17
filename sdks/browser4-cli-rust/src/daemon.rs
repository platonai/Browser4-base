//! Server daemon management for the Browser4 CLI.
//!
//! Ensures a Browser4 server is running before executing commands.
//! Only manages localhost instances; remote servers are not touched.

use std::fs;
use std::path::PathBuf;
use std::process::{Command, Stdio};

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

    // Check if already running
    let client = Client::builder()
        .timeout(std::time::Duration::from_secs(5))
        .build()
        .map_err(|e| e.to_string())?;

    let health_url = format!("{}/actuator/health", base_url.trim_end_matches('/'));
    if client.get(&health_url).send().await.is_ok() {
        return Ok(());
    }

    eprintln!("Browser4 server not running. Starting...");

    let jar_path = find_or_download_jar().await?;
    eprintln!("Jar path: {}", jar_path.display());

    let port = extract_port(base_url);
    start_server(&jar_path, base_url, port).await
}

fn extract_port(base_url: &str) -> u16 {
    if let Ok(url) = reqwest::Url::parse(base_url) {
        url.port().unwrap_or(8182)
    } else {
        8182
    }
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
        PathBuf::from("Browser4.jar"),
        PathBuf::from("target/Browser4.jar"),
        home.join(".browser4").join("Browser4.jar"),
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

async fn download_jar(target_path: &PathBuf) -> Result<(), String> {
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

async fn start_server(jar_path: &PathBuf, base_url: &str, port: u16) -> Result<(), String> {
    eprintln!(
        "Starting server from {} on port {}...",
        jar_path.display(),
        port
    );

    let child = Command::new("java")
        .args([
            "-jar",
            jar_path.to_str().unwrap_or("Browser4.jar"),
            &format!("--server.port={}", port),
        ])
        .stdin(Stdio::null())
        .stdout(Stdio::null())
        .stderr(Stdio::null())
        .spawn()
        .map_err(|e| format!("Failed to start server: {e}"))?;

    let pid = child.id();

    register_managed_server_process(
        ManagedServerProcess {
            pid,
            base_url: base_url.to_string(),
            port,
            jar_path: jar_path.to_string_lossy().to_string(),
            started_at: chrono::Utc::now().to_rfc3339(),
        },
        None,
    );

    // Detach: we drop the Child handle here. The spawned process continues
    // running independently because we set all stdio to null and call drop().
    // On Unix this means the child is still a child of this process until we
    // exit, at which point it is re-parented to init/systemd. That is
    // sufficient for our use-case where the parent CLI exits immediately.
    drop(child);

    // Wait for health check (up to 60 seconds)
    let client = Client::builder()
        .timeout(std::time::Duration::from_secs(5))
        .build()
        .map_err(|e| e.to_string())?;

    let health_url = format!("http://localhost:{}/actuator/health", port);
    let start = std::time::Instant::now();
    let timeout = std::time::Duration::from_secs(60);

    loop {
        tokio::time::sleep(std::time::Duration::from_secs(1)).await;
        if client.get(&health_url).send().await.is_ok() {
            eprintln!("Server is up and running.");
            return Ok(());
        }
        if start.elapsed() > timeout {
            return Err(format!(
                "Server failed to start within {}s",
                timeout.as_secs()
            ));
        }
    }
}

/// Resolve the base URL from CLI state + optional server override arg.
pub fn resolve_base_url(override_url: Option<&str>) -> String {
    let state = read_state(None);
    let base = override_url
        .map(|s| s.to_string())
        .unwrap_or(state.base_url);
    base.trim_end_matches('/').to_string()
}
