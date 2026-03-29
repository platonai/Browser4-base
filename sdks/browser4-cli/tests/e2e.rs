//! End-to-end tests for the `browser4-cli` Rust binary.
//!
//! The browser-backed scenarios run sequentially in a custom `harness = false`
//! test target so they can reuse the proven ordering without libtest starting
//! multiple Browser4 backends concurrently. Covered commands are tracked per
//! scenario via [`E2ECtx::covered_commands`]; the dedicated coverage check
//! verifies that the union of all tested commands plus the explicitly-excluded
//! set equals the full command list from [`browser4_cli::commands::all_commands`].
//!
//! # Running
//!
//! ```bash
//! cargo test --test e2e -- --nocapture
//! ```
//!
//! The Browser4 jar is resolved from (in order):
//! 1. `BROWSER4_E2E_JAR_PATH` environment variable
//! 2. `<repo_root>/browser4/browser4-agents/target/Browser4.jar`

use std::collections::HashSet;
use std::fs;
use std::io::{Read, Write};
use std::net::TcpListener;
use std::path::{Path, PathBuf};
use std::process::{Child, Command, Stdio};
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::thread;
use std::time::{Duration, Instant};

use browser4_cli::commands::all_commands;

// ---------------------------------------------------------------------------
// Constants
// ---------------------------------------------------------------------------

const INTERACTIVE_PATH: &str = "/interactive";
const OTHER_PATH: &str = "/other";
const INTERACTIVE_TITLE: &str = "Browser4 CLI Interactive Fixture";
const OTHER_TITLE: &str = "Browser4 CLI Other Fixture";

// ---------------------------------------------------------------------------
// Environment helpers
// ---------------------------------------------------------------------------

fn cli_binary() -> PathBuf {
    PathBuf::from(env!("CARGO_BIN_EXE_browser4-cli"))
}

fn repo_root() -> PathBuf {
    // CARGO_MANIFEST_DIR → sdks/browser4-cli  →  pop twice → repo root
    let mut path = PathBuf::from(env!("CARGO_MANIFEST_DIR"));
    path.pop(); // browser4-cli
    path.pop(); // sdks
    path
}

fn default_jar_path() -> PathBuf {
    repo_root()
        .join("browser4")
        .join("browser4-agents")
        .join("target")
        .join("Browser4.jar")
}

// ---------------------------------------------------------------------------
// HTML fixtures (matching the TypeScript tests verbatim)
// ---------------------------------------------------------------------------

fn interactive_html() -> String {
    format!(
        r#"<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>{title}</title>
  <style>
    body {{ margin: 0; font-family: sans-serif; min-height: 2400px; }}
    #mouse-area {{
      position: fixed;
      top: 0;
      left: 0;
      width: 480px;
      height: 320px;
      background: rgba(0, 128, 255, 0.08);
      border: 1px solid #08f;
    }}
    #drag-source, #drag-target {{
      width: 160px;
      height: 48px;
      margin-top: 16px;
      border: 1px solid #555;
      display: flex;
      align-items: center;
      justify-content: center;
    }}
  </style>
