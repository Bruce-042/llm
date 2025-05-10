package com.bruce.youngman.service;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

/**
 * @author Liangyonghui
 * @since 2025/5/9 16:00
 */
@Service
public class ChatService {

    @Resource
    private YoungMan youngMan;

    @Resource
    private YoungMan confirmIntentYoungMan;

    public String askYoungMan(String message) {
        return youngMan.answer(message);
    }

    public String confirmIndent(String message) {
        return confirmIntentYoungMan.answer(message);
    }
}
