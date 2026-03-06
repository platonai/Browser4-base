# MEMORY.202603.md
## Monthly Memory - 2026-03

### Work Themes
- **AI Software Factory Implementation**:Designed and implemented the "Architect" orchestrator, directory-based workflow structures, and supporting scripts (e.g., `orchestrator.ps1`, `refine-last-draft`).
- **MCP & Agent Integration**: Reimplemented Mock MCP server using the official Kotlin SDK, refactored `AgentToolExecutor` for native MCP/Skills support, and added comprehensive controller tests.
- **DevEx & Tooling Improvements**: Enhanced developer utilities including branch cleanup, link checking filters, script moving automation, and agent example runners.
- **Refactoring & Cleanup**: Removed the obsolete `ToDoManager` component and standardized agent context logging directories.

### Recurring Issues
- **Scripting Fragility**:Repeated challenges with PowerShell argument passing (arrays vs. strings for CLI tools) and relative path resolution in helper scripts (`rename.ps1`, `coworker.ps1`).
- **Build & Dependency Management**: Frequent build failures due to missing local dependencies in multi-module projects, requiring explicit `mvn install` or `-am` flags.
- **Serialization Nuances**: Specific issues with Jackson `ObjectNode` vs. `Map` handling in `RestTestClient` and E2E tests.

### Structural Bottlenecks
- **Cross-Platform Maintenance**:Dual maintenance of `.ps1` and `.sh` scripts and handling path separator differences (backslashes vs. forward slashes) slows down tooling updates.
- **Local Repository State**: Reliance on local artifacts for dependent modules complicates isolated task execution.

### Efficiency Trend
- **High Velocity**:Rapid transition from AI Software Factory design to functional implementation.
- **Investment in Automation**: Proactive creation of maintenance tools (`move-scripts`, `check_links` filters) indicates a shift towards long-term efficiency over short-term fixes.

### System Adjustments Proposed
- **Robust Script Invocation**:Standardize PowerShell argument passing (prefer explicit quoting) and strictly verify relative paths for helper scripts.
- **Unified Build Practices**: consistently use `-am` (also make) or `mvn install` when working with cross-module dependencies to ensure local repo consistency.
- **Directory-Based Architectures**: Expand the use of directory-based state machines (validated by the Orchestrator and Agent Logging work) for managing complex asynchronous workflows.

### Daily Rollup (2026-03-01 to 2026-03-04)
- Rebuilt Mock MCP server using the official Kotlin SDK and aligned request handling/tests to Map-based payloads for reliable serialization.
- Delivered automation/tooling improvements across coworker scripts (branch cleanup, orchestrator/copilot integration, draft refinement, link filtering, script relocation, and token usage reporting).
- Advanced AI Software Factory architecture and documentation with orchestrator workflows, templates, and directory-based execution/state patterns.
- Refactored agent/tool internals by removing obsolete ToDoManager logic, integrating native MCP/Skills execution in `AgentToolExecutor`, and adding broad MCP controller test coverage.
- Recurring execution insight: use robust absolute paths, reliable PowerShell argument quoting, and `-am`/local installs to avoid cross-module and invocation-context failures.

### Daily Rollup Update (2026-03-01 to 2026-03-05)
- Migrated MCP mock server/testing to official SDK patterns, stabilized Map-based payload handling, and fixed stale test-class/API mismatch failures.
- Expanded MCP controller coverage (unit + E2E focus), with environment caveats noted for Kotlin daemon startup.
- Built/iterated AI Software Factory orchestration and related coworker automation scripts/docs.
- Refined tooling reliability around absolute-path resolution, PowerShell argument quoting, and script modularization.
- Reaffirmed execution hygiene: prefer repo-root absolute paths and `-am`/local installs for multi-module consistency.