</head>
<body>
  <div id="mouse-area">mouse area</div>
  <main style="padding: 360px 24px 24px;">
    <input id="type-target" type="text" />
    <input id="fill-target" type="text" />
    <input id="file-input" type="file" />
    <input id="check-target" type="checkbox" />
    <select id="select-target">
      <option value="">-- choose --</option>
      <option value="green">Green</option>
      <option value="blue">Blue</option>
    </select>
    <button id="click-target" type="button">Click</button>
    <button id="dblclick-target" type="button">Double Click</button>
    <button id="hover-target" type="button">Hover</button>
    <button id="prompt-target" type="button">Prompt</button>
    <button id="confirm-target" type="button">Confirm</button>
    <div id="drag-source" draggable="true">drag source</div>
    <div id="drag-target">drag target</div>
    <pre id="state-log"></pre>
  </main>
  <script>
    window.__browser4State = {{
      clickCount: 0,
      doubleClickCount: 0,
      hovered: false,
      dragStarted: false,
      dragDropped: '',
      promptResult: '',
      confirmResult: '',
      keyEvents: [],
      mouseDownCount: 0,
      mouseUpCount: 0,
      lastMouse: null,
      lastWheel: null,
      typeValue: '',
      fillValue: '',
      checkbox: false,
      selectValue: '',
      uploadCount: 0,
      uploadName: '',
      submitCount: 0
    }};

    function syncState() {{
      const state = window.__browser4State;
      state.typeValue = document.getElementById('type-target').value;
      state.fillValue = document.getElementById('fill-target').value;
      state.checkbox = document.getElementById('check-target').checked;
      state.selectValue = document.getElementById('select-target').value;
      const files = document.getElementById('file-input').files;
      state.uploadCount = files ? files.length : 0;
      state.uploadName = files && files[0] ? files[0].name : '';
      document.getElementById('state-log').textContent = JSON.stringify(state);
    }}

    document.getElementById('click-target').addEventListener('click', () => {{
      window.__browser4State.clickCount += 1;
      console.info('click-target clicked');
      syncState();
    }});

    document.getElementById('dblclick-target').addEventListener('dblclick', () => {{
      window.__browser4State.doubleClickCount += 1;
      syncState();
    }});

    document.getElementById('hover-target').addEventListener('mouseenter', () => {{
      window.__browser4State.hovered = true;
      syncState();
    }});

    document.getElementById('drag-source').addEventListener('dragstart', (event) => {{
      window.__browser4State.dragStarted = true;
      event.dataTransfer.setData('text/plain', 'drag-source');
      syncState();
    }});

    document.getElementById('drag-target').addEventListener('dragover', (event) => {{
      event.preventDefault();
    }});

    document.getElementById('drag-target').addEventListener('drop', (event) => {{
      event.preventDefault();
      window.__browser4State.dragDropped = event.dataTransfer.getData('text/plain');
      syncState();
    }});

    document.getElementById('prompt-target').addEventListener('click', () => {{
      setTimeout(() => {{
        const value = window.prompt('Enter prompt text', 'seed');
        window.__browser4State.promptResult = value === null ? '__dismissed__' : value;
        syncState();
      }}, 0);
    }});

    document.getElementById('confirm-target').addEventListener('click', () => {{
      setTimeout(() => {{
        const accepted = window.confirm('Confirm action');
        window.__browser4State.confirmResult = accepted ? 'accepted' : 'dismissed';
        syncState();
      }}, 0);
    }});

    document.getElementById('fill-target').addEventListener('keydown', (event) => {{
      if (event.key === 'Enter') {{
        window.__browser4State.submitCount += 1;
        syncState();
      }}
    }});

    document.addEventListener('keydown', (event) => {{
      window.__browser4State.keyEvents.push('down:' + event.key);
      syncState();
    }});

    document.addEventListener('keyup', (event) => {{
      window.__browser4State.keyEvents.push('up:' + event.key);
      syncState();
    }});

    document.getElementById('type-target').addEventListener('input', syncState);
    document.getElementById('fill-target').addEventListener('input', syncState);
    document.getElementById('check-target').addEventListener('change', syncState);
    document.getElementById('select-target').addEventListener('change', syncState);
    document.getElementById('file-input').addEventListener('change', syncState);

    const mouseArea = document.getElementById('mouse-area');
    mouseArea.addEventListener('mousemove', (event) => {{
      window.__browser4State.lastMouse = [Math.round(event.clientX), Math.round(event.clientY)];
      syncState();
    }});
    mouseArea.addEventListener('mousedown', () => {{
      window.__browser4State.mouseDownCount += 1;
      syncState();
    }});
    mouseArea.addEventListener('mouseup', () => {{
      window.__browser4State.mouseUpCount += 1;
      syncState();
    }});
    mouseArea.addEventListener('wheel', (event) => {{
      window.__browser4State.lastWheel = [Math.round(event.deltaX), Math.round(event.deltaY)];
      syncState();
    }});

    console.info('interactive fixture ready');
    syncState();
  </script>
</body>
</html>"#,
        title = INTERACTIVE_TITLE
    )
}

fn other_html() -> String {
    format!(
        r#"<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <title>{title}</title>
</head>
<body>
  <h1 id="page-marker">other page</h1>
  <script>console.info('other fixture ready');</script>
</body>
</html>"#,
        title = OTHER_TITLE
    )
}

// ---------------------------------------------------------------------------
// Free-port helper
// ---------------------------------------------------------------------------

fn find_free_port() -> u16 {
    let listener = TcpListener::bind("127.0.0.1:0").expect("bind on port 0 failed");
    listener.local_addr().unwrap().port()
}

// ---------------------------------------------------------------------------
// Minimal HTTP fixture server
// ---------------------------------------------------------------------------

struct FixtureServer {
    port: u16,
    shutdown: Arc<AtomicBool>,
}

impl FixtureServer {
    fn start() -> Self {
        let listener = TcpListener::bind("127.0.0.1:0").expect("fixture server bind failed");
        let port = listener.local_addr().unwrap().port();
        let shutdown = Arc::new(AtomicBool::new(false));
        let flag = shutdown.clone();

        thread::spawn(move || {
            listener.set_nonblocking(true).ok();
            loop {
                if flag.load(Ordering::Relaxed) {
                    break;
                }
                match listener.accept() {
                    Ok((stream, _)) => {
                        thread::spawn(move || serve_fixture_request(stream));
                    }
                    Err(ref e) if e.kind() == std::io::ErrorKind::WouldBlock => {
                        thread::sleep(Duration::from_millis(5));
                    }
                    Err(_) => break,
                }
            }
        });

        Self { port, shutdown }
    }

    fn base_url(&self) -> String {
        format!("http://127.0.0.1:{}", self.port)
    }
}

impl Drop for FixtureServer {
    fn drop(&mut self) {
        self.shutdown.store(true, Ordering::Relaxed);
    }
}

