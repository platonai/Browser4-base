# AI Software Factory Orchestrator

## Overview

The `orchestrator.ps1` script is the core engine of the AI Software Factory. It automates the software development lifecycle by processing tasks (stories) through a series of AI-driven stages, from analysis to implementation and validation.

## How It Works

### Workflow

1.  **Monitoring**: The orchestrator continuously checks the `coworker/tasks/1created` directory for new task folders.
2.  **Processing**: When a new task is detected, it is immediately moved to `coworker/tasks/2working` to signal that work has begun.
3.  **Pipeline Execution**: The task undergoes a sequential pipeline of AI-driven steps:
    *   **Analysis**: Analyzes the `story.md` requirements using the template `analysis.prompt.md`.
    *   **Plan**: Creates a structured development plan (`plan.json`) using `plan.prompt.md`.
    *   **SubFeature Design**: Generates detailed design documents (`design.md`) using `subfeature.prompt.md`. This step is designed to iterate through sub-features defined in the plan.
    *   **Implementation**: Produces code patches (`impl.patch`) based on the design using `implementation.prompt.md`.
    *   **E2E Test**: Generates End-to-End (E2E) tests (`e2e.kt`) to verify the implementation using `e2e.prompt.md`.
    *   **Validation**: Validates the generated artifacts against the original requirements using `validation.prompt.md`.
4.  **Completion**: Upon successful execution of all steps, the task folder is moved to `coworker/tasks/3_1complete`.
5.  **Failure Handling**: If any step fails (e.g., missing files, execution errors), the task is moved to `coworker/tasks/3_5aborted` for manual inspection.

### Directory Structure

*   `coworker/tasks/100templates`: Contains the prompt templates for each stage of the pipeline.
*   `coworker/tasks/300logs`: Stores detailed execution logs for each task, organized by story ID.
*   `coworker/tasks/1created`: Input directory where new tasks are placed.
*   `coworker/tasks/2working`: Active processing directory.
*   `coworker/tasks/3_1complete`: Archive for successfully completed tasks.
*   `coworker/tasks/3_5aborted`: Archive for failed or aborted tasks.

## Design Principles

1.  **Automated State Machine**: The system uses file system directories to represent the state of a task (Created -> Working -> Complete/Aborted), ensuring persistence and easy monitoring without a complex database.
2.  **Template-Driven Intelligence**: All AI interactions are governed by markdown templates. This allows easy customization and tuning of the AI's behavior without modifying the core orchestrator code.
3.  **Modular Pipeline**: The process is broken down into discrete, logical steps (Analysis -> Plan -> Design -> Code -> Test), mimicking a standard software engineering workflow.
4.  **Traceability**: Every interaction with the AI is logged with timestamps, full prompts, and raw outputs, providing a complete audit trail for debugging and improvement.
5.  **Idempotency & Recovery**: The system is designed to handle failures by isolating problematic tasks, preventing them from blocking the entire pipeline.

## Future Evolution

1.  **Real AI Integration**: Replace the current placeholder `Invoke-Copilot` function with actual CLI calls to GitHub Copilot or other LLM providers (e.g., `gh copilot`).
2.  **Iterative Refinement Loop**: Implement feedback loops where validation failures trigger automatic re-planning or re-implementation attempts.
3.  **Parallel Processing**: Support concurrent execution of multiple sub-features or even multiple stories to increase throughput.
4.  **Enhanced Error Handling**: Add sophisticated retry mechanisms, exponential backoff, and more granular error reporting.
5.  **Dynamic Configuration**: Allow per-task configuration overrides (e.g., model selection, specific linter rules) via a configuration file in the task folder.
6.  **Human-in-the-Loop**: Introduce pause points for human review and approval before critical stages (e.g., before code commitment).
