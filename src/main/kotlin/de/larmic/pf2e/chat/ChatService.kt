package de.larmic.pf2e.chat

import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.stereotype.Service

@Service
class ChatService(
    chatClientBuilder: ChatClient.Builder,
    toolCallbackProvider: ToolCallbackProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val chatClient = chatClientBuilder
        .defaultSystem(
            """
            You are a Pathfinder 2e rules expert assistant.
            Use the available tools to search for accurate rule information from the official game data.
            Always base your answers on the search results from the tools.
            If you cannot find relevant information, clearly state that.
            Be concise but thorough in your explanations.

            TOOL SELECTION GUIDE:
            - Game rules & mechanics (HP calculation, dying, bonuses, combat) → searchGameRules
            - Character classes (Fighter, Wizard, etc.) → searchClasses
            - Ancestries/races (Elf, Dwarf, Human) → searchAncestries
            - Backgrounds (Acolyte, Criminal) → searchBackgrounds
            - Spells and cantrips → searchSpells
            - Feats and abilities → searchFeats
            - Actions and activities → searchActions
            - Items, weapons, armor → searchEquipment
            - Conditions (frightened, stunned) → searchConditions
            - General questions → searchRules
            - Exact name lookup → getEntry

            For character building, start with searchClasses or searchAncestries.
            For rule questions, start with searchGameRules.
            For complex questions, call multiple tools.

            IMPORTANT: Always respond in the same language the user writes in.
            When searching, translate the user's query to English for the tools.
            For official game terms, include the English term in parentheses, e.g., "Feuerball (Fireball)".
            """.trimIndent()
        )
        .defaultToolCallbacks(toolCallbackProvider)
        .build()

    fun chat(message: String): String {
        log.debug("Starting LLM call")
        val startTime = System.currentTimeMillis()

        val result = chatClient.prompt()
            .user(message)
            .call()
            .content() ?: "No response received"

        val duration = System.currentTimeMillis() - startTime
        log.debug("LLM call completed in {}ms", duration)

        return result
    }
}