fn serve_fixture_request(mut stream: std::net::TcpStream) {
    let mut buf = vec![0u8; 8192];
    let n = match stream.read(&mut buf) {
        Ok(n) => n,
        Err(_) => return,
    };

    let request = std::str::from_utf8(&buf[..n]).unwrap_or("");
    let path = request
        .lines()
        .next()
        .and_then(|line| line.split_whitespace().nth(1))
        .unwrap_or("/");

    let (status, content_type, body) = if path == INTERACTIVE_PATH || path == "/" {
        ("200 OK", "text/html; charset=utf-8", interactive_html())
    } else if path == OTHER_PATH {
        ("200 OK", "text/html; charset=utf-8", other_html())
    } else {
        (
            "404 Not Found",
            "text/plain; charset=utf-8",
            "not found".to_string(),
        )
    };

    let response = format!(
        "HTTP/1.1 {}\r\nContent-Type: {}\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",
        status,
        content_type,
        body.len(),
        body
    );
    let _ = stream.write_all(response.as_bytes());
}

// ---------------------------------------------------------------------------
// Browser4 backend server
// ---------------------------------------------------------------------------

struct Browser4Server {
    child: Child,
}

impl Browser4Server {
    /// Start the Browser4 jar on the given base URL's port, waiting up to 120 s for health.
    fn start(base_url: &str, jar_path: &Path) -> Self {
        let port = reqwest::Url::parse(base_url)
            .ok()
            .and_then(|u| u.port())
            .map(|p| p.to_string())
            .unwrap_or_else(|| "8182".to_string());

        let child = Command::new("java")
            .args([
                "-jar",
                jar_path.to_str().expect("jar path not UTF-8"),
                &format!("--server.port={}", port),
            ])
            .current_dir(jar_path.parent().unwrap_or(Path::new(".")))
            .stdin(Stdio::null())
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .spawn()
            .expect("failed to spawn Browser4 java process");

        wait_for_health(base_url, 120_000).expect("Browser4 did not become healthy in time");

        Self { child }
    }
}

impl Drop for Browser4Server {
    fn drop(&mut self) {
        let _ = self.child.kill();
        let _ = self.child.wait();
    }
}

fn wait_for_health(base_url: &str, timeout_ms: u64) -> Result<(), String> {
    let health_url = format!("{}/actuator/health", base_url.trim_end_matches('/'));
    let tools_url = format!("{}/mcp/tools", base_url.trim_end_matches('/'));
    let client = reqwest::blocking::Client::builder()
        .timeout(Duration::from_secs(5))
        .build()
        .expect("reqwest blocking client build failed");

    let deadline = Instant::now() + Duration::from_millis(timeout_ms);
    let mut last_error = String::from("unknown");

    while Instant::now() < deadline {
        match client.get(&health_url).send() {
            Ok(resp) => {
                let body = resp.text().unwrap_or_default();
                if body.contains("\"status\":\"UP\"") {
                    match client.get(&tools_url).send() {
                        Ok(tools_resp) => {
                            let tools_body = tools_resp.text().unwrap_or_default();
                            if tools_body.contains("open_session") && tools_body.contains("browser_navigate") {
                                return Ok(());
                            }
                            last_error = format!("MCP tools endpoint not ready: {tools_body}");
                        }
                        Err(e) => {
                            last_error = format!("MCP tools endpoint not ready: {e}");
                        }
                    }
                } else {
                    last_error = body;
                }
            }
            Err(e) => last_error = e.to_string(),
        }
        thread::sleep(Duration::from_secs(1));
    }

    Err(format!(
        "Browser4 did not become healthy within {}ms. Last response: {}",
        timeout_ms, last_error
    ))
}

// ---------------------------------------------------------------------------
// CLI runner
// ---------------------------------------------------------------------------

struct CliRunResult {
    stdout: String,
    stderr: String,
    exit_code: i32,
}

/// Context for running CLI commands in isolation.
struct E2ECtx {
    fixture_base_url: String,
    browser4_base_url: String,
    workspace_dir: PathBuf,
    state_dir: PathBuf,
    upload_file_path: PathBuf,
    covered_commands: HashSet<String>,
}

impl E2ECtx {
    fn interactive_url(&self) -> String {
        format!("{}{}", self.fixture_base_url, INTERACTIVE_PATH)
    }

    fn other_url(&self) -> String {
        format!("{}{}", self.fixture_base_url, OTHER_PATH)
    }
}

struct E2ETestResources {
    _temp_dir: tempfile::TempDir,
    browser4: Option<Browser4Server>,
    _fixture: FixtureServer,
    browser4_jar_path: PathBuf,
    ctx: E2ECtx,
}

impl E2ETestResources {
    fn restart_browser4(&mut self) {
        self.browser4 = None;
        let browser4_port = find_free_port();
        self.ctx.browser4_base_url = format!("http://127.0.0.1:{}", browser4_port);
        self.browser4 = Some(Browser4Server::start(
            &self.ctx.browser4_base_url,
            &self.browser4_jar_path,
        ));
    }
}

/// Run `browser4-cli --server=<url> <args...>` in the workspace dir with the isolated state dir.
fn run_cli_process(ctx: &E2ECtx, args: &[&str]) -> CliRunResult {
    let server_arg = format!("--server={}", ctx.browser4_base_url);
    let mut full_args: Vec<&str> = vec![server_arg.as_str()];
    full_args.extend_from_slice(args);

    let output = Command::new(cli_binary())
        .args(&full_args)
        .current_dir(&ctx.workspace_dir)
        .env("BROWSER4_CLI_STATE_DIR", &ctx.state_dir)
        .stdin(Stdio::null())
        .stdout(Stdio::piped())
        .stderr(Stdio::piped())
        .output()
        .expect("failed to spawn browser4-cli process");

    CliRunResult {
        stdout: String::from_utf8_lossy(&output.stdout).into_owned(),
        stderr: String::from_utf8_lossy(&output.stderr).into_owned(),
        exit_code: output.status.code().unwrap_or(-1),
    }
}

