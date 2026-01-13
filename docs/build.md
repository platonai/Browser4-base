# ▶️ Build & Run Browser4

## Build from Source

```shell
git clone https://github.com/platonai/Browser4.git
cd Browser4 && bin/build-run.sh
```

For Chinese developers, we strongly suggest you to follow [this](/bin/tools/maven/maven-settings.md) instruction to accelerate the building process.

## Build Windows Installer

Browser4 Agents can be packaged as a Windows installer using JDK's jpackage tool.

### Prerequisites

- JDK 17 or later (with jpackage)
- Maven 3.6+
- For EXE installer: [WiX Toolset v3.x](https://wixtoolset.org/releases/)

### Quick Start

Navigate to the browser4-agents module and run:

```powershell
cd browser4/browser4-agents
./build-windows-installer.ps1
```

Or with CMD:

```cmd
cd browser4\browser4-agents
build-windows-installer.cmd
```

### Build Options

**Portable App-Image (no installation required)**:
```shell
mvnw package -Pwin-jpackage -DskipTests
```
Output: `target/jpackage/app-image/Browser4/Browser4.exe`

**Windows EXE Installer** (requires WiX):
```shell
mvnw package -Pwin-jpackage -Djpackage.installer.skip=false -DskipTests
```
Output: `target/jpackage/dist/Browser4-4.4.0.exe`

**PowerShell with Installer**:
```powershell
./build-windows-installer.ps1 -Installer
```

See [Windows Installer Guide](windows-installer.md) and [browser4-agents/README.md](../browser4/browser4-agents/README.md) for detailed documentation.
