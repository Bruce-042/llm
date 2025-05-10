package com.bruce.youngman.controller;

import com.bruce.youngman.service.ChatService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * @author Liangyonghui
 * @since 2025/5/9 15:56
 */
@RestController
public class ChatController {

    @Resource
    private ChatService chatService;


    @GetMapping("/askYoungMan")
    public String askYoungMan(@RequestParam(value = "message", defaultValue = "你好") String message) {
        return chatService.askYoungMan(message);
    }



    @GetMapping("/confirmIndent")
    public String confirmIndent(@RequestParam(value = "message", defaultValue = "你好") String message) {
        return chatService.confirmIndent(message);
    }

}
