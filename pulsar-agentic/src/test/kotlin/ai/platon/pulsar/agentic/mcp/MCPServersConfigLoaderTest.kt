package ai.platon.pulsar.agentic.mcp

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.writeText

/**
 * Tests for MCPServersConfigLoader functionality.
 *
 * These tests validate the JSON configuration file loading and parsing for MCP servers.
 */
@Tag("unit")
@Tag("mcp")
class MCPServersConfigLoaderTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var registry: MCPPluginRegistry
    private lateinit var configLoader: MCPServersConfigLoader

    @BeforeEach
    fun setUp() {
        registry = MCPPluginRegistry()
        configLoader = MCPServersConfigLoader(tempDir, registry)
    }

    @AfterEach
    fun tearDown() {
        configLoader.close()
        registry.close()
        // Clean up singleton registry
        MCPPluginRegistry.instance.getRegisteredServers().forEach {
            MCPPluginRegistry.instance.unregisterMCPServer(it)
        }
    }

    @Test
    fun testParseJsonBasicFormat() {
        val json = """
            {
              "mcpServers": {
                "filesystem": {
                  "command": "npx",
                  "args": ["-y", "@modelcontextprotocol/server-filesystem", "/Users/test/Desktop"]
                }
              }
            }
        """.trimIndent()

        val config = MCPServersConfigLoader.parseJson(json)

        assertEquals(1, config.mcpServers.size)
        assertTrue(config.mcpServers.containsKey("filesystem"))

        val serverConfig = config.mcpServers["filesystem"]!!
        assertEquals("npx", serverConfig.command)
        assertEquals(listOf("-y", "@modelcontextprotocol/server-filesystem", "/Users/test/Desktop"), serverConfig.args)
        assertTrue(serverConfig.enabled)
    }

    @Test
    fun testParseJsonMultipleServers() {
        val json = """
            {
              "mcpServers": {
                "filesystem": {
                  "command": "npx",
                  "args": ["-y", "@modelcontextprotocol/server-filesystem", "/path"]
                },
                "puppeteer": {
                  "command": "npx",
                  "args": ["-y", "@modelcontextprotocol/server-puppeteer"]
                },
                "github": {
                  "command": "node",
                  "args": ["github-server.js"],
                  "enabled": false
                }
              }
            }
        """.trimIndent()

        val config = MCPServersConfigLoader.parseJson(json)

        assertEquals(3, config.mcpServers.size)
        assertTrue(config.mcpServers.containsKey("filesystem"))
        assertTrue(config.mcpServers.containsKey("puppeteer"))
        assertTrue(config.mcpServers.containsKey("github"))

        val githubConfig = config.mcpServers["github"]!!
        assertEquals("node", githubConfig.command)
        assertFalse(githubConfig.enabled)
    }

    @Test
    fun testParseJsonWithEnvVariables() {
        val json = """
            {
              "mcpServers": {
                "server-with-env": {
                  "command": "node",
                  "args": ["server.js"],
                  "env": {
                    "API_KEY": "secret123",
                    "DEBUG": "true"
                  }
                }
              }
            }
        """.trimIndent()

        val config = MCPServersConfigLoader.parseJson(json)

        assertEquals(1, config.mcpServers.size)
        val serverConfig = config.mcpServers["server-with-env"]!!
        assertNotNull(serverConfig.env)
        assertEquals("secret123", serverConfig.env!!["API_KEY"])
        assertEquals("true", serverConfig.env!!["DEBUG"])
    }

    @Test
    fun testParseJsonEmptyServers() {
        val json = """
            {
              "mcpServers": {}
            }
        """.trimIndent()

        val config = MCPServersConfigLoader.parseJson(json)

        assertTrue(config.mcpServers.isEmpty())
    }

    @Test
    fun testParseJsonIgnoresUnknownFields() {
        val json = """
            {
              "mcpServers": {
                "test-server": {
                  "command": "node",
                  "args": ["test.js"],
                  "unknownField": "should be ignored",
                  "anotherUnknown": 123
                }
              },
              "otherTopLevelField": "should also be ignored"
            }
        """.trimIndent()

        val config = MCPServersConfigLoader.parseJson(json)

        assertEquals(1, config.mcpServers.size)
        val serverConfig = config.mcpServers["test-server"]!!
        assertEquals("node", serverConfig.command)
        assertEquals(listOf("test.js"), serverConfig.args)
    }

    @Test
    fun testToMCPConfigs() {
        val jsonConfig = MCPServersJsonConfigFile(
            mcpServers = mapOf(
                "server1" to MCPServerJsonConfig(command = "node", args = listOf("a.js")),
                "server2" to MCPServerJsonConfig(command = "npx", args = listOf("-y", "pkg"), enabled = false)
            )
        )

        val mcpConfigs = MCPServersConfigLoader.toMCPConfigs(jsonConfig)

        assertEquals(2, mcpConfigs.size)

        val server1 = mcpConfigs.find { it.serverName == "server1" }!!
        assertEquals("node", server1.command)
        assertEquals(listOf("a.js"), server1.args)
        assertEquals(MCPTransportType.STDIO, server1.transportType)
        assertTrue(server1.enabled)

        val server2 = mcpConfigs.find { it.serverName == "server2" }!!
        assertEquals("npx", server2.command)
        assertFalse(server2.enabled)
    }

    @Test
    fun testLoadFromFileNonexistent() = runBlocking {
        val nonExistentFile = tempDir.resolve("nonexistent.json")

        val errors = configLoader.loadFromFile(nonExistentFile)

        assertTrue(errors.isEmpty())
        assertTrue(configLoader.getLoadedServers().isEmpty())
    }

    @Test
    fun testLoadFromFileWithDisabledServers() = runBlocking {
        val configFile = tempDir.resolve("test-config.json")
        configFile.writeText("""
            {
              "mcpServers": {
                "disabled-server": {
                  "command": "node",
                  "args": ["server.js"],
                  "enabled": false
                }
              }
            }
        """.trimIndent())

        val errors = configLoader.loadFromFile(configFile)

        // Disabled servers should not cause errors
        assertTrue(errors.isEmpty())
        // Server should not be registered since it's disabled
        assertFalse(registry.isRegistered("disabled-server"))
    }

    @Test
    fun testLoadFromFileWithInvalidJson() = runBlocking {
        val configFile = tempDir.resolve("invalid.json")
        configFile.writeText("{ invalid json }")

        val errors = configLoader.loadFromFile(configFile)

        assertTrue(errors.isNotEmpty())
        assertTrue(errors.containsKey("__file_error__"))
    }

    @Test
    fun testLoadFromDirectory() = runBlocking {
        // Create multiple config files
        tempDir.resolve("config1.json").writeText("""
            {
              "mcpServers": {
                "server1": {
                  "command": "echo",
                  "args": ["test"],
                  "enabled": false
                }
              }
            }
        """.trimIndent())

        tempDir.resolve("config2.json").writeText("""
            {
              "mcpServers": {
                "server2": {
                  "command": "echo",
                  "args": ["test2"],
                  "enabled": false
                }
              }
            }
        """.trimIndent())

        // Non-JSON file should be ignored
        tempDir.resolve("readme.txt").writeText("This is not a config file")

        val errors = configLoader.loadFromDirectory()

        assertTrue(errors.isEmpty())
        assertEquals(2, configLoader.getLoadedFiles().size)
    }

    @Test
    fun testLoadFromEmptyDirectory() = runBlocking {
        val errors = configLoader.loadFromDirectory()

        assertTrue(errors.isEmpty())
        assertTrue(configLoader.getLoadedServers().isEmpty())
        assertTrue(configLoader.getLoadedFiles().isEmpty())
    }

    @Test
    fun testGetLoadedServersInitiallyEmpty() {
        assertTrue(configLoader.getLoadedServers().isEmpty())
    }

    @Test
    fun testGetLoadedFilesInitiallyEmpty() {
        assertTrue(configLoader.getLoadedFiles().isEmpty())
    }

    @Test
    fun testCloseDoesNotThrow() {
        assertDoesNotThrow {
            configLoader.close()
        }
    }

    @Test
    fun testStartAndStopPeriodicScan() {
        assertDoesNotThrow {
            configLoader.startPeriodicScan(
                interval = Duration.ofMinutes(10),
                initialDelay = Duration.ofMinutes(10)
            )
        }

        assertDoesNotThrow {
            configLoader.stopPeriodicScan()
        }
    }

    @Test
    fun testStartPeriodicScanTwiceWarns() {
        configLoader.startPeriodicScan(
            interval = Duration.ofMinutes(10),
            initialDelay = Duration.ofMinutes(10)
        )

        // Second call should warn but not throw
        assertDoesNotThrow {
            configLoader.startPeriodicScan(
                interval = Duration.ofMinutes(10),
                initialDelay = Duration.ofMinutes(10)
            )
        }

        configLoader.stopPeriodicScan()
    }

    @Test
    fun testCheckAndReloadWithNoFiles() = runBlocking {
        val errors = configLoader.checkAndReload()
        assertTrue(errors.isEmpty())
    }

    @Test
    fun testMCPPathsConfigDirExists() {
        // The MCP config dir should be created when MCPPaths is initialized
        // This test verifies that either:
        // 1. The directory exists, or
        // 2. The parent directory doesn't exist (test environment with limited directories)
        val dirExists = Files.exists(MCPPaths.MCP_CONFIG_DIR)
        val parentExists = Files.exists(MCPPaths.MCP_CONFIG_DIR.parent)
        assertTrue(dirExists || !parentExists, 
            "Expected MCP_CONFIG_DIR to exist or parent to not exist. " +
            "Dir: ${MCPPaths.MCP_CONFIG_DIR}, exists: $dirExists, parent exists: $parentExists")
    }

    @Test
    fun testMCPPathsConstants() {
        // Verify the path constants are properly defined
        assertTrue(MCPPaths.MCP_CONFIG_DIR.toString().contains("mcp"))
        assertTrue(MCPPaths.MCP_SERVERS_CONFIG_FILE.toString().endsWith("mcp-servers.json"))
    }
}
