# Implement AI Architect

According to `Future Evolution` section in [orchestrator.md](../../docs/architect/orchestrator.md),
the `orchestrator.ps1` script currently uses a placeholder function `Invoke-Copilot` to simulate AI interactions.
The next step in the evolution of the AI Software Factory is to replace this placeholder with actual calls to a real LLM provider,
such as GitHub Copilot.

- Learn from `coworker.ps1` to implement the `Invoke-Copilot` function that interacts with GitHub Copilot via CLI (e.g., `gh copilot`).

## Reference:

[orchestrator.ps1](../../scripts/architect/orchestrator.ps1)
[coworker.ps1](../../scripts/coworker.ps1)
