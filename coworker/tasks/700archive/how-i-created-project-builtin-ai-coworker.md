# How I created a project builtin AI coworker

## 1. Motivation

Managing a complex, high-performance project like **Browser4** (a coroutine-safe browser engine for AI agents) presents unique challenges. With modules ranging from core browser control to AI agent implementations, the cognitive load of tracking tasks, maintaining documentation, and ensuring architectural consistency is immense.

I often found myself context-switching between coding, updating documentation, and managing task lists. External project management tools felt disconnected from the codebase, creating friction. I needed a solution that lived *where the work happens*—inside the repository.

The goal was to create an **AI Coworker**: a built-in assistant that understands the project structure, maintains its own memory of past decisions, and can autonomously execute tasks, all while adhering to the project's strict coding and testing guidelines.

## 2. Design

The AI Coworker is designed to be an integral part of the repository, not an external service. Its architecture focuses on three pillars: **Persistence, Autonomy, and Integration**.

### Architecture
- **Repository-Native**: The coworker lives in the `coworker/` directory. Its "brain" (scripts) and "memory" (logs) are version-controlled alongside the code it works on.
- **Hierarchical Memory**: To handle long-term context, I designed a memory system inspired by human recall:
  - **Daily Memory**: Captures granular tasks, successes, and failures (e.g., `MEMORY.20260228.md`).
  - **Monthly/Yearly Summaries**: Aggregates daily logs into high-level themes and strategic reviews.
- **Task Pipeline**: Tasks are managed as Markdown files in `coworker/tasks/`. The coworker picks up tasks from `2working`, processes them, and moves them to `300logs` upon completion.

### Integration
The coworker leverages the **GitHub Copilot CLI** and **Model Context Protocol (MCP)** to interact with the system. It uses a suite of shell scripts (PowerShell/Bash) to bridge the gap between high-level AI reasoning and low-level system execution.

## 3. Implementation

The implementation process was iterative, focusing on building the infrastructure for the coworker to "live" in the repo.

### Core Components
1. **Memory System**: I implemented scripts to generate and maintain the hierarchical memory. This involved creating a `coworker-daily-memory-generator` that intelligently batches logs and summarizes them using `gh copilot explain`. A "Head 10 + Tail 300" strategy was adopted to handle large log files efficiently.
2. **Task Monitoring**: I built `monitor.ps1` and `monitor.sh` to fetch tasks from GitHub issues or URLs. These scripts use `gh search` with MD5 deduplication to prevent repetitive work.
3. **Self-Healing Automation**: To ensure robustness, I added loop detection logic that kills low-activity processes (<5% CPU) and moves stuck tasks, preventing the coworker from getting into infinite loops.
4. **Tooling & SDKs**: I developed the `browser4-nodejs` SDK and enhanced the `pulsar-agentic` module to support MCP, allowing the coworker to understand and call tools effectively.

### Challenges
- **Context Limits**: Initial attempts to process all logs hit token limits. This was solved by the batched summarization and compression strategies.
- **Shell Consistency**: Ensuring scripts ran reliably across both Windows (PowerShell) and Linux (Bash) required strict argument handling and quoting conventions.

## 4. Future Plans

The AI Coworker is currently in its infancy (Version 0.0.420), but the roadmap is ambitious:

- **Proactive Refactoring**: I plan to enable the coworker to autonomously identify code smells and propose refactoring PRs based on the "Code Style Guidelines".
- **Enhanced "Senses"**: Integrating more robust monitoring to allow the coworker to "see" build failures or performance regressions in real-time.
- **Collaborative Logic**: enabling multiple specialized agent instances (e.g., a "Tester" and a "Reviewer") to collaborate on complex tasks within the `coworker` framework.

## 5. Conclusion

Creating a project-builtin AI coworker has transformed the development workflow for Browser4. By embedding intelligence directly into the repository, I've created a system that grows with the project. The coworker doesn't just execute commands; it remembers context, learns from mistakes, and handles the drudgery of project management.

The journey from a simple script to a memory-augmented agent highlighted the importance of **context**. An AI that "lives" in your code, remembers your decisions, and adapts to your workflow is infinitely more valuable than a generic chatbot.

## 6. References

- **Browser4 Repository**: [https://github.com/platonai/Browser4](https://github.com/platonai/Browser4)
- **Model Context Protocol (MCP)**: Used for tool integration.
- **GitHub Copilot CLI**: The core intelligence engine.