/// Run a command, asserting it succeeds (exit code 0).
fn run_command<'a>(ctx: &mut E2ECtx, args: &[&'a str]) -> CliRunResult {
    if let Some(cmd) = args.first() {
        ctx.covered_commands.insert(cmd.to_string());
    }
    let result = run_cli_process_with_retry(ctx, args);
    assert_eq!(
        result.exit_code, 0,
        "Command {:?} failed (exit={}):\nstdout:\n{}\nstderr:\n{}",
        args, result.exit_code, result.stdout, result.stderr
    );
    result
}

/// Run a command, asserting it fails (exit code != 0) and that the combined
/// stdout+stderr contains `pattern`.
fn run_command_expecting_failure(ctx: &mut E2ECtx, args: &[&str], pattern: &str) -> CliRunResult {
    if let Some(cmd) = args.first() {
        ctx.covered_commands.insert(cmd.to_string());
    }
    let result = run_cli_process_with_retry(ctx, args);
    assert_ne!(
        result.exit_code, 0,
        "Expected command {:?} to fail, but it exited with 0.\nstdout:\n{}\nstderr:\n{}",
        args, result.stdout, result.stderr
    );
    let combined = format!("{}\n{}", result.stdout, result.stderr);
    assert!(
        combined.contains(pattern),
        "Expected output to contain '{pattern}', but got:\n{combined}"
    );
    result
}

fn run_cli_process_with_retry(ctx: &E2ECtx, args: &[&str]) -> CliRunResult {
    let max_attempts = 3;
    let mut attempt = 0;

    loop {
        attempt += 1;
        let result = run_cli_process(ctx, args);
        if attempt >= max_attempts || !is_transient_transport_failure(&result) {
            return result;
        }
        thread::sleep(Duration::from_secs(2));
    }
}

fn is_transient_transport_failure(result: &CliRunResult) -> bool {
    if result.exit_code == 0 {
        return false;
    }

    let combined = format!("{}\n{}", result.stdout, result.stderr).to_lowercase();
    combined.contains("http request failed: error sending request for url")
        || combined.contains("connection refused")
        || combined.contains("tcp connect error")
}

// ---------------------------------------------------------------------------
// Output helpers
// ---------------------------------------------------------------------------

/// Strip the auto-appended `### Page` snapshot block from CLI stdout.
fn strip_snapshot_output(stdout: &str) -> String {
    let marker = "\n### Page";
    let without = match stdout.find(marker) {
        Some(idx) => &stdout[..idx],
        None => stdout,
    };
    without
        .lines()
        .map(str::trim)
        .filter(|l| !l.is_empty() && *l != "ensuring server...")
        .collect::<Vec<_>>()
        .join("\n")
}

