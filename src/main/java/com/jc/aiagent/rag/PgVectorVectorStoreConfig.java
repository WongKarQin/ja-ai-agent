package com.jc.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Slf4j
// 为方便开发调试和部署，临时注释，如果需要使用 PgVector 存储知识库，取消注释即可
@Configuration
public class PgVectorVectorStoreConfig {

    private static final String SCHEMA_NAME = "public";
    private static final String VECTOR_TABLE_NAME = "vector_store";
    private static final int EMBEDDING_BATCH_SIZE = 10;

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    public VectorStore pgVectorVectorStore(@Qualifier("postgresJdbcTemplate") JdbcTemplate jdbcTemplate,
                                           EmbeddingModel dashscopeEmbeddingModel) {
        boolean tableExists = tableExists(jdbcTemplate, SCHEMA_NAME, VECTOR_TABLE_NAME);
        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1024)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(!tableExists)
                .schemaName(SCHEMA_NAME)
                .vectorTableName(VECTOR_TABLE_NAME)
                .maxDocumentBatchSize(10000)
                .build();

        initDocumentsIfNeeded(jdbcTemplate, vectorStore, tableExists);
        return vectorStore;
    }

    private void initDocumentsIfNeeded(JdbcTemplate jdbcTemplate, PgVectorStore vectorStore, boolean tableExists) {
        if (!tableExists) {
            vectorStore.afterPropertiesSet();
        } else {
            log.info("向量表 {}.{} 已存在，跳过建表", SCHEMA_NAME, VECTOR_TABLE_NAME);
        }

        if (tableHasData(jdbcTemplate, SCHEMA_NAME, VECTOR_TABLE_NAME)) {
            log.info("向量表 {}.{} 已有数据，跳过文档初始化", SCHEMA_NAME, VECTOR_TABLE_NAME);
            return;
        }

        List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
        if (documents.isEmpty()) {
            log.warn("未加载到任何 Markdown 文档，跳过向量入库");
            return;
        }
        batchAddDocuments(vectorStore, documents, EMBEDDING_BATCH_SIZE);
        log.info("向量表 {}.{} 文档初始化完成，共写入 {} 条", SCHEMA_NAME, VECTOR_TABLE_NAME, documents.size());
    }

    private void batchAddDocuments(VectorStore vectorStore, List<Document> documents, int batchSize) {
        for (int i = 0; i < documents.size(); i += batchSize) {
            int end = Math.min(i + batchSize, documents.size());
            vectorStore.add(documents.subList(i, end));
        }
    }

    private boolean tableHasData(JdbcTemplate jdbcTemplate, String schemaName, String tableName) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM %s.%s".formatted(schemaName, tableName),
                Long.class);
        return count != null && count > 0;
    }

    private boolean tableExists(JdbcTemplate jdbcTemplate, String schemaName, String tableName) {
        Boolean exists = jdbcTemplate.queryForObject("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = ?
                      AND table_name = ?
                )
                """, Boolean.class, schemaName, tableName);
        return Boolean.TRUE.equals(exists);
    }
}
