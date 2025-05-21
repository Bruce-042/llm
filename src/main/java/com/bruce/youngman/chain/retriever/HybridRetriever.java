package com.bruce.youngman.chain.retriever;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 混合检索器（稠密检索 + 稀疏检索）
 * 继承自 ContentRetriever 实现 RAG 兼容接口
 */
public class HybridRetriever implements ContentRetriever {

    // region 配置参数
    private final EmbeddingStore<TextSegment> denseStore;
    private final EmbeddingModel embeddingModel;
    private final Analyzer analyzer;
    private final Directory sparseIndex;
    private final List<Document> documents;
    private final float alpha; // 稠密检索权重 (0-1)
    private final int topK;    // 返回结果数量
    // endregion
    private MessageWindowChatMemory messageWindowChatMemory;

    // region 构造器
    private HybridRetriever(Builder builder) {
        this.denseStore = builder.denseStore;
        this.embeddingModel = builder.embeddingModel;
        this.alpha = builder.alpha;
        this.topK = builder.topK;
        this.documents = builder.documents;
        this.messageWindowChatMemory = builder.messageWindowChatMemory;
        this.analyzer = new SmartChineseAnalyzer();
        this.sparseIndex = buildSparseIndex(builder.documents);
    }
    // endregion

    // region 核心检索方法
    @Override
    public List<Content> retrieve(dev.langchain4j.rag.query.Query query) {
        // 1. 并行执行双路检索
        List<EmbeddingMatch<TextSegment>> denseResults = denseRetrieve(query.text());
        List<TextSegment> sparseResults = sparseRetrieve(query.text());

        // 2. 归一化分数
        normalizeScores(denseResults, sparseResults);

        // 3. 融合排序
        return hybridRanking(denseResults, sparseResults);
    }