/// Extract a tab ID for the given URL from `tab-list` output.
///
/// Looks for a pattern like `id:<VALUE>` (or `id="<VALUE>"`) followed — on the
/// same or a later line — by the URL, similar to the TypeScript `extractTabId`.
fn extract_tab_id(output: &str, url: &str) -> String {
    // Match "id" followed by ":" or "=" then an optional quote, then the value,
    // somewhere before the URL on the same block.  We iterate over lines/chunks
    // because the backend may format as YAML / JSON / plaintext.
    static RE: std::sync::OnceLock<regex::Regex> = std::sync::OnceLock::new();
    let re = RE.get_or_init(|| {
        regex::Regex::new(r#"id[:=]"?([^",}\s]+)"?"#).expect("tab id regex compile")
    });

    // Collect all (id_value, byte_position) pairs.
    let ids: Vec<(String, usize)> = re
        .captures_iter(output)
        .filter_map(|cap| {
            let m = cap.get(0)?;
            Some((cap[1].to_string(), m.start()))
        })
        .collect();

    // Find the position of our target URL in the output.
    let url_pos = output
        .find(url)
        .unwrap_or_else(|| panic!("URL '{}' not found in tab list output:\n{}", url, output));

    // Pick the id that appears immediately before the URL.
    ids.iter()
        .filter(|(_, pos)| *pos < url_pos)
        .last()
        .map(|(id, _)| id.clone())
        .unwrap_or_else(|| panic!("Could not find tab id for '{}' in:\n{}", url, output))
}

// ---------------------------------------------------------------------------
// State helpers
// ---------------------------------------------------------------------------

fn read_persisted_session_id(state_dir: &Path) -> String {
    let path = state_dir.join("cli-state.json");
    let raw = fs::read_to_string(&path).expect("cli-state.json not found");
    let parsed: serde_json::Value =
        serde_json::from_str(&raw).expect("cli-state.json is not valid JSON");
    parsed["sessionId"]
        .as_str()
        .expect("no sessionId in cli-state.json")
        .to_string()
}

fn eval_text(ctx: &mut E2ECtx, expression: &str) -> String {
    let result = run_command(ctx, &["eval", expression]);
    strip_snapshot_output(&result.stdout)
}

fn read_interactive_state(ctx: &mut E2ECtx) -> serde_json::Value {
    let text = eval_text(ctx, "document.getElementById('state-log').textContent");
    serde_json::from_str(text.trim()).unwrap_or(serde_json::Value::Null)
}

fn key_event_count(state: &serde_json::Value) -> usize {
    state["keyEvents"].as_array().map_or(0, |events| events.len())
}

fn wait_for_state<F>(ctx: &mut E2ECtx, predicate: F, timeout_ms: u64) -> serde_json::Value
where
    F: Fn(&serde_json::Value) -> bool,
{
    let deadline = Instant::now() + Duration::from_millis(timeout_ms);
    while Instant::now() < deadline {
        let state = read_interactive_state(ctx);
        if predicate(&state) {
            return state;
        }
        thread::sleep(Duration::from_millis(300));
    }
    let state = read_interactive_state(ctx);
    panic!("Timed out waiting for interactive state. Last state:\n{state:#?}");
}

fn wait_for_eval_text(
    ctx: &mut E2ECtx,
    expression: &str,
    expected: &str,
    timeout_ms: u64,
    failure_message: &str,
) {
    let deadline = Instant::now() + Duration::from_millis(timeout_ms);
    let mut last_value = String::new();

    while Instant::now() < deadline {
        last_value = eval_text(ctx, expression);
        if last_value == expected {
            return;
        }
        thread::sleep(Duration::from_millis(300));
    }

    panic!("{failure_message}. Expected '{expected}', got '{last_value}'");
}

// ---------------------------------------------------------------------------
// Per-test isolation helper
// ---------------------------------------------------------------------------

fn reset_cli_artifacts(ctx: &E2ECtx) {
    let _ = fs::remove_dir_all(&ctx.state_dir);
    fs::create_dir_all(&ctx.state_dir).ok();
    let _ = fs::remove_dir_all(ctx.workspace_dir.join(".browser4-cli"));
}

fn create_e2e_test_resources() -> E2ETestResources {
    let jar_path = std::env::var("BROWSER4_E2E_JAR_PATH")
        .map(PathBuf::from)
        .unwrap_or_else(|_| default_jar_path());

    assert!(
        jar_path.exists(),
        "Browser4 jar not found at {jar_path:?}. \
        Build browser4/browser4-agents first or set BROWSER4_E2E_JAR_PATH."
    );
    assert!(
        cli_binary().exists(),
        "CLI binary not found at {:?}. Run `cargo build` first.",
        cli_binary()
    );

    let fixture = FixtureServer::start();
    let fixture_base_url = fixture.base_url();
    let browser4_port = find_free_port();
    let browser4_base_url = format!("http://127.0.0.1:{}", browser4_port);

    let temp_dir = tempfile::TempDir::new().expect("tempdir creation failed");
    let workspace_dir = temp_dir.path().join("workspace");
    let state_dir = temp_dir.path().join("state");
    fs::create_dir_all(&workspace_dir).unwrap();
    fs::create_dir_all(&state_dir).unwrap();

    let upload_file_path = temp_dir.path().join("upload.txt");
    fs::write(&upload_file_path, b"browser4-cli e2e upload payload")
        .expect("write upload file failed");

    E2ETestResources {
        _temp_dir: temp_dir,
        browser4: None,
        _fixture: fixture,
        browser4_jar_path: jar_path,
        ctx: E2ECtx {
            fixture_base_url,
            browser4_base_url,
            workspace_dir,
            state_dir,
            upload_file_path,
            covered_commands: HashSet::new(),
        },
    }
}

// ---------------------------------------------------------------------------
// Test scenarios
// ---------------------------------------------------------------------------

fn test_session_and_navigation(ctx: &mut E2ECtx) {
    reset_cli_artifacts(ctx);

    let open_result = run_command(ctx, &["open"]);
    assert!(
        open_result.stdout.contains("Session opened:"),
        "Expected 'Session opened:' in:\n{}",
        open_result.stdout
    );

    let session_id = read_persisted_session_id(&ctx.state_dir);
    let list_result = run_command(ctx, &["list"]);
    assert!(
        list_result.stdout.contains(&session_id),
        "Expected session id '{session_id}' in list output:\n{}",
        list_result.stdout
    );

    let interactive_url = ctx.interactive_url();
    let other_url = ctx.other_url();

    run_command(ctx, &["goto", &interactive_url]);
    wait_for_eval_text(
        ctx,
        "window.location.pathname",
        INTERACTIVE_PATH,
        15_000,
        "Expected to be on interactive path",
    );

    run_command(ctx, &["goto", &other_url]);
    wait_for_eval_text(
        ctx,
        "document.title",
        OTHER_TITLE,
        15_000,
        "Expected other page title",
    );

    run_command(ctx, &["go-back"]);
    wait_for_eval_text(
        ctx,
        "window.location.pathname",
        INTERACTIVE_PATH,
        15_000,
        "Expected to be back on interactive path after go-back",
    );

    run_command(ctx, &["go-forward"]);
    wait_for_eval_text(
        ctx,
        "window.location.pathname",
        OTHER_PATH,
        15_000,
        "Expected to be on other path after go-forward",
    );

    run_command(ctx, &["reload"]);
    wait_for_eval_text(
        ctx,
        "document.title",
        OTHER_TITLE,
        15_000,
        "Expected other page title after reload",
    );

    let delete_result = run_command(ctx, &["delete-data"]);
    let stripped = strip_snapshot_output(&delete_result.stdout).to_lowercase();
    assert!(
        stripped.contains("deleted"),
        "Expected 'deleted' in delete-data output:\n{}",
        stripped
    );

    let close_result = run_command(ctx, &["close"]);
    assert!(
        close_result.stdout.contains("Session closed."),
        "Expected 'Session closed.' in:\n{}",
        close_result.stdout
    );
}

fn test_interaction_console_and_export(ctx: &mut E2ECtx) {
    reset_cli_artifacts(ctx);

    run_command(ctx, &["open"]);
    let interactive_url = ctx.interactive_url();
    run_command(ctx, &["goto", &interactive_url]);

    // resize
    let resize_result = run_command(ctx, &["resize", "1280", "900"]);
    assert!(
        resize_result.stdout.contains("### Page"),
        "Expected '### Page' in resize output:\n{}",
        resize_result.stdout
    );
    let vw: u64 = eval_text(ctx, "window.innerWidth.toString()")
        .parse()
        .unwrap_or(0);
    assert!(vw >= 1000, "Expected viewport width >= 1000, got {vw}");

    // type
    run_command(ctx, &["type", "#type-target", "hello world"]);
    wait_for_state(
        ctx,
        |s| s["typeValue"].as_str() == Some("hello world"),
        15_000,
    );

    // fill
    run_command(ctx, &["fill", "#fill-target", "filled text"]);
    wait_for_state(
        ctx,
        |s| s["fillValue"].as_str() == Some("filled text"),
        15_000,
    );

    let press_before = read_interactive_state(ctx);
    let press_before_events = key_event_count(&press_before);
    run_command(ctx, &["press", "#type-target", "!"]);
    wait_for_state(
        ctx,
        |s| {
            s["typeValue"].as_str() == Some("hello world!")
                && key_event_count(s) > press_before_events
        },
        15_000,
    );

    // keydown / keyup
    run_command(ctx, &["click", "#type-target"]);
    let keydown_before = key_event_count(&read_interactive_state(ctx));
    run_command(ctx, &["keydown", "Shift"]);
    wait_for_state(
        ctx,
        |s| {
            key_event_count(s) > keydown_before
                && s["keyEvents"]
                    .as_array()
                    .and_then(|events| events.last())
                    .and_then(|event| event.as_str())
                    == Some("down:Shift")
        },
        15_000,
    );

    let keyup_before = key_event_count(&read_interactive_state(ctx));
    run_command(ctx, &["keyup", "Shift"]);
    wait_for_state(
        ctx,
        |s| {
            key_event_count(s) > keyup_before
                && s["keyEvents"]
                    .as_array()
                    .and_then(|events| events.last())
                    .and_then(|event| event.as_str())
                    == Some("up:Shift")
        },
        15_000,
    );

    run_command(ctx, &["click", "#click-target"]);
    wait_for_state(
        ctx,
        |s| s["clickCount"].as_u64() == Some(1),
        15_000,
    );

    run_command(ctx, &["dblclick", "#dblclick-target"]);
    wait_for_state(
        ctx,
        |s| s["doubleClickCount"].as_u64() == Some(1),
        15_000,
    );

    run_command(ctx, &["hover", "#hover-target"]);
    wait_for_state(ctx, |s| s["hovered"].as_bool() == Some(true), 15_000);

    run_command(ctx, &["drag", "#drag-source", "#drag-target"]);
    wait_for_state(
        ctx,
        |s| {
            s["dragStarted"].as_bool() == Some(true)
                && s["dragDropped"].as_str() == Some("drag-source")
        },
        15_000,
    );

    // select
    run_command(ctx, &["select", "#select-target", "green"]);
    wait_for_state(ctx, |s| s["selectValue"].as_str() == Some("green"), 15_000);

    // check / uncheck
    run_command(ctx, &["check", "#check-target"]);
    wait_for_state(ctx, |s| s["checkbox"].as_bool() == Some(true), 15_000);

    run_command(ctx, &["uncheck", "#check-target"]);
    wait_for_state(ctx, |s| s["checkbox"].as_bool() == Some(false), 15_000);

    // upload
    let upload_path = ctx.upload_file_path.to_string_lossy().into_owned();
    run_command(ctx, &["upload", "#file-input", &upload_path]);
    wait_for_state(
        ctx,
        |s| s["uploadName"].as_str() == Some("upload.txt"),
        15_000,
    );

    // console – expected to fail (tool not supported by backend)
    run_command_expecting_failure(
        ctx,
        &["console", "info"],
        "Unknown tool: browser_console_messages",
    );

    // snapshot with explicit filename
    let snapshot_result = run_command(ctx, &["snapshot", "--filename=interactive.yml"]);
    assert!(
        snapshot_result.stdout.contains("[Snapshot]("),
        "Expected '[Snapshot](' in snapshot output:\n{}",
        snapshot_result.stdout
    );
    let snapshot_path = ctx
        .workspace_dir
        .join(".browser4-cli")
        .join("snapshot")
        .join("interactive.yml");
    assert!(
        snapshot_path.exists(),
        "Snapshot file not found at {snapshot_path:?}"
    );

    // screenshot with explicit filename
    run_command(ctx, &["screenshot", "--filename=interactive.png"]);
    let screenshot_path = ctx
        .workspace_dir
        .join(".browser4-cli")
        .join("snapshot")
        .join("interactive.png");
    assert!(screenshot_path.exists(), "Screenshot file not found");
    assert!(
        fs::metadata(&screenshot_path).unwrap().len() > 0,
        "Screenshot file is empty"
    );

    // pdf – expected to fail (tool not supported by backend)
    run_command_expecting_failure(
        ctx,
        &["pdf", "--filename=interactive.pdf"],
        "Unknown tool: browser_pdf_save",
    );

    run_command(ctx, &["close"]);
}

fn test_mouse_and_dialog(ctx: &mut E2ECtx) {
    reset_cli_artifacts(ctx);

    run_command(ctx, &["open"]);
    let interactive_url = ctx.interactive_url();
    run_command(ctx, &["goto", &interactive_url]);
    run_command(ctx, &["resize", "1280", "900"]);

    // mousemove
    run_command(ctx, &["mousemove", "120", "120"]);
    wait_for_state(
        ctx,
        |s| s["lastMouse"][0].as_i64() == Some(120) && s["lastMouse"][1].as_i64() == Some(120),
        15_000,
    );

    // mousedown / mouseup
    let before_mouse_down = read_interactive_state(ctx)["mouseDownCount"]
        .as_u64()
        .unwrap_or(0);
    run_command(ctx, &["mousedown", "left"]);
    wait_for_state(
        ctx,
        |s| s["mouseDownCount"].as_u64() == Some(before_mouse_down + 1),
        15_000,
    );

    let before_mouse_up = read_interactive_state(ctx)["mouseUpCount"]
        .as_u64()
        .unwrap_or(0);
    run_command(ctx, &["mouseup", "left"]);
    wait_for_state(
        ctx,
        |s| s["mouseUpCount"].as_u64() == Some(before_mouse_up + 1),
        15_000,
    );

    // mousewheel
    run_command(ctx, &["mousewheel", "0", "160"]);
    let wheel_state = wait_for_state(
        ctx,
        |s| s["lastWheel"][0].as_i64() == Some(160) && s["lastWheel"][1].as_i64() == Some(0),
        15_000,
    );
    assert!(
        wheel_state["lastWheel"][0].as_i64() == Some(160)
            && wheel_state["lastWheel"][1].as_i64() == Some(0),
        "Expected lastWheel to equal [160, 0], got {wheel_state:#?}"
    );

    // dialog-accept (prompt): schedule a JS click that will trigger window.prompt,
    // wait a moment for the dialog to appear, then accept it.
    // This mirrors the Node.js test pattern of using a deferred click + sleep.
    eval_text(
        ctx,
        "(() => { setTimeout(() => document.getElementById('prompt-target').click(), 100); return 'scheduled'; })()",
    );
    thread::sleep(Duration::from_millis(500));
    run_command(ctx, &["dialog-accept", "accepted by cli"]);
    wait_for_state(
        ctx,
        |s| s["promptResult"].as_str() == Some("accepted by cli"),
        15_000,
    );

    // dialog-dismiss (confirm): same deferred-click pattern for window.confirm.
    eval_text(
        ctx,
        "(() => { setTimeout(() => document.getElementById('confirm-target').click(), 100); return 'scheduled'; })()",
    );
    thread::sleep(Duration::from_millis(500));
    run_command(ctx, &["dialog-dismiss"]);
    wait_for_state(
        ctx,
        |s| s["confirmResult"].as_str() == Some("dismissed"),
        15_000,
    );

    run_command(ctx, &["close"]);
}

fn test_tab_commands(ctx: &mut E2ECtx) {
    reset_cli_artifacts(ctx);

    run_command(ctx, &["open"]);
    let interactive_url = ctx.interactive_url();
    let other_url = ctx.other_url();
    run_command(ctx, &["goto", &interactive_url]);

    // tab-list – should contain the interactive URL
    let initial_tabs = run_command(ctx, &["tab-list"]);
    let tab_output = strip_snapshot_output(&initial_tabs.stdout);
    assert!(
        tab_output.contains(&interactive_url),
        "Expected interactive URL in tab-list output:\n{tab_output}"
    );

    // tab-new
    run_command(ctx, &["tab-new", &other_url]);
    let updated_tabs = run_command(ctx, &["tab-list"]);
    let tab_output = strip_snapshot_output(&updated_tabs.stdout);
    assert!(
        tab_output.contains(&interactive_url),
        "Expected interactive URL in updated tab-list"
    );
    assert!(
        tab_output.contains(&other_url),
        "Expected other URL in updated tab-list"
    );

    // Extract the tab ID for the other tab so we can select and close it.
    let other_tab_id = extract_tab_id(&tab_output, &other_url);

    // tab-select
    run_command(ctx, &["tab-select", &other_tab_id]);

    // tab-close
    run_command(ctx, &["tab-close", &other_tab_id]);
}

// ---------------------------------------------------------------------------
// Command-coverage helpers
// ---------------------------------------------------------------------------

/// Commands that require an LLM/agent backend, destructive global cleanup, or
/// multi-browser contexts and therefore cannot be exercised in the browser-
/// backed e2e suite. Each entry has a brief justification.
///
/// This set is validated by [`test_e2e_command_coverage`]: if a new command is
/// added to `commands.rs` without appearing here *or* in the tested set, the
/// build will fail.
fn excluded_commands() -> HashSet<&'static str> {
    [
        // LLM / agent backend required
        "extract",
        "summarize",
        "agent-run",
        "agent-status",
        "agent-result",
        // Destructive across concurrent sessions — would make the suite flaky
        "close-all",
        "kill-all",
        // Collective (co-*) commands require a multi-context backend
        "co-create",
        "co-submit",
        "co-scrape",
        "co-status",
        "co-result",
    ]
    .into()
}

