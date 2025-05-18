package com.bruce.youngman.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bruce.youngman.chain.retriever.HybridRetriever;
import com.bruce.youngman.chain.prompts.PromptsProvider;
import com.bruce.youngman.model.IntentVO;
import com.bruce.youngman.util.MdDocumentSplitter;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author Liangyonghui
 * @since 2025/5/9 16:00
 */
@Service
public class ChatService {

    @Resource
    private YoungMan youngMan;

    @Resource
    private IntentYoungMan confirmIntentYoungMan;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${youngman.api-key}")
    private String apiKey;

    @Value("${youngman.base-url}")
    private String baseUrl;

    @Value("${youngman.model-name}")
    private String modelName;


    public String askYoungMan(String message) {
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        HybridRetriever contentRetriever = HybridRetriever.builder()
                .denseStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documents(MdDocumentSplitter.splitMarkdownByTitlesAndGetDocuments("documents/大模型TOP机型md.md"))
                .alpha(0.8f)  // 调整权重
                .topK(3)
                .build();

        List<Content> contents = contentRetriever.retrieve(Query.from(message));

        System.out.println(contents);

        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .modelName(modelName)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .temperature(0D)
                .customHeaders(
                        Map.of("scene", "liangyonghui",
                                "token", "Yab31yR2frCnkwXbmHBpYwwKDJ3TnjtD",
                                "Accept", "application/json"
                        )
                )
                .build();


        AiMessage aiMessage = chatModel.chat(PromptsProvider.stepByStepAnalysisPrompt(contents, message).toAiMessage()).aiMessage();
        System.out.println("aiMessage========================================================================");
        System.out.println(aiMessage);
        System.out.println("aiMessage========================================================================");


        UserMessage userMessage = PromptsProvider.finalAnswerPrompt2(aiMessage.text(), message).toUserMessage();
        SystemMessage systemMessage = PromptsProvider.finalAnswerPrompt().toSystemMessage();
        return chatModel.chat(userMessage, systemMessage).aiMessage().text();

    }

    public IntentVO confirmIndent(String message) {
        String answer = confirmIntentYoungMan.answer(message);
        JSONObject json = JSON.parseObject(answer);
        IntentVO vo = new IntentVO();
        vo.setIntent(json.getString("intent"));
        vo.setIntendResult(json.getString("intentResult"));
        vo.setThoughtChain(json.getString("thoughtChain"));
        return vo;
    }
}
