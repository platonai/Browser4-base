# Windows Installer Build Guide

This document provides instructions for building the Browser4 Agents application as a Windows installer.

## Quick Start

### For End Users (Windows)

1. Navigate to the browser4-agents directory:
   ```cmd
   cd browser4\browser4-agents
   ```

2. Run the build script:
   ```cmd
   build-windows-installer.cmd --installer
   ```
   
   Or with PowerShell:
   ```powershell
   .\build-windows-installer.ps1 -Installer
   ```

3. The installer will be created at:
   ```
   target\jpackage\dist\Browser4-4.4.0.exe
   ```

### For Developers

Build from the repository root:

**Portable App-Image** (no installation required):
```shell
mvnw clean package -pl browser4/browser4-agents -am -Pwin-jpackage -DskipTests
```

**Windows Installer** (requires WiX Toolset):
```shell
mvnw clean package -pl browser4/browser4-agents -am -Pwin-jpackage -Djpackage.installer.skip=false -DskipTests
```

## Prerequisites

- **JDK 17+** with jpackage tool
- **Maven 3.6+**
- **WiX Toolset v3.x** (for .exe installer only)
  - Download from: https://github.com/wixtoolset/wix3/releases
  - Add to PATH (candle.exe and light.exe must be accessible)

## Build Outputs

| Output | Location | Description |
|--------|----------|-------------|
| Spring Boot JAR | `target/Browser4.jar` | Executable JAR (~314MB) |
| App-Image | `target/jpackage/app-image/Browser4/` | Portable directory with exe |
| Windows Installer | `target/jpackage/dist/Browser4-4.4.0.exe` | Traditional installer |

## Configuration

The build can be customized through Maven properties:

```shell
mvnw package -Pwin-jpackage -Djpackage.appVersion=4.5.0 -DskipTests
```

Properties:
- `jpackage.appVersion`: Application version (default: 4.4.0)
- `jpackage.skip`: Skip app-image creation (default: false in profile)
- `jpackage.installer.skip`: Skip installer creation (default: true)

## Application Icon

To add a custom icon:

1. Create directory: `browser4/browser4-agents/packaging/windows/`
2. Add icon file: `Browser4.ico` (256x256 recommended)
3. Add icon arguments to pom.xml executions (see comments in pom.xml)

## Troubleshooting

### "jpackage: command not found"
- Ensure JDK 17+ is installed (not just JRE)
- Verify: `jpackage --version`

### WiX errors
- Install WiX Toolset v3.x
- Add WiX bin directory to PATH
- Verify: `candle.exe -?`

### Build fails with dependency errors
- Build from repository root with `-am` flag
- Ensure all parent modules are built first

### Application doesn't start
- Console window shows logs (--win-console flag is enabled)
- Check for port conflicts (default: 8182)
- Ensure target machine has Java 17+ (bundled JRE should suffice)

## Distribution

### App-Image (Portable)
1. ZIP the `target/jpackage/app-image/Browser4/` directory
2. Users extract and run `Browser4.exe`
3. No installation required
4. Size: ~500MB (app + bundled JRE)

### Windows Installer
1. Distribute `Browser4-4.4.0.exe`
2. Users run the installer
3. Creates Start Menu shortcuts
4. Adds to Programs and Features
5. Professional installation experience

## Technical Details

### jpackage Process

1. **Stage Input**: Maven copies the Spring Boot JAR to `target/jpackage/input/`
2. **Create App-Image**: jpackage bundles JAR with JRE into `target/jpackage/app-image/`
3. **Create Installer**: jpackage uses WiX to wrap app-image into .exe installer

### JVM Options

The bundled runtime includes these default options:
- `-Dfile.encoding=UTF-8`

Additional options can be specified in pom.xml under `--java-options`.

### Console Window

The `--win-console` flag keeps a console window open to show application logs. This is useful for:
- Development and debugging
- Seeing startup logs
- Monitoring application behavior

For production deployment, remove this flag from pom.xml.

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Build Windows Installer

on:
  push:
    tags:
      - 'v*'

jobs:
  build-windows:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Install WiX Toolset
        run: |
          choco install wixtoolset -y
          
      - name: Build Installer
        run: |
          mvnw clean package -pl browser4/browser4-agents -am -Pwin-jpackage -Djpackage.installer.skip=false -DskipTests
          
      - name: Upload Installer
        uses: actions/upload-artifact@v3
        with:
          name: Browser4-Installer
          path: browser4/browser4-agents/target/jpackage/dist/*.exe
```

## Related Resources

- [browser4-agents/README.md](../browser4/browser4-agents/README.md) - Module-specific documentation
- [JPackage Documentation](https://docs.oracle.com/en/java/javase/17/jpackage/)
- [WiX Toolset](https://wixtoolset.org/)
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)

## Support

For issues or questions:
- Open an issue on GitHub
- Check existing documentation in `docs/` directory
- Review the main README.md for general Browser4 usage
