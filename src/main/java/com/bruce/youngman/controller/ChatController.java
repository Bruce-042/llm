package com.bruce.youngman.controller;

import com.bruce.youngman.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author Liangyonghui
 * @since 2025/5/9 15:56
 */
@Controller
public class ChatController {

    @Resource
    private ChatService chatService;

    @GetMapping("/")
    public String chatPage() {
        return "chat";
    }

    @PostMapping("/chat")
    @ResponseBody
    public String chat(@RequestParam(value = "message", defaultValue = "你好") String message) {
        // 先确认用户意图
        String intent = chatService.confirmIndent(message);
        // 如果确认了意图，则进行回答
        if (intent != null && !intent.isEmpty() && !intent.contains("不明确")) {
            return chatService.askYoungMan(message);
        }
        return intent;
    }
}
