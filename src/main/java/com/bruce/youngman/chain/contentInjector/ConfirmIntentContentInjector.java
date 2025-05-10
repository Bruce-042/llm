package com.bruce.youngman.chain.contentInjector;

import com.bruce.youngman.chain.prompts.PromptsProvider;
import dev.langchain4j.data.message.*;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;

import java.util.List;

/**
 * @author Liangyonghui
 * @since 2025/5/10 15:15
 */
public class ConfirmIntentContentInjector implements ContentInjector {
    @Override
    public ChatMessage inject(List<Content> list, ChatMessage chatMessage) {
        String text1 = PromptsProvider.confirmIndentPrompt(list).text();
        System.out.println("text1-->" + text1);
        System.out.println("chatMessage-->" + chatMessage);
        return new UserMessage(text1);
    }
}
