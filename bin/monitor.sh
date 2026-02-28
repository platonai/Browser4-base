#!/usr/bin/env bash
# monitor.sh — Monitor GitHub issues and a specified URL, dispatch tasks to coworker workflow.
#
# Usage:
#   ./bin/monitor.sh [options]
#
# Options:
#   --repo      GitHub repository in "owner/repo" format  (default: platonai/Browser4)
#   --assignee  GitHub username to filter issues by       (default: galaxyeye)
#   --url       URL to poll every minute                  (default: "")
#   --keyword   Keyword to search for in URL response     (default: @galaxyeye)
#   --interval  Polling interval in seconds               (default: 60)
#   --once      Run once and exit (no loop)
#
# Examples:
#   ./bin/monitor.sh --repo platonai/Browser4 --url https://example.com/tasks
#   ./bin/monitor.sh --once

set -euo pipefail

# ── resolve repo root ──────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" >/dev/null 2>&1; pwd)"
REPO_ROOT="$SCRIPT_DIR"
while [[ ! -f "$REPO_ROOT/VERSION" && "$REPO_ROOT" != "/" ]]; do
  REPO_ROOT="$(dirname "$REPO_ROOT")"
done
[[ -f "$REPO_ROOT/VERSION" ]] || { echo "ERROR: cannot find repo root"; exit 1; }

# ── defaults ───────────────────────────────────────────────────────────────────
GH_REPO="platonai/Browser4"
ASSIGNEE="galaxyeye"
POLL_URL=""
KEYWORD="@galaxyeye"
INTERVAL=60
RUN_ONCE=false
TASKS_DIR="$REPO_ROOT/coworker/tasks/2working"

# ── parse arguments ────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    --repo)      GH_REPO="$2";   shift 2 ;;
    --assignee)  ASSIGNEE="$2";  shift 2 ;;
    --url)       POLL_URL="$2";  shift 2 ;;
    --keyword)   KEYWORD="$2";   shift 2 ;;
    --interval)  INTERVAL="$2";  shift 2 ;;
    --once)      RUN_ONCE=true;  shift   ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

mkdir -p "$TASKS_DIR"

# ── helpers ────────────────────────────────────────────────────────────────────
timestamp() { date +%s%3N; }   # millisecond-precision unix timestamp (pure digits)

# Save content to a timestamped file and move it into the working queue.
dispatch_task() {
  local content="$1"
  local ts; ts="$(timestamp)"
  local tmp_file; tmp_file="$(mktemp)"
  printf '%s\n' "$content" > "$tmp_file"
  mv "$tmp_file" "$TASKS_DIR/$ts"
  echo "[$(date -Iseconds)] Dispatched task file: $TASKS_DIR/$ts"
}

# ── GitHub issues monitor ──────────────────────────────────────────────────────
# Requires: gh CLI (https://cli.github.com/) authenticated, OR curl + GITHUB_TOKEN env var.
fetch_github_issues() {
  echo "[$(date -Iseconds)] Fetching latest issues from $GH_REPO assigned to $ASSIGNEE ..."

  if command -v gh &>/dev/null; then
    # Use gh CLI
    gh issue list \
      --repo "$GH_REPO" \
      --assignee "$ASSIGNEE" \
      --limit 20 \
      --json number,title,body,url,createdAt,state \
      2>/dev/null
  elif [[ -n "${GITHUB_TOKEN:-}" ]]; then
    # Fall back to curl + REST API
    curl -fsSL \
      -H "Authorization: Bearer $GITHUB_TOKEN" \
      -H "Accept: application/vnd.github+json" \
      "https://api.github.com/repos/$GH_REPO/issues?assignee=$ASSIGNEE&per_page=20&state=open"
  else
    echo "WARNING: Neither 'gh' CLI nor GITHUB_TOKEN is available; skipping GitHub monitor." >&2
    return
  fi | while IFS= read -r line; do
    # Accumulate JSON output
    printf '%s\n' "$line"
  done | python3 -c "
import sys, json
data = json.load(sys.stdin)
if not isinstance(data, list):
    data = [data]
for issue in data:
    print(json.dumps(issue, ensure_ascii=False))
" 2>/dev/null | while IFS= read -r issue_json; do
    # Each line is one issue JSON object
    dispatch_task "$issue_json"
  done
}

# ── URL monitor ────────────────────────────────────────────────────────────────
fetch_url() {
  [[ -z "$POLL_URL" ]] && return
  echo "[$(date -Iseconds)] Polling URL: $POLL_URL ..."
  local response
  response="$(curl -fsSL --max-time 30 "$POLL_URL" 2>/dev/null)" || {
    echo "WARNING: Failed to fetch $POLL_URL" >&2
    return
  }
  if echo "$response" | grep -qF "$KEYWORD"; then
    echo "[$(date -Iseconds)] Keyword '$KEYWORD' found in URL response."
    dispatch_task "$response"
  else
    echo "[$(date -Iseconds)] Keyword '$KEYWORD' not found; nothing to dispatch."
  fi
}

# ── main loop ──────────────────────────────────────────────────────────────────
echo "[$(date -Iseconds)] Monitor started. repo=$GH_REPO assignee=$ASSIGNEE url=${POLL_URL:-<none>} keyword=$KEYWORD interval=${INTERVAL}s"

while true; do
  fetch_github_issues
  fetch_url
  "$RUN_ONCE" && break
  echo "[$(date -Iseconds)] Sleeping ${INTERVAL}s ..."
  sleep "$INTERVAL"
done
