package de.larmic.pf2e.chat

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Service

@Service
class ChatService(
    chatClientBuilder: ChatClient.Builder,
    toolCallbackProvider: ToolCallbackProvider
) {
    private val chatClient = chatClientBuilder
        .defaultSystem(
            """
            You are a Pathfinder 2e rules expert assistant.
            Use the available tools to search for accurate rule information from the official game data.
            Always base your answers on the search results from the tools.
            If you cannot find relevant information, clearly state that.
            Be concise but thorough in your explanations.
            """.trimIndent()
        )
        .defaultToolCallbacks(toolCallbackProvider)
        .build()

    fun chat(message: String): String {
        return chatClient.prompt()
            .user(message)
            .call()
            .content() ?: "No response received"
    }
}
