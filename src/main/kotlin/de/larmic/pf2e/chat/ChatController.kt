package de.larmic.pf2e.chat

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
class ChatController(private val chatService: ChatService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun chat(@RequestBody request: ChatRequest): ChatResponse {
        log.info("Received chat request: {}", request.message.take(100))
        val startTime = System.currentTimeMillis()

        val response = chatService.chat(request.message)

        val duration = System.currentTimeMillis() - startTime
        log.info("Chat response generated in {}ms, length: {} chars", duration, response.length)

        return ChatResponse(response)
    }
}

data class ChatRequest(val message: String)
data class ChatResponse(val response: String)
