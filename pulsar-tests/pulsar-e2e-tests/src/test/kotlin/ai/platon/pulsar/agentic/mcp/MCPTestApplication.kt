package ai.platon.pulsar.agentic.mcp

import ai.platon.pulsar.test.mcp.TestMCPServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType

/**
 * Spring Boot test application for MCP end-to-end tests.
 *
 * This application automatically starts the TestMCPServer as a Spring bean,
 * making it available for testing MCPToolExecutor in a realistic environment.
 */
@SpringBootApplication
@ComponentScan(
    basePackages = ["ai.platon.pulsar.agentic.mcp", "ai.platon.pulsar.test.mcp"],
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.REGEX, pattern = ["ai\\.platon\\.pulsar\\.rest\\..*"])
    ]
)
class MCPTestApplication {

    /**
     * Creates and configures a TestMCPServer bean for testing.
     *
     * @return A TestMCPServer instance with default configuration.
     */
    @Bean
    fun testMCPServer(): TestMCPServer {
        return TestMCPServer(
            serverName = "test-mcp-server",
            serverVersion = "1.0.0"
        )
    }
}
