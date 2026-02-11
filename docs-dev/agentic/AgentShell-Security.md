# AgentShell Security & Whitelist

## Overview

`AgentShell` is a secure shell command execution subsystem for AI agents. It provides controlled execution of shell commands with strict security controls to prevent destructive operations.

## Security Model

### Command Whitelist

As of the latest update, `AgentShell` implements a **whitelist-based security model**. Only explicitly allowed commands can be executed. This provides strong protection against potentially harmful operations.

### Allowed Commands

The following commands are permitted (first batch):

**Basic Navigation and Listing**
- `ls` - List directory contents
- `pwd` - Print working directory
- `tree` - Display directory tree structure

**File Viewing**
- `cat` - Concatenate and display files
- `less` - View file contents with pagination
- `head` - Display first lines of a file
- `tail` - Display last lines of a file

**Text Processing**
- `grep` - Search text patterns
- `awk` - Pattern scanning and processing
- `sed` - Stream editor for text manipulation (in-place editing with `-i` flag is blocked)

**Note on sed**: While `sed` is in the whitelist, in-place editing with the `-i` flag is explicitly blocked to prevent file modifications. Use read-only operations like `sed -n` instead.

**Counting**
- `wc` - Word, line, and character count

**System Information**
- `uname` - System information
- `hostname` - Display hostname
- `uptime` - System uptime
- `whoami` - Display current user
- `id` - Display user and group IDs

**Resource Monitoring**
- `free` - Display memory usage
- `df` - Display disk space usage
- `du` - Display directory space usage

**Process Information**
- `ps` - Display process status
- `top` - Display running processes
- `pgrep` - Search for processes by name

**Network Information**
- `ip addr` - Display IP addresses
- `ip route` - Display routing table
- `ss` - Display socket statistics

**Note on ip**: Only `ip addr` and `ip route` are allowed. Other ip subcommands (like `ip link`, `ip neigh`) are blocked.

**Environment**
- `env` - Display environment variables
- `printenv` - Print environment variables
- `which` - Locate a command
- `type` - Display command type

### Blocked Commands

All commands not in the whitelist are automatically blocked, including but not limited to:

- File modification: `rm`, `mv`, `cp`, `touch`, `mkdir`, `rmdir`
- Permission changes: `chmod`, `chown`
- Network operations: `curl`, `wget`, `ssh`, `scp`
- Script execution: `bash`, `sh`, `python`, `node`, `perl`
- Package management: `apt`, `yum`, `npm`, `pip`
- System modification: `shutdown`, `reboot`, `init`

### Additional Security Controls

In addition to the whitelist, `AgentShell` implements:

1. **Pattern-based blocking** - Certain destructive patterns are explicitly blocked:
   - Recursive deletion of root: `rm -rf /`
   - Disk formatting: `mkfs.*`
   - Raw disk writes: `dd ... of=/dev/...`
   - System shutdown/reboot
   - Fork bombs

2. **Path traversal prevention** - Working directories must stay within the configured base directory

3. **Timeout enforcement** - Commands are limited to a maximum execution time (default: 30 seconds, max: 300 seconds)

4. **Output truncation** - Output is limited to 100,000 characters to prevent memory exhaustion

5. **Command-specific restrictions**:
   - `sed -i` (in-place editing) is blocked to prevent file modifications
   - Only `ip addr` and `ip route` are allowed; other `ip` subcommands are blocked

## Usage

### Basic Execution

```kotlin
import ai.platon.pulsar.agentic.common.AgentShell
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

fun main() = runBlocking {
    val shell = AgentShell(baseDir = Paths.get("/tmp/agent-work"))
    
    // Allowed command - executes successfully
    val result = shell.execute("ls -la")
    println(result)
    
    // Blocked command - returns error message
    val blocked = shell.execute("rm file.txt")
    println(blocked)  // Error: command 'rm' is not in the whitelist...
}
```

### With Timeout and Working Directory

```kotlin
val result = shell.execute(
    command = "grep -r 'pattern' .",
    timeoutSeconds = 60,
    workingDir = "subdirectory"
)
```

### Session Management

```kotlin
// Execute command
val result = shell.execute("ls -la")

// Read previous output by session ID
val output = shell.readOutput("shell-1")

// Get execution status
val status = shell.getStatus("shell-1")

// List all sessions
val sessions = shell.listSessions()
```

## Error Handling

When a command is blocked or fails, `AgentShell` returns a descriptive error message:

```kotlin
val result = shell.execute("curl http://example.com")
// Returns: "Error: Command blocked for security reasons - 
//           command 'curl' is not in the whitelist. 
//           Allowed commands: awk, cat, df, du, env, free, ..."
```

## Design Rationale

The whitelist approach provides:

1. **Predictable behavior** - Agents can only execute read-only, inspection commands
2. **Defense in depth** - Even if other security controls fail, destructive operations are impossible
3. **Auditability** - Clear list of permitted operations
4. **Extensibility** - Whitelist can be expanded based on specific use cases

## Future Enhancements

Potential future improvements:

- Configurable whitelist per agent or session
- Command aliasing and parameter validation
- Rate limiting per command type
- Audit logging of all command executions
- Fine-grained permissions (e.g., `sed -n` only, not all `sed` operations)

## Related Components

- **ShellToolExecutor** - MCP tool executor that wraps AgentShell
- **AgentFileSystem** - Secure file system operations for agents
- **BrowserPerceptiveAgent** - Main agent that uses these tools

## See Also

- [MCP (Model Context Protocol) Integration](../mcp/)
- [Agent Security Model](./README.md)
- [Tool Execution Framework](./custom-agent-tools.md)
