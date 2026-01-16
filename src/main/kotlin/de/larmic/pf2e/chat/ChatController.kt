package de.larmic.pf2e.chat

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
class ChatController(private val chatService: ChatService) {

    @PostMapping
    fun chat(@RequestBody request: ChatRequest): ChatResponse {
        return ChatResponse(chatService.chat(request.message))
    }
}

data class ChatRequest(val message: String)
data class ChatResponse(val response: String)
