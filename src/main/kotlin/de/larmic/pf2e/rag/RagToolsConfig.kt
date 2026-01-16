package de.larmic.pf2e.rag

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration for registering RAG tools with the MCP server.
 *
 * The MethodToolCallbackProvider scans the RagService for @Tool annotated
 * methods and exposes them as MCP tools to connected clients.
 */
@Configuration
class RagToolsConfig {

    @Bean
    fun ragTools(ragService: RagService): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder()
            .toolObjects(ragService)
            .build()
    }
}
