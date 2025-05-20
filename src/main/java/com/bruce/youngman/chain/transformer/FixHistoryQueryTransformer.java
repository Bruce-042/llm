package com.bruce.youngman.chain.transformer;

import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author Liangyonghui
 * @since 2025/5/18 14:10
 */
public class FixHistoryQueryTransformer implements QueryTransformer {

    private ChatLanguageModel chatModel;

    private MessageWindowChatMemory chatMemory;

    private String promptStr = "\"请阅读并理解用户与AI之间的对话内容。然后分析用户的新查询。从对话和新查询中识别所有相关细节、术语和上下文。将此查询重新表述为清晰、简洁且自包含的格式，使其适合信息检索。\n" +
            "\n" +
            "对话记录：\n" +
            "{{chatMemory}}\n" +
            "\n" +
            "用户查询：{{query}}\n" +
            "\n" +
            "非常重要：你只需提供重新表述后的查询内容，不要添加任何其他内容！不要在查询前添加任何前缀！\"\n" +
            "\n" +
            "（译文说明：保持技术文档的严谨性，同时符合中文表达习惯。关键要求部分使用加重语气处理，变量名保持原样以方便程序调用）";

    public FixHistoryQueryTransformer(ChatLanguageModel chatModel, MessageWindowChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
    }

    @Override
    public Collection<Query> transform(Query query) {
        List<ChatMessage> history = chatMemory.messages();
        // 首次提问一定是精确的
        if (CollectionUtils.isEmpty(history)) {
            System.out.println("==================查询1=====================");
            System.out.println(query);
            System.out.println("=======================================");
            return List.of(query);
        }
        // 第二次提问加入历史会话
        SystemMessage systemMessage = PromptTemplate.from(promptStr).apply(Map.of("chatMemory", history, "query", query.text())).toSystemMessage();
        ChatResponse chat = chatModel.chat(systemMessage);
        AiMessage aiMessage = chat.aiMessage();
        System.out.println("==================查询2=====================");
        System.out.println(Query.from(aiMessage.text()));
        System.out.println("=======================================");
        return List.of(Query.from(aiMessage.text()));
    }
}
