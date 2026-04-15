//! Managed server process registry for the Browser4 CLI.
//!
//! Tracks Browser4 server processes started by this CLI so they can be shut down
//! later via `close-all` or `kill-all`.

use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};

use crate::state::resolve_default_state_dir;

const DEFAULT_REGISTRY_NAME: &str = "cli-managed-processes.json";

/// Information about a managed Browser4 server process.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ManagedServerProcess {
    pub pid: u32,
    #[serde(rename = "baseUrl")]
    pub base_url: String,
    pub port: u16,
    #[serde(rename = "jarPath")]
    pub jar_path: String,
    #[serde(rename = "startedAt")]
    pub started_at: String,
}

#[derive(Debug, Serialize, Deserialize)]
struct ManagedServerProcessRegistry {
    processes: Vec<ManagedServerProcess>,
}

/// Resolve the path to the managed process registry file.
pub fn managed_server_registry_path(state_dir: Option<&Path>) -> PathBuf {
    let dir = state_dir
        .map(|p| p.to_path_buf())
        .unwrap_or_else(resolve_default_state_dir);
    dir.join(DEFAULT_REGISTRY_NAME)
}

/// Read all registered managed server processes from the registry.
pub fn read_managed_server_processes(registry_path: Option<&Path>) -> Vec<ManagedServerProcess> {
    let path = registry_path
        .map(|p| p.to_path_buf())
        .unwrap_or_else(|| managed_server_registry_path(None));

    match fs::read_to_string(&path) {
        Ok(raw) => serde_json::from_str::<ManagedServerProcessRegistry>(&raw)
            .map(|r| r.processes)
            .unwrap_or_default(),
        Err(_) => vec![],
    }
}

/// Register a new managed server process in the registry.
pub fn register_managed_server_process(
    process_info: ManagedServerProcess,
    registry_path: Option<&Path>,
) {
    let path = registry_path
        .map(|p| p.to_path_buf())
        .unwrap_or_else(|| managed_server_registry_path(None));

    let mut existing: Vec<ManagedServerProcess> = read_managed_server_processes(Some(&path))
        .into_iter()
        .filter(|e| e.pid != process_info.pid)
        .collect();
    existing.push(process_info);
    write_managed_server_processes(&existing, &path);
}

/// Remove a managed server process from the registry by PID.
#[allow(dead_code)]
pub fn remove_managed_server_process(pid: u32, registry_path: Option<&Path>) {
    let path = registry_path
        .map(|p| p.to_path_buf())
        .unwrap_or_else(|| managed_server_registry_path(None));

    let remaining: Vec<ManagedServerProcess> = read_managed_server_processes(Some(&path))
        .into_iter()
        .filter(|e| e.pid != pid)
        .collect();
    write_managed_server_processes(&remaining, &path);
}

/// Clear all managed server processes from the registry.
#[allow(dead_code)]
pub fn clear_managed_server_processes(registry_path: Option<&Path>) {
    let path = registry_path
        .map(|p| p.to_path_buf())
        .unwrap_or_else(|| managed_server_registry_path(None));
    let _ = fs::remove_file(path);
}

fn write_managed_server_processes(processes: &[ManagedServerProcess], registry_path: &Path) {
    if let Some(dir) = registry_path.parent() {
        let _ = fs::create_dir_all(dir);
    }

    if processes.is_empty() {
        let _ = fs::remove_file(registry_path);
        return;
    }

    let payload = ManagedServerProcessRegistry {
        processes: processes.to_vec(),
    };
    let json = serde_json::to_string_pretty(&payload).expect("serialisation should not fail");
    let _ = fs::write(registry_path, json);
}

/// Result of a shutdown operation.
#[derive(Debug, Default)]
pub struct ShutdownResult {
    pub stopped_pids: Vec<u32>,
    pub missing_pids: Vec<u32>,
    pub forced_pids: Vec<u32>,
    pub remaining_pids: Vec<u32>,
}

/// Result of force-killing Browser4 Chrome processes.
#[derive(Debug, Default)]
pub struct BrowserKillResult {
    pub killed_pids: Vec<u32>,
    pub remaining_pids: Vec<u32>,
}

/// Result of force-stopping Browser4 server processes and their related Chrome processes.
#[derive(Debug, Default)]
pub struct ForceStopBrowser4ServerResult {
    pub shutdown: ShutdownResult,
    pub browser_kill: BrowserKillResult,
}

fn stop_browser4_server(force: bool) -> ShutdownResult {
    shutdown_managed_server_processes(force, None, 5_000, 250)
}

