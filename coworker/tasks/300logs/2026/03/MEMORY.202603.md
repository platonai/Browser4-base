# Monthly Memory - 2026-03

## 2026-03-01
- **Task**: Reimplement Mock MCP Server
- **Summary**: Reimplemented MockMCPServer using `io.modelcontextprotocol` SDK (v0.8.1). Updated `pulsar-tests-common` dependencies and refactored endpoints to use Maps for serialization. Verified with updated unit and E2E tests, resolving Jackson serialization issues with `RestTestClient`.
- **Lessons**: Explicitly converting `ObjectNode` to `Map` ensures correct serialization for `RestTestClient`. Use `-am` flag when building dependent modules.

## 2026-03-02
- **Tasks**: Completed `delete-copilot-branches.ps1`, improved `run-agent-examples`, added filtering to `check_links.py`, created `move-scripts.py`, enhanced `count-total-token-usage.py`, reviewed AI Software Factory design, and implemented initial `orchestrator.ps1`.
- **Summary**: Focused on tooling improvements and implementing the AI Software Factory infrastructure. Delivered scripts for branch cleanup, link checking, script relocation, and token usage analysis. Reviewed and implemented the initial version of the AI orchestrator.
- **Lessons**: Local installation of dependencies is key for multi-module builds. Directory-based state machines provide robustness for AI workflows.
