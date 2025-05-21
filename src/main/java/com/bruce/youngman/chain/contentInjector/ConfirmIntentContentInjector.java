package com.bruce.youngman.chain.contentInjector;

import com.bruce.youngman.chain.prompts.PromptsProvider;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;

import java.util.List;

/**
 * @author Liangyonghui
 * @since 2025/5/10 15:15
 */
public class ConfirmIntentContentInjector implements ContentInjector {

    private MessageWindowChatMemory chatMemory;
    public ConfirmIntentContentInjector(MessageWindowChatMemory messageWindowChatMemory) {
        this.chatMemory = messageWindowChatMemory;
    }

    @Override
    public ChatMessage inject(List<Content> list, ChatMessage chatMessage) {
        List<ChatMessage> messages = chatMemory.messages();
        // todo 后续改回来
        UserMessage userMessage = PromptsProvider.confirmIntentPrompt(list, chatMessage).toUserMessage();
        System.out.println("++++++++++++++++++++++++++userMessage++++++++++++++++++++++++++++++++");
        System.out.println(userMessage.singleText());
        System.out.println("++++++++++++++++++++++++++userMessage++++++++++++++++++++++++++++++++");


        return userMessage;
    }
}
