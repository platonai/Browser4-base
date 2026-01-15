package ai.platon.pulsar.e2e.mcp

import ai.platon.pulsar.test.mcp.TestMCPServer
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

/**
 * Spring Boot test application for MCP end-to-end tests.
 * 
 * This application automatically starts the TestMCPServer as a Spring bean,
 * making it available for testing MCPToolExecutor in a realistic environment.
 */
@SpringBootApplication(scanBasePackages = ["ai.platon.pulsar"])
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
