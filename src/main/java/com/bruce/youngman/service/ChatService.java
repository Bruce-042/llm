package com.bruce.youngman.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.bruce.youngman.chain.retriever.HybridRetriever;
import com.bruce.youngman.chain.prompts.PromptsProvider;
import com.bruce.youngman.model.AnswerVO;
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

    public static int sureCount = 0;

    public static int uncertainCount = 0;

    public static int total = 0;

    @Resource
    private EmbeddingStore<TextSegment> embeddingStore;

    @Value("${youngman.api-key}")
    private String apiKey;

    @Value("${youngman.base-url}")
    private String baseUrl;

    @Value("${youngman.model-name}")
    private String modelName;


    public AnswerVO askYoungMan(String message) {
        String answer = youngMan.answer(message);
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

        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println(answer);
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        UserMessage userMessage = PromptsProvider.finalAnswerPrompt2(answer, message).toUserMessage();
        SystemMessage systemMessage = PromptsProvider.finalAnswerPrompt().toSystemMessage();
        String text = chatModel.chat(userMessage, systemMessage).aiMessage().text();
        return JSON.parseObject(text, AnswerVO.class);

    }

    public IntentVO confirmIndent(String message) {
        String answer = confirmIntentYoungMan.answer(message);

        try {
            JSONObject json = JSON.parseObject(answer);
            String intentResult = json.getString("intentResult");
            String intent = json.getString("intent");
            if ("明确".equals(intentResult)) {
                sureCount++;
            }
            total++;
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            System.out.println("提问:" + message);
            System.out.println("是否明确:" + intentResult);
            System.out.println("猜测询问:" + intent);
            System.out.println("明确数量：" + sureCount + "/" + total);
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
            IntentVO vo = new IntentVO();
            vo.setIntent(intent);
            vo.setIntendResult(intentResult);
            vo.setThoughtChain(json.getString("thoughtChain"));
            return vo;
        } catch (Exception e) {
            IntentVO vo = new IntentVO();
            vo.setIntent(answer);
            vo.setIntendResult("格式不正确");
            vo.setThoughtChain(answer);
            return vo;
        }
    }
}