/// Shut down all managed server processes, optionally forcing them.
pub fn shutdown_managed_server_processes(
    force: bool,
    registry_path: Option<&Path>,
    timeout_ms: u64,
    poll_interval_ms: u64,
) -> ShutdownResult {
    let path = registry_path
        .map(|p| p.to_path_buf())
        .unwrap_or_else(|| managed_server_registry_path(None));

    let tracked = read_managed_server_processes(Some(&path));
    let mut result = ShutdownResult::default();
    let mut remaining: Vec<ManagedServerProcess> = Vec::new();

    for proc in &tracked {
        let pid = proc.pid;
        if !is_process_running(pid) {
            result.missing_pids.push(pid);
            continue;
        }

        if force {
            force_stop(pid);
            result.forced_pids.push(pid);
            if wait_for_exit(pid, timeout_ms, poll_interval_ms) {
                result.stopped_pids.push(pid);
            } else {
                remaining.push(proc.clone());
            }
            continue;
        }

        graceful_stop(pid);
        if wait_for_exit(pid, timeout_ms, poll_interval_ms) {
            result.stopped_pids.push(pid);
            continue;
        }

        force_stop(pid);
        result.forced_pids.push(pid);
        if wait_for_exit(pid, timeout_ms, poll_interval_ms) {
            result.stopped_pids.push(pid);
        } else {
            remaining.push(proc.clone());
        }
    }

    write_managed_server_processes(&remaining, &path);
    result.remaining_pids = remaining.iter().map(|p| p.pid).collect();
    result
}

/// Gracefully stop all managed Browser4 server processes.
pub fn stop_browser4_server_gracefully() -> ShutdownResult {
    stop_browser4_server(false)
}

/// Force-stop all managed Browser4 server processes, then kill all related Chrome processes.
pub fn stop_browser4_server_forcibly() -> ForceStopBrowser4ServerResult {
    ForceStopBrowser4ServerResult {
        shutdown: stop_browser4_server(true),
        browser_kill: kill_all_browsers(),
    }
}

/// Kill all found Browser4 Chrome processes (marked with PULSAR_CHROME).
pub fn kill_all_browsers() -> BrowserKillResult {
    const KILL_TIMEOUT_MS: u64 = 15_000;
    const WAIT_BETWEEN_SWEEPS_MS: u64 = 250;
    const WAIT_AFTER_KILL_MS: u64 = 2_000;

    let start = std::time::Instant::now();
    let timeout = std::time::Duration::from_millis(KILL_TIMEOUT_MS);
    let sweep_delay = std::time::Duration::from_millis(WAIT_BETWEEN_SWEEPS_MS);
    let mut result = BrowserKillResult::default();

    loop {
        let pids = find_unique_pulsar_browser_processes();
        if pids.is_empty() {
            break;
        }

        result.killed_pids.extend(pids.iter().copied());
        for pid in &pids {
            force_stop_browser_process(*pid);
        }
        for pid in &pids {
            let _ = wait_for_exit(*pid, WAIT_AFTER_KILL_MS, 100);
        }

        if start.elapsed() >= timeout {
            break;
        }

        std::thread::sleep(sweep_delay);
    }

    result.killed_pids.sort_unstable();
    result.killed_pids.dedup();
    result.remaining_pids = find_unique_pulsar_browser_processes();
    result
}

fn find_unique_pulsar_browser_processes() -> Vec<u32> {
    let mut pids = find_pulsar_browser_processes();
    pids.sort_unstable();
    pids.dedup();
    pids
}

fn find_pulsar_browser_processes() -> Vec<u32> {
    let mut pids = Vec::new();

    #[cfg(unix)]
    {
        use std::process::Command;
        if let Ok(output) = Command::new("pgrep").args(["-f", "PULSAR_CHROME"]).output() {
            pids.extend(parse_pid_list(&output.stdout));
        }
    }

    #[cfg(windows)]
    {
        use std::process::Command;
        let ps_command = r#"
            Get-CimInstance Win32_Process -Filter "Name = 'chrome.exe'" -ErrorAction SilentlyContinue |
                Where-Object {
                    -not [string]::IsNullOrWhiteSpace($_.CommandLine) -and
                    $_.CommandLine -match 'PULSAR_CHROME'
                } |
                Select-Object -ExpandProperty ProcessId
        "#;
        if let Ok(output) = Command::new("powershell")
            .args(["-NoProfile", "-NonInteractive", "-Command", ps_command])
            .output()
        {
            pids.extend(parse_pid_list(&output.stdout));
        }
    }

    pids
}

fn parse_pid_list(stdout: &[u8]) -> Vec<u32> {
    String::from_utf8_lossy(stdout)
        .lines()
        .filter_map(|line| line.trim().parse::<u32>().ok())
        .collect()
}

// ---------------------------------------------------------------------------
// Platform-specific process control
// ---------------------------------------------------------------------------

