# Installation

This guide will help you add Browser4 Kotlin SDK to your project.

## System Requirements

- **Java**: 17 or later
- **Kotlin**: 1.9.0 or later (if using Kotlin)
- **Build Tool**: Maven or Gradle
- **Operating System**: Windows, macOS, or Linux

## Maven Installation

Add the following dependency to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>ai.platon.pulsar</groupId>
        <artifactId>pulsar-sdk-kotlin</artifactId>
        <version>4.6.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

If you're using a SNAPSHOT version, you may need to add the Sonatype snapshots repository:

```xml
<repositories>
    <repository>
        <id>sonatype-snapshots</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

## Gradle Installation

### Kotlin DSL (build.gradle.kts)

```kotlin
dependencies {
    implementation("ai.platon.pulsar:pulsar-sdk-kotlin:4.6.0-SNAPSHOT")
}

repositories {
    mavenCentral()
    // Add this for SNAPSHOT versions
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}
```

### Groovy DSL (build.gradle)

```groovy
dependencies {
    implementation 'ai.platon.pulsar:pulsar-sdk-kotlin:4.6.0-SNAPSHOT'
}

repositories {
    mavenCentral()
    // Add this for SNAPSHOT versions
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots'
    }
}
```

## Verify Installation

Create a simple test file to verify the installation:

```kotlin
import ai.platon.pulsar.sdk.v0.AgenticSession

suspend fun main() {
    println("Browser4 Kotlin SDK is installed!")

    // This will verify the SDK can be imported
    val session = AgenticSession.getOrCreate()
    println("Session created successfully!")
    session.context.close()
}
```

Run the file:

```bash
# Maven
mvn compile exec:java -Dexec.mainClass="YourMainClassKt"

# Gradle
gradle run
```

If you see "Browser4 Kotlin SDK is installed!" and no errors, the installation was successful!

## Additional Dependencies

The SDK automatically includes these transitive dependencies:

- **Kotlin Standard Library** - Core Kotlin functionality
- **Kotlinx Coroutines** - Async/await support
- **Ktor Client** - HTTP client for API communication
- **Gson** - JSON serialization/deserialization
- **Jsoup** - HTML parsing

You don't need to add these manually.

## Optional: Local Driver Setup

When using local driver mode (default), the SDK will automatically:

1. Download Browser4.jar from GitHub releases (if not present)
2. Save it to `~/.browser4/Browser4.jar`
3. Start the driver on port 8182

You can customize this behavior - see [Local Driver Configuration](../configuration/local-driver.md).

## Environment Variables

For AI-powered features, set your OpenRouter API key:

### Linux/macOS

```bash
export OPENROUTER_API_KEY="your-api-key-here"
```

### Windows (Command Prompt)

```cmd
set OPENROUTER_API_KEY=your-api-key-here
```

### Windows (PowerShell)

```powershell
$env:OPENROUTER_API_KEY="your-api-key-here"
```

!!! info "API Key is Optional"
    The API key is only needed if you use AI-powered features like `agent.act()` or `agent.run()`. Basic automation and data extraction work without it.

## IDE Setup

### IntelliJ IDEA

IntelliJ IDEA has excellent Kotlin support built-in. Just open your project and you're ready to go!

### VS Code

Install the [Kotlin extension](https://marketplace.visualstudio.com/items?itemName=fwcd.kotlin):

1. Open VS Code
2. Go to Extensions (Ctrl+Shift+X)
3. Search for "Kotlin"
4. Install the "Kotlin Language" extension

### Eclipse

Install the Kotlin plugin:

1. Go to Help → Eclipse Marketplace
2. Search for "Kotlin"
3. Install "Kotlin Plugin for Eclipse"

## Troubleshooting

### "Could not find artifact" Error

If Maven/Gradle can't find the artifact:

1. Ensure you've added the snapshot repository (if using SNAPSHOT version)
2. Try refreshing dependencies:
   - Maven: `mvn clean install`
   - Gradle: `gradle clean build --refresh-dependencies`

### "Class not found" at Runtime

Make sure you're using Java 17 or later:

```bash
java -version
```

If you see a version less than 17, update Java:

- **Linux**: `sudo apt-get install openjdk-17-jdk`
- **macOS**: `brew install openjdk@17`
- **Windows**: Download from [adoptium.net](https://adoptium.net/)

### Port 8182 Already in Use

If the local driver can't start because port 8182 is in use:

```kotlin
val options = LocalDriverOptions(port = 9000)
val client = PulsarClient(useLocalDriver = true, localDriverOptions = options)
```

See [Local Driver Configuration](../configuration/local-driver.md) for more options.

## Next Steps

Now that the SDK is installed, let's write some code:

- [Quick Start Guide](quick-start.md) - Run your first script
- [First Steps Tutorial](first-steps.md) - Learn the basics
- [Examples](../examples/basic-usage.md) - See real code examples
