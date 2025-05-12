package com.bruce.youngman.config;

import com.bruce.youngman.util.EmbeddingUtil;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Liangyonghui
 * @since 2025/5/9 16:04
 */
@Configuration
public class EmbeddingConfig {


    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return EmbeddingUtil.embed2("documents/大模型TOP机型md.md",  new BgeSmallEnV15QuantizedEmbeddingModel());
    }

}