fn is_process_running(pid: u32) -> bool {
    #[cfg(unix)]
    {
        use std::process::Command;
        // SIGCHECK (signal 0) returns success if process exists.
        // Suppress stderr to avoid "kill: No such process" noise when the
        // process has already exited.
        let status = Command::new("kill")
            .args(["-0", &pid.to_string()])
            .stderr(std::process::Stdio::null())
            .status();
        status.map(|s| s.success()).unwrap_or(false)
    }
    #[cfg(windows)]
    {
        use std::process::Command;
        let output = Command::new("tasklist")
            .args(["/FI", &format!("PID eq {}", pid), "/FO", "CSV", "/NH"])
            .output();
        match output {
            Ok(out) => {
                let stdout = String::from_utf8_lossy(&out.stdout);
                stdout.contains(&pid.to_string())
            }
            Err(_) => false,
        }
    }
}

fn graceful_stop(pid: u32) {
    #[cfg(unix)]
    {
        let _ = std::process::Command::new("kill")
            .args(["-TERM", &pid.to_string()])
            .status();
    }
    #[cfg(windows)]
    {
        // Try jcmd first (graceful JVM exit), fallback to Stop-Process
        let jcmd_result = std::process::Command::new("jcmd")
            .args([&pid.to_string(), "VM.exit", "0"])
            .status();
        if jcmd_result.is_err() || jcmd_result.map(|s| !s.success()).unwrap_or(true) {
            let _ = std::process::Command::new("powershell")
                .args([
                    "-NoProfile",
                    "-NonInteractive",
                    "-Command",
                    &format!("Stop-Process -Id {}", pid),
                ])
                .status();
        }
    }
}

fn force_stop(pid: u32) {
    #[cfg(unix)]
    {
        let _ = std::process::Command::new("kill")
            .args(["-KILL", &pid.to_string()])
            .stderr(std::process::Stdio::null())
            .status();
    }
    #[cfg(windows)]
    {
        let _ = std::process::Command::new("powershell")
            .args([
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                &format!(
                    "Stop-Process -Id {} -Force -ErrorAction SilentlyContinue",
                    pid
                ),
            ])
            .status();
    }
}

fn force_stop_browser_process(pid: u32) {
    #[cfg(unix)]
    {
        force_stop(pid);
    }

    #[cfg(windows)]
    {
        let taskkill_failed = match std::process::Command::new("taskkill")
            .args(["/PID", &pid.to_string(), "/T", "/F"])
            .stdout(std::process::Stdio::null())
            .stderr(std::process::Stdio::null())
            .status()
        {
            Ok(status) => !status.success(),
            Err(_) => true,
        };

        if taskkill_failed {
            force_stop(pid);
        }
    }
}

fn wait_for_exit(pid: u32, timeout_ms: u64, poll_interval_ms: u64) -> bool {
    let start = std::time::Instant::now();
    let timeout = std::time::Duration::from_millis(timeout_ms);
    let poll = std::time::Duration::from_millis(poll_interval_ms);
    while start.elapsed() < timeout {
        if !is_process_running(pid) {
            return true;
        }
        std::thread::sleep(poll);
    }
    !is_process_running(pid)
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;

    #[test]
    fn test_register_and_remove() {
        let tmp = TempDir::new().unwrap();
        let reg_path = tmp.path().join("reg.json");

        let proc = ManagedServerProcess {
            pid: 12345,
            base_url: "http://localhost:8182".to_string(),
            port: 8182,
            jar_path: "/path/to/Browser4.jar".to_string(),
            started_at: "2026-01-01T00:00:00Z".to_string(),
        };

        register_managed_server_process(proc.clone(), Some(&reg_path));
        let procs = read_managed_server_processes(Some(&reg_path));
        assert_eq!(procs.len(), 1);
        assert_eq!(procs[0].pid, 12345);

        remove_managed_server_process(12345, Some(&reg_path));
        let procs = read_managed_server_processes(Some(&reg_path));
        assert!(procs.is_empty());
    }

    #[test]
    fn test_read_missing_registry() {
        let tmp = TempDir::new().unwrap();
        let reg_path = tmp.path().join("missing.json");
        let procs = read_managed_server_processes(Some(&reg_path));
        assert!(procs.is_empty());
    }

    #[test]
    fn test_clear_registry() {
        let tmp = TempDir::new().unwrap();
        let reg_path = tmp.path().join("reg.json");

        let proc = ManagedServerProcess {
            pid: 99999,
            base_url: "http://localhost:8182".to_string(),
            port: 8182,
            jar_path: "/tmp/Browser4.jar".to_string(),
            started_at: "2026-01-01T00:00:00Z".to_string(),
        };
        register_managed_server_process(proc, Some(&reg_path));
        assert!(reg_path.exists());

        clear_managed_server_processes(Some(&reg_path));
        assert!(!reg_path.exists());
    }

    #[test]
    fn test_parse_pid_list_ignores_noise() {
        let stdout = b"123\nwarning\n\n456 \nnot-a-pid\n789\r\n";
        assert_eq!(parse_pid_list(stdout), vec![123, 456, 789]);
    }
}
