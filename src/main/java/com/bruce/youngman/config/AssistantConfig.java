package com.bruce.youngman.config;

import com.bruce.youngman.chain.contentInjector.ConfirmIntentContentInjector;
import com.bruce.youngman.chain.retriever.HybridRetriever;
import com.bruce.youngman.service.IntentYoungMan;
import com.bruce.youngman.service.YoungMan;
import com.bruce.youngman.util.EmbeddingUtil;
import com.bruce.youngman.util.MdDocumentSplitter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * @author Liangyonghui
 * @since 2025/5/9 16:19
 */
@Configuration
public class AssistantConfig {

    @Value("${youngman.api-key}")
    private String apiKey;

    @Value("${youngman.base-url}")
    private String baseUrl;

    @Value("${youngman.model-name}")
    private String modelName;

    @Bean
    public YoungMan youngMan() {

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore =
                EmbeddingUtil.embed2("documents/大模型TOP机型md.md", embeddingModel);

        ContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(2)
                .minScore(0.6)
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(DefaultContentInjector.builder().build())
                .build();

        ChatLanguageModel model = OpenAiChatModel.builder()
                .modelName(GPT_4_O_MINI)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();

        return AiServices.builder(YoungMan.class)
                .chatLanguageModel(model)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }


    @Bean
    public IntentYoungMan confirmIntentYoungMan() {
        MessageWindowChatMemory messageWindowChatMemory = MessageWindowChatMemory.withMaxMessages(10);

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();

        EmbeddingStore<TextSegment> embeddingStore =
                EmbeddingUtil.embed2("documents/大模型TOP机型md.md", embeddingModel);

        HybridRetriever contentRetriever = HybridRetriever.builder()
                .denseStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documents(MdDocumentSplitter.splitMarkdownByTitlesAndGetDocuments("documents/大模型TOP机型md.md"))
                .alpha(0.8f)  // 调整权重
                .topK(3)
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .contentInjector(new ConfirmIntentContentInjector(messageWindowChatMemory))
                .build();

        ChatLanguageModel model = OpenAiChatModel.builder()
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

        return AiServices.builder(IntentYoungMan.class)
                .chatLanguageModel(model)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemory(messageWindowChatMemory)
                .build();
    }


}