/// The set of commands that the e2e scenario functions exercise via
/// [`run_command`] / [`run_command_expecting_failure`].  This must be kept in
/// sync with what the test functions actually call.
fn tested_commands() -> HashSet<&'static str> {
    [
        // test_session_and_navigation
        "open",
        "list",
        "goto",
        "go-back",
        "go-forward",
        "reload",
        "delete-data",
        "close",
        // test_interaction_console_and_export
        "resize",
        "type",
        "fill",
        "press",
        "keydown",
        "keyup",
        "click",
        "dblclick",
        "hover",
        "drag",
        "select",
        "check",
        "uncheck",
        "upload",
        "console",
        "snapshot",
        "screenshot",
        "pdf",
        // test_mouse_and_dialog
        "mousemove",
        "mousedown",
        "mouseup",
        "mousewheel",
        "dialog-accept",
        "dialog-dismiss",
        // test_tab_commands
        "tab-list",
        "tab-new",
        "tab-select",
        "tab-close",
        // eval is exercised indirectly by the eval_text helper
        "eval",
    ]
    .into()
}

// ---------------------------------------------------------------------------
// Entry point — custom sequential harness
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// Coverage assertion — runs without a server
// ---------------------------------------------------------------------------

/// Verify that `tested_commands() ∪ excluded_commands()` equals the full
/// command list from [`browser4_cli::commands::all_commands`].
///
/// This check does **not** require a running server. It is a test-time
/// guard: if a command is added to `commands.rs` without being placed into
/// either [`tested_commands`] or [`excluded_commands`], this test fails.
fn verify_e2e_command_coverage() {
    let all: HashSet<&str> = all_commands().iter().map(|c| c.name).collect();

    let tested = tested_commands();
    let excluded = excluded_commands();

    // 1. No command should appear in both sets.
    let overlap: Vec<&&str> = tested.intersection(&excluded).collect();
    assert!(
        overlap.is_empty(),
        "Commands appear in BOTH tested and excluded sets: {overlap:?}"
    );

    // 2. Every command from commands.rs must be in one of the two sets.
    let accounted: HashSet<&str> = tested.union(&excluded).copied().collect();
    let mut missing: Vec<&str> = all
        .iter()
        .copied()
        .filter(|cmd| !accounted.contains(cmd))
        .collect();
    missing.sort();
    assert!(
        missing.is_empty(),
        "Commands defined in commands.rs are not accounted for in e2e tests \
         (add them to `tested_commands` or `excluded_commands`): {missing:?}"
    );

    // 3. No stale entries: every name in the two sets must exist in commands.rs.
    let mut stale: Vec<&str> = accounted
        .iter()
        .copied()
        .filter(|cmd| !all.contains(cmd))
        .collect();
    stale.sort();
    assert!(
        stale.is_empty(),
        "Stale command names in e2e test sets that no longer exist in commands.rs: {stale:?}"
    );
}