    /**
     * 稠密检索（基于向量相似度）
     */
    private List<EmbeddingMatch<TextSegment>> denseRetrieve(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        EmbeddingSearchRequest embeddingSearchRequest = EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding).maxResults(topK).build();
        EmbeddingSearchResult<TextSegment> search = denseStore.search(embeddingSearchRequest);
        return search.matches();
    }

    /**
     * 稀疏检索（基于BM25算法）
     */
    private List<TextSegment> sparseRetrieve(String query) {
        try (IndexReader reader = DirectoryReader.open(sparseIndex)) {
            IndexSearcher searcher = new IndexSearcher(reader);
            QueryParser parser = new QueryParser("content", analyzer);
            
            // Preprocess query to escape special characters
            String processedQuery = query.replaceAll("[\\[\\](){}^\"~*?:\\\\]", "\\\\$0");
            Query luceneQuery = parser.parse(processedQuery);

            TopDocs topDocs = searcher.search(luceneQuery, topK);
            return Arrays.stream(topDocs.scoreDocs)
                    .map(scoreDoc -> toTextSegment(searcher, scoreDoc))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Sparse retrieval failed", e);
        }
    }

    /**
     * 构建稀疏检索索引
     */
    private Directory buildSparseIndex(List<Document> docs) {
        try {
            Directory directory = new ByteBuffersDirectory();
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            try (IndexWriter writer = new IndexWriter(directory, config)) {
                for (int i = 0; i < docs.size(); i++) {
                    org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();
                    luceneDoc.add(new TextField("content", docs.get(i).text(), Field.Store.YES));
                    luceneDoc.add(new TextField("id", String.valueOf(i), Field.Store.YES));
                    writer.addDocument(luceneDoc);
                }
            }
            return directory;
        } catch (IOException e) {
            throw new RuntimeException("Failed to build sparse index", e);
        }
    }

    /**
     * 分数归一化处理
     */
    private void normalizeScores(List<EmbeddingMatch<TextSegment>> denseResults,
                                 List<TextSegment> sparseResults) {
        // 稠密结果归一化 (原始分数已经是0-1范围)

        // 稀疏结果归一化
        OptionalDouble maxSparseScore = sparseResults.stream()
                .mapToDouble(seg -> Double.parseDouble(Objects.requireNonNull(seg.metadata().getString("sparse_score"))))
                .max();

        if (maxSparseScore.isPresent()) {
            sparseResults.forEach(seg -> {
                double normalized = Double.parseDouble(Objects.requireNonNull(seg.metadata().getString("sparse_score"))) / maxSparseScore.getAsDouble();
                seg.metadata().put("sparse_score", String.valueOf(normalized));
            });
        }
    }

    /**
     * 混合排序算法
     */
    private List<Content> hybridRanking(List<EmbeddingMatch<TextSegment>> denseResults,
                                        List<TextSegment> sparseResults) {
        // 合并结果并计算混合分数
        Map<String, HybridScore> scoreMap = new HashMap<>();

        // 处理稠密结果
        denseResults.forEach(match -> {
            String docId = match.embedded().metadata().getString("doc_id");
            scoreMap.putIfAbsent(docId, new HybridScore());
            scoreMap.get(docId).denseScore = match.score();
        });

        // 处理稀疏结果
        sparseResults.forEach(seg -> {
            String docId = seg.metadata().getString("doc_id");
            scoreMap.putIfAbsent(docId, new HybridScore());
            scoreMap.get(docId).sparseScore = Double.parseDouble(Objects.requireNonNull(seg.metadata().getString("sparse_score")));
        });

        // 生成最终结果
        return scoreMap.entrySet().stream()
                .filter(entry -> entry.getKey() != null)
                .map(entry -> createScoredSegment(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingDouble(seg -> -Double.parseDouble(Objects.requireNonNull(seg.metadata().getString("hybrid_score")))))
                .limit(topK)
                .map(Content::from)
                .collect(Collectors.toList());
    }

    /**
     * 创建带分数的TextSegment
     */
    private TextSegment createScoredSegment(String docId, HybridScore score) {
        Document originalDoc = documents.get(Integer.parseInt(docId));
        TextSegment segment = TextSegment.from(originalDoc.text(), originalDoc.metadata());

        // 计算混合分数
        double hybridScore = alpha * score.denseScore + (1 - alpha) * score.sparseScore;

        // 添加元数据
        segment.metadata().put("dense_score", String.valueOf(score.denseScore));
        segment.metadata().put("sparse_score", String.valueOf(score.sparseScore));
        segment.metadata().put("hybrid_score", String.valueOf(hybridScore));
        segment.metadata().put("doc_id", docId);

        return segment;
    }

    /**
     * Lucene结果转换为TextSegment
     */
    private TextSegment toTextSegment(IndexSearcher searcher, ScoreDoc scoreDoc) {
        try {
            org.apache.lucene.document.Document doc = searcher.doc(scoreDoc.doc);
            String docId = doc.get("id");
            Document originalDoc = documents.get(Integer.parseInt(docId));

            TextSegment segment = TextSegment.from(doc.get("content"), originalDoc.metadata());
            segment.metadata().put("sparse_score", String.valueOf(scoreDoc.score));
            segment.metadata().put("doc_id", docId);

            return segment;
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert Lucene document", e);
        }
    }
    // endregion

    // region 构建器模式
    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        private EmbeddingStore<TextSegment> denseStore;
        private EmbeddingModel embeddingModel;
        private List<Document> documents;
        private float alpha = 0.7f;
        private int topK = 5;

        private MessageWindowChatMemory messageWindowChatMemory;
        public Builder denseStore(EmbeddingStore<TextSegment> denseStore) {
            this.denseStore = denseStore;
            return this;
        }

        public Builder messageWindowChatMemory(MessageWindowChatMemory messageWindowChatMemory) {
            this.messageWindowChatMemory = messageWindowChatMemory;
            return this;
        }


        public Builder embeddingModel(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }

        public Builder documents(List<Document> documents) {
            this.documents = documents;
            return this;
        }

        public Builder alpha(float alpha) {
            this.alpha = alpha;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }


        public HybridRetriever build() {
            validate();
            return new HybridRetriever(this);
        }

        private void validate() {
            if (denseStore == null) {
                throw new IllegalArgumentException("Dense store must be set");
            }
            if (embeddingModel == null) {
                throw new IllegalArgumentException("Embedding model must be set");
            }
            if (documents == null || documents.isEmpty()) {
                throw new IllegalArgumentException("Documents must not be empty");
            }
            if (alpha < 0 || alpha > 1) {
                throw new IllegalArgumentException("Alpha must be between 0 and 1");
            }
            if (topK <= 0) {
                throw new IllegalArgumentException("TopK must be positive");
            }
        }
    }
    // endregion

    // region 内部类
    private static class HybridScore {
        double denseScore;
        double sparseScore;
    }
    // endregion
}