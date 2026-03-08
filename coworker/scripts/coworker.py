#!/usr/bin/env python3

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
from typing import Iterable


COPILOT_NAME_TIMEOUT_SECONDS = 60
COPILOT_RUN_TIMEOUT_SECONDS = 6000
STRUCTURED_TASK_PATTERN = re.compile(
    r"(?ms)^Title:\s*(?P<title>.*?)(?:\r\n|\n)"
    r"Description:\s*(?P<desc>.*?)(?:\r\n|\n)"
    r"Prompt:\s*(?P<prompt>.*)$"
)


@dataclass(frozen=True)
class TaskRoot:
    prepare: Path
    created: Path
    working: Path
    finished: Path
    review: Path
    approved: Path
    pushed: Path
    logs: Path
    label: str


@dataclass(frozen=True)
class CopilotCommand:
    repo_root: Path
    executable: str
    base_args: list[str]
    config_path: Path | None


@dataclass(frozen=True)
class MemoryPayload:
    context: str
    instructions: str


class Logger:
    def __init__(self, log_path: Path) -> None:
        self.log_path = log_path
        self.log_path.parent.mkdir(parents=True, exist_ok=True)

    def message(self, message: str, level: str = "INFO") -> None:
        timestamp = utc_now().strftime("%Y-%m-%d %H:%M:%S")
        entry = f"[{timestamp}] [{level}] {message}"
        if level == "ERROR":
            print(entry, file=sys.stderr)
        else:
            print(entry)
        with self.log_path.open("a", encoding="utf-8", newline="\n") as handle:
            handle.write(entry + "\n")

    def verbose(self, message: str) -> None:
        timestamp = utc_now().strftime("%Y-%m-%d %H:%M:%S")
        entry = f"[{timestamp}] [DEBUG] {message}"
        with self.log_path.open("a", encoding="utf-8", newline="\n") as handle:
            handle.write(entry + "\n")


def utc_now() -> datetime:
    return datetime.now(timezone.utc)


def git_repo_root(cwd: Path) -> Path | None:
    result = subprocess.run(
        ["git", "rev-parse", "--show-toplevel"],
        cwd=str(cwd),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
    )
    if result.returncode != 0:
        return None
    output = result.stdout.strip()
    return Path(output).resolve() if output else None


def find_repo_root(start: Path) -> Path:
    git_root = git_repo_root(start)
    if git_root is not None:
        return git_root

    current = start.resolve()
    for candidate in (current, *current.parents):
        if (candidate / "ROOT.md").exists():
            return candidate
    raise RuntimeError("Repo root not found.")


def parse_powershell_copilot_config(config_path: Path) -> list[str]:
    if not config_path.exists():
        return []
    content = config_path.read_text(encoding="utf-8")
    match = re.search(r"\$COPILOT\s*=\s*@\((?P<body>.*?)\)", content, re.DOTALL)
    if not match:
        return []
    body = match.group("body")
    tokens: list[str] = []
    for line in body.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        token_match = re.match(r"""['"](?P<token>.*)['"]""", stripped)
        if token_match:
            tokens.append(token_match.group("token"))
    return tokens


def parse_bash_copilot_config(config_path: Path) -> list[str]:
    if not config_path.exists():
        return []
    content = config_path.read_text(encoding="utf-8")
    match = re.search(r"COPILOT=\((?P<body>.*?)\)", content, re.DOTALL)
    if not match:
        return []
    body = match.group("body")
    tokens: list[str] = []
    for line in body.splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        tokens.append(stripped.strip("'\""))
    return tokens


def load_copilot_command(repo_root: Path) -> CopilotCommand:
    config_ps1 = repo_root / "coworker" / "scripts" / "config.ps1"
    config_sh = repo_root / "coworker" / "scripts" / "config.sh"
    tokens = parse_powershell_copilot_config(config_ps1)
    config_path: Path | None = config_ps1 if tokens else None
    if not tokens:
        tokens = parse_bash_copilot_config(config_sh)
        if tokens:
            config_path = config_sh
    if not tokens:
        tokens = ["gh", "copilot"]
        config_path = None
    if len(tokens) < 2:
        config_label = str(config_path) if config_path else "default config"
        raise RuntimeError(f"COPILOT must include an executable and at least one argument in {config_label}")
    return CopilotCommand(
        repo_root=repo_root,
        executable=tokens[0],
        base_args=tokens[1:],
        config_path=config_path,
    )


def format_command(executable: str, arguments: Iterable[str]) -> str:
    if os.name == "nt":
        return subprocess.list2cmdline([executable, *arguments])
    return " ".join(shlex_quote(argument) for argument in [executable, *arguments])


def shlex_quote(value: str) -> str:
    if not value:
        return "''"
    if re.fullmatch(r"[A-Za-z0-9_@%+=:,./-]+", value):
        return value
    return "'" + value.replace("'", "'\"'\"'") + "'"


def build_copilot_arguments(command: CopilotCommand, prompt: str, additional_arguments: Iterable[str] = ()) -> list[str]:
    arguments = list(command.base_args)
    arguments.extend(["--", "-p", prompt])
    arguments.extend(additional_arguments)
    return arguments


def ensure_directories(task_root: TaskRoot) -> None:
    for path in (
        task_root.prepare,
        task_root.created,
        task_root.working,
        task_root.finished,
        task_root.review,
        task_root.approved,
        task_root.pushed,
        task_root.logs,
    ):
        path.mkdir(parents=True, exist_ok=True)


def ensure_draft_placeholders(draft_directory: Path, logger: Logger) -> None:
    for draft_number in range(1, 6):
        draft_path = draft_directory / f"{draft_number}.md"
        if not draft_path.exists():
            with draft_path.open("w", encoding="utf-8", newline="\n"):
                pass
            logger.message(f"Created missing draft placeholder: {draft_path}")


def resolve_unique_path(directory: Path, base_name: str, extension: str) -> tuple[Path, str]:
    candidate_name = f"{base_name}{extension}"
    candidate_path = directory / candidate_name
    if not candidate_path.exists():
        return candidate_path, candidate_name

    counter = 2
    while True:
        next_name = f"{base_name}.{counter}{extension}"
        next_path = directory / next_name
        if not next_path.exists():
            return next_path, next_name
        counter += 1


def normalize_task_name(raw_name: str, fallback: str) -> str:
    normalized = re.sub(r"\s+", "-", raw_name.strip())
    normalized = re.sub(r"[^A-Za-z0-9._-]", "-", normalized)
    normalized = re.sub(r"-+", "-", normalized)
    normalized = normalized.strip(" .-_")
    normalized = normalized[:60].strip(" .-_")
    return normalized or fallback


def build_naming_prompt(title: str, description: str, prompt: str) -> str:
    prompt_sample = prompt[:600]
    return (
        "Create a short, descriptive task name in kebab-case (3-6 words max). Output only the name.\n"
        f"Title: {title}\n"
        f"Description: {description}\n"
        f"Prompt: {prompt_sample}\n"
    )


def generate_task_basename(
    title: str,
    description: str,
    prompt: str,
    fallback: str,
    command: CopilotCommand,
    logger: Logger,
) -> str:
    naming_prompt = build_naming_prompt(title, description, prompt)
    arguments = build_copilot_arguments(command, naming_prompt)
    logger.verbose(f"Executing GH Copilot for naming: {format_command(command.executable, arguments)}")

    try:
        result = subprocess.run(
            [command.executable, *arguments],
            cwd=str(command.repo_root),
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            timeout=COPILOT_NAME_TIMEOUT_SECONDS,
            check=False,
        )
    except subprocess.TimeoutExpired:
        logger.message(f"GH Copilot naming timed out after {COPILOT_NAME_TIMEOUT_SECONDS}s", "WARN")
        return fallback
    except OSError as error:
        logger.verbose(f"Naming Copilot failed to start: {error}")
        return fallback

    raw_name = ""
    for line in result.stdout.splitlines():
        if line.strip():
            raw_name = line.strip()
            break

    if result.stderr.strip():
        logger.verbose(f"Naming Copilot STDERR: {result.stderr.strip()}")
    if result.returncode != 0:
        logger.verbose(f"Naming Copilot exited with code {result.returncode}")
        return fallback
    if not raw_name:
        logger.verbose("Naming Copilot returned empty name")
        return fallback
    logger.verbose(f"Naming Copilot STDOUT: {raw_name}")
    return normalize_task_name(raw_name, fallback)


def parse_structured_content(content: str, fallback_title: str, fallback_description: str) -> tuple[str, str, str]:
    match = STRUCTURED_TASK_PATTERN.match(content)
    if match:
        return match.group("title").strip(), match.group("desc").strip(), match.group("prompt").strip()
    return fallback_title, fallback_description, content


def current_date_parts() -> tuple[str, str, str, str, str]:
    now = utc_now()
    return (
        now.strftime("%Y"),
        now.strftime("%m"),
        now.strftime("%d"),
        now.strftime("%m%d"),
        now.strftime("%H%M%S"),
    )


def select_helper_command(script_path: Path) -> list[str]:
    if script_path.suffix == ".ps1":
        return [resolve_powershell_executable(), "-File", str(script_path)]
    if script_path.suffix == ".sh":
        return ["bash", str(script_path)]
    raise RuntimeError(f"Unsupported helper script: {script_path}")


def resolve_powershell_executable() -> str:
    for candidate in ("pwsh", "powershell"):
        if shutil.which(candidate):
            return candidate
    raise RuntimeError("PowerShell executable not found.")


def helper_script(repo_root: Path, name: str) -> Path:
    workers_dir = repo_root / "coworker" / "scripts" / "workers"
    suffix = ".ps1" if os.name == "nt" else ".sh"
    script_path = workers_dir / f"{name}{suffix}"
    if script_path.exists():
        return script_path
    fallback = workers_dir / f"{name}.ps1"
    if fallback.exists():
        return fallback
    fallback = workers_dir / f"{name}.sh"
    if fallback.exists():
        return fallback
    raise RuntimeError(f"Helper script not found for {name}")


def initialize_memory_payload(repo_root: Path, date_text: str, logger: Logger) -> MemoryPayload:
    script_path = helper_script(repo_root, "coworker-memory-generator")
    command = select_helper_command(script_path)
    if script_path.suffix == ".ps1":
        command.extend(["-Type", "init", "-Date", date_text])
    else:
        command.extend(["init", date_text])
    try:
        result = subprocess.run(
            command,
            cwd=str(repo_root),
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
    except OSError as error:
        logger.message(f"Failed to initialize memory context: {error}", "ERROR")
        return MemoryPayload(context="", instructions="")

    if result.returncode != 0:
        stderr = result.stderr.strip() or result.stdout.strip()
        logger.message(f"Failed to initialize memory context: {stderr}", "ERROR")
        return MemoryPayload(context="", instructions="")

    output = result.stdout.strip()
    if not output:
        logger.message("Memory generator returned empty result.", "WARN")
        return MemoryPayload(context="", instructions="")

    try:
        payload = json.loads(output)
    except json.JSONDecodeError as error:
        logger.message(f"Failed to parse memory context: {error}", "ERROR")
        return MemoryPayload(context="", instructions="")

    logger.message("Memory context initialized via generator.")
    return MemoryPayload(
        context=payload.get("context", "") or "",
        instructions=payload.get("instructions", "") or "",
    )


def move_task_file_to_created(task_file: Path, created_dir: Path) -> Path:
    destination = created_dir / task_file.name
    if destination.exists():
        destination.unlink()
    shutil.move(str(task_file), str(destination))
    print(f"Moved specified task file to: {destination}")
    return destination


def list_files(directory: Path) -> list[Path]:
    if not directory.exists():
        return []
    return sorted((path for path in directory.iterdir() if path.is_file()), key=lambda path: path.name.lower())


def list_recursive_files(directory: Path) -> list[Path]:
    if not directory.exists():
        return []
    return sorted((path for path in directory.rglob("*") if path.is_file()), key=lambda path: str(path).lower())


def run_git_sync(repo_root: Path, logger: Logger) -> None:
    script_path = helper_script(repo_root, "git-sync")
    command = select_helper_command(script_path)
    logger.message("Executing commit script for approved tasks...")
    result = subprocess.run(command, cwd=str(repo_root), check=False)
    if result.returncode == 0:
        logger.message("Git sync executed successfully.")
    else:
        logger.message(f"Git sync failed with exit code {result.returncode}.", "ERROR")


def stream_copilot_execution(
    command: CopilotCommand,
    prompt: str,
    stdout_path: Path,
    stderr_path: Path,
    logger: Logger,
) -> int:
    arguments = build_copilot_arguments(command, prompt, ["--allow-all-tools", "--allow-all-paths"])
    logger.message("=== Starting Copilot execution ===")

    with stdout_path.open("w", encoding="utf-8", newline="\n") as stdout_handle, stderr_path.open(
        "w", encoding="utf-8", newline="\n"
    ) as stderr_handle:
        process = subprocess.Popen(
            [command.executable, *arguments],
            cwd=str(command.repo_root),
            stdout=stdout_handle,
            stderr=stderr_handle,
            text=True,
            encoding="utf-8",
            errors="replace",
        )

    last_output_position = 0
    start_time = time.monotonic()
    while True:
        time.sleep(0.5)
        if stdout_path.exists():
            with stdout_path.open("r", encoding="utf-8", errors="replace") as handle:
                handle.seek(last_output_position)
                new_output = handle.read()
                last_output_position = handle.tell()
            for line in new_output.splitlines():
                if line.strip():
                    print(line)

        exit_code = process.poll()
        if exit_code is not None:
            break

        if time.monotonic() - start_time > COPILOT_RUN_TIMEOUT_SECONDS:
            process.kill()
            logger.message(f"Copilot timed out after {COPILOT_RUN_TIMEOUT_SECONDS}s", "WARN")
            print(f"[TIMEOUT] Copilot execution exceeded {COPILOT_RUN_TIMEOUT_SECONDS}s timeout")
            process.wait()
            break

    if stdout_path.exists():
        with stdout_path.open("r", encoding="utf-8", errors="replace") as handle:
            handle.seek(last_output_position)
            remaining_output = handle.read()
        for line in remaining_output.splitlines():
            if line.strip():
                print(line)

    if stderr_path.exists():
        stderr_content = stderr_path.read_text(encoding="utf-8", errors="replace")
        if stderr_content.strip():
            print("\n[STDERR OUTPUT]")
            for line in stderr_content.splitlines():
                if line.strip():
                    print(line)

    return process.returncode if process.returncode is not None else -1


def append_combined_copilot_log(copilot_log_path: Path, stdout_path: Path, stderr_path: Path) -> None:
    with copilot_log_path.open("w", encoding="utf-8", newline="\n") as handle:
        if stdout_path.exists():
            handle.write(stdout_path.read_text(encoding="utf-8", errors="replace"))
        if stderr_path.exists():
            stderr_content = stderr_path.read_text(encoding="utf-8", errors="replace")
            if stderr_content:
                handle.write("\n=== COPILOT STDERR ===\n")
                handle.write(stderr_content)


def safe_title(base_name: str) -> str:
    normalized = re.sub(r'[\\/*?:"<>|]', "_", base_name)
    return normalized or "task"


def process_created_task(
    file_path: Path,
    repo_root: Path,
    task_root: TaskRoot,
    command: CopilotCommand,
    logger: Logger,
    current_year: str,
    current_month: str,
    current_day: str,
    current_date: str,
    current_time: str,
) -> None:
    rename_attempts = 3
    original_content = file_path.read_text(encoding="utf-8")
    file_safe_title = safe_title(file_path.stem)
    descriptive_name = ""

    logger.verbose(f"renameScript path: {repo_root / 'coworker' / 'scripts' / 'workers' / 'rename.ps1'}")
    logger.verbose(f"Test-Path renameScript: {(repo_root / 'coworker' / 'scripts' / 'workers' / 'rename.ps1').exists()}")

    title_for_name, description_for_name, prompt_for_name = parse_structured_content(
        original_content,
        file_safe_title,
        f"Task from {file_path.name}",
    )

    for attempt in range(1, rename_attempts + 1):
        descriptive_name = generate_task_basename(
            title_for_name,
            description_for_name,
            prompt_for_name,
            file_safe_title,
            command,
            logger,
        )
        if descriptive_name and "Error" not in descriptive_name and "Timeout" not in descriptive_name:
            break
        descriptive_name = ""
        if attempt < rename_attempts:
            time.sleep(2)

    if not descriptive_name:
        logger.message(f"Renaming failed after {rename_attempts} attempts. Using fallback safe title.", "WARN")
        descriptive_name = file_safe_title

    current_file = file_path
    if descriptive_name != current_file.stem:
        renamed_path = task_root.created / f"{descriptive_name}{current_file.suffix}"
        if renamed_path.exists():
            counter = 2
            while (task_root.created / f"{descriptive_name}.{counter}{current_file.suffix}").exists():
                counter += 1
            renamed_path = task_root.created / f"{descriptive_name}.{counter}{current_file.suffix}"
            descriptive_name = f"{descriptive_name}.{counter}"
        shutil.move(str(current_file), str(renamed_path))
        logger.message(f"Renamed in created: {current_file.name} -> {renamed_path.name}")
        current_file = renamed_path

    working_path, working_file_name = resolve_unique_path(task_root.working, current_file.stem, current_file.suffix)
    shutil.move(str(current_file), str(working_path))
    logger.message(f"Moved to working: {working_path}")

    working_base_name = Path(working_file_name).stem
    title = descriptive_name
    description = f"Task from {current_file.name}"
    prompt = (
        f"Finish the task described in file: {working_path}.\n"
        "Do not move **this** task file, just execute the task based on its content, the system will move it after you finished the task.\n"
    )

    if STRUCTURED_TASK_PATTERN.match(original_content):
        title, description, prompt = parse_structured_content(original_content, title, description)

    memory_payload = initialize_memory_payload(repo_root, f"{current_year}-{current_month}-{current_day}", logger)
    prompt = f"{prompt}\n\n{memory_payload.instructions}\n\n{memory_payload.context}"

    logs_sub_dir = task_root.logs / current_year / current_month / current_day
    logs_sub_dir.mkdir(parents=True, exist_ok=True)
    task_log_path = logs_sub_dir / f"{current_time}-{working_base_name}.task.log"
    copilot_log_path = logs_sub_dir / f"{current_time}-{working_base_name}.copilot.log"
    stdout_log = Path(str(copilot_log_path) + ".stdout")
    stderr_log = Path(str(copilot_log_path) + ".stderr")

    logger.verbose(f"Task log will be written to: {task_log_path}")
    logger.message(f"Executing Copilot for task: {working_base_name}")
    logger.verbose(f"Prompt length: {len(prompt)} characters")

    task_log_path.write_text(
        "\n".join(
            [
                f"Task: {title}",
                f"Description: {description}",
                f"Original File: {current_file.name}",
                f"Started: {utc_now().strftime('%Y-%m-%d %H:%M:%S')}",
                "Prompt:",
                prompt,
                "---",
                "Copilot Execution Output:",
                "",
            ]
        ),
        encoding="utf-8",
    )

    exit_code = -1
    try:
        exit_code = stream_copilot_execution(command, prompt, stdout_log, stderr_log, logger)
        append_combined_copilot_log(copilot_log_path, stdout_log, stderr_log)
        logger.message(f"Copilot execution finished with exit code {exit_code}")
        logger.message("=== Copilot execution completed ===")
        logger.verbose(f"Copilot external tool log: {copilot_log_path}")
        with task_log_path.open("a", encoding="utf-8", newline="\n") as handle:
            handle.write(f"\nCopilot Exit Code: {exit_code}\nCopilot Log: {copilot_log_path}\n")
        if exit_code != 0:
            logger.message(f"Warning: Copilot exited with non-zero code. Check log: {copilot_log_path}", "WARN")
    except OSError as error:
        logger.message(f"Failed to execute copilot: {error}", "ERROR")
        with task_log_path.open("a", encoding="utf-8", newline="\n") as handle:
            handle.write(f"Error executing copilot: {error}\n")
    finally:
        for temp_path in (stdout_log, stderr_log):
            if temp_path.exists():
                temp_path.unlink()

    target_dir = task_root.finished
    target_message = "Task moved to finished"
    if "#auto-approve" in original_content:
        target_dir = task_root.approved
        target_message = "Task AUTO-APPROVED and moved to"

    target_sub_dir = target_dir / current_year / current_date
    target_sub_dir.mkdir(parents=True, exist_ok=True)
    target_path, _ = resolve_unique_path(target_sub_dir, working_base_name, current_file.suffix)
    shutil.move(str(working_path), str(target_path))
    logger.message(f"{target_message} : {target_path}")
    ensure_draft_placeholders(task_root.prepare, logger)
    logger.message("---")


def run(task_file_argument: str | None) -> int:
    script_path = Path(__file__).resolve()
    input_task_path = Path(task_file_argument).resolve() if task_file_argument else None
    repo_root = find_repo_root(script_path.parent)
    os.chdir(repo_root)

    tasks_root = repo_root / "coworker" / "tasks"
    task_root = TaskRoot(
        prepare=tasks_root / "0draft",
        created=tasks_root / "1created",
        working=tasks_root / "2working",
        finished=tasks_root / "3_1complete",
        review=tasks_root / "4review",
        approved=tasks_root / "5approved",
        pushed=tasks_root / "6git-pushed",
        logs=tasks_root / "300logs",
        label="tasks",
    )
    ensure_directories(task_root)

    if input_task_path is not None:
        if not input_task_path.exists():
            print(f"Specified task file not found: {input_task_path}", file=sys.stderr)
            return 1
        move_task_file_to_created(input_task_path, task_root.created)

    current_year, current_month, current_day, current_date, current_time = current_date_parts()
    logs_sub_dir = task_root.logs / current_year / current_month / current_day
    logs_sub_dir.mkdir(parents=True, exist_ok=True)
    script_log_path = logs_sub_dir / f"{current_time}-coworker.log"
    logger = Logger(script_log_path)
    script_start_time = utc_now()

    command = load_copilot_command(repo_root)

    logger.message("===========================================================================")
    logger.message("Coworker Task Runner - Python Version")
    logger.message(f"Started at: {script_start_time}")
    logger.message(f"Script Log: {script_log_path}")
    logger.message("===========================================================================")

    ensure_draft_placeholders(task_root.prepare, logger)

    for file_path in list_files(task_root.prepare):
        logger.message(f"[PREPARE] Task: {file_path.name}")

    recent_cutoff = utc_now() - timedelta(days=1)
    for file_path in list_recursive_files(task_root.finished):
        modified_at = datetime.fromtimestamp(file_path.stat().st_mtime, tz=timezone.utc)
        if modified_at >= recent_cutoff:
            logger.message(f"[COMPLETE] Task waiting for review: {file_path.name}")

    for file_path in list_files(task_root.review):
        logger.message(f"[REVIEW] Task: {file_path.name}")

    approved_files = list_recursive_files(task_root.approved)
    if approved_files:
        pushed_sub_dir = task_root.pushed / current_year / current_date
        pushed_sub_dir.mkdir(parents=True, exist_ok=True)
        for file_path in approved_files:
            print(f"Moving approved task to pushed: {file_path}")
            pushed_path, _ = resolve_unique_path(pushed_sub_dir, file_path.stem, file_path.suffix)
            shutil.move(str(file_path), str(pushed_path))
            logger.message(f"Task moved to pushed: {pushed_path}")
        run_git_sync(repo_root, logger)

    pushed_cutoff = utc_now() - timedelta(days=2)
    for file_path in list_recursive_files(task_root.pushed):
        modified_at = datetime.fromtimestamp(file_path.stat().st_mtime, tz=timezone.utc)
        if modified_at >= pushed_cutoff:
            logger.message(f"[PUSHED] Task: {file_path.name} (updated {datetime.fromtimestamp(file_path.stat().st_mtime).strftime('%Y-%m-%d %H:%M:%S')})")

    for file_path in list_files(task_root.created):
        process_created_task(
            file_path=file_path,
            repo_root=repo_root,
            task_root=task_root,
            command=command,
            logger=logger,
            current_year=current_year,
            current_month=current_month,
            current_day=current_day,
            current_date=current_date,
            current_time=current_time,
        )

    script_end_time = utc_now()
    logger.message("===========================================================================")
    logger.message("All tasks completed")
    logger.message(f"Ended at: {script_end_time}")
    logger.message(f"Script Log: {script_log_path}")
    logger.message("===========================================================================")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Coworker task runner implemented in Python.")
    parser.add_argument("task_file", nargs="?", help="Optional task file to move into coworker/tasks/1created before processing.")
    return parser


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    try:
        return run(args.task_file)
    except RuntimeError as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