fn run_named_test(name: &str, test_fn: fn()) {
    print!("test {name} ... ");
    std::io::stdout().flush().expect("stdout flush failed");
    test_fn();
    println!("ok");
}

fn run_named_scenario(name: &str, resources: &mut E2ETestResources, test_fn: fn(&mut E2ECtx)) {
    print!("test {name} ... ");
    std::io::stdout().flush().expect("stdout flush failed");
    resources.restart_browser4();
    test_fn(&mut resources.ctx);
    println!("ok");
}

fn main() {
    let total_tests = 5;
    println!("running {total_tests} tests");

    run_named_test("test_e2e_command_coverage", verify_e2e_command_coverage);

    let mut resources = create_e2e_test_resources();
    run_named_scenario(
        "test_e2e_session_and_navigation",
        &mut resources,
        test_session_and_navigation,
    );
    run_named_scenario(
        "test_e2e_interaction_console_and_export",
        &mut resources,
        test_interaction_console_and_export,
    );
    run_named_scenario(
        "test_e2e_mouse_and_dialog",
        &mut resources,
        test_mouse_and_dialog,
    );
    run_named_scenario("test_e2e_tab_commands", &mut resources, test_tab_commands);

    println!(
        "test result: ok. {} passed; 0 failed; 0 ignored; 0 measured; 0 filtered out",
        total_tests
    );
}
