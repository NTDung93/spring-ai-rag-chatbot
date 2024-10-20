package dev.ntd.spring_ai_rag_chatbot.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.retry.support.RetryTemplate;

/**
 *
 * @author ntig
 */
@Configuration
public class RagConfiguration {
    private static Logger logger = Logger.getLogger(RagConfiguration.class);

    @Value("classpath:/docs/greenz-faq.txt")
    private Resource faq;

    @Value("vectorstore.json")
    private String vectorStoreName;

    @Bean
    public RetryTemplate retryTemplate() {
        return RetryTemplate.builder()
                .exponentialBackoff(1000, 1.5, 5000)
                .maxAttempts(5)
                .build();
    }

    @Bean
    SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel) throws IOException {
        SimpleVectorStore simpleVectorStore = new SimpleVectorStore(embeddingModel);
        try {
            File vectorStoreFile = getVectorStoreFile();
            if (vectorStoreFile.exists()) {
                logger.info("Vector Store File Exists");
                simpleVectorStore.load(vectorStoreFile);
            } else {
                logger.info("Vector Store File Does Not Exist, loading documents");
                TextReader textReader = new TextReader(faq);
                textReader.getCustomMetadata().put("filename", "greenz-faq.txt");
                List<Document> documents = textReader.get();
                TextSplitter textSplitter = new TokenTextSplitter();
                List<Document> splitDocuments = textSplitter.apply(documents);
                simpleVectorStore.add(splitDocuments);
                simpleVectorStore.save(vectorStoreFile);
            }
        } catch (Exception e) {
            logger.error("Error loading vector store", e);
        }
        return simpleVectorStore;
    }

    private File getVectorStoreFile() {
        Path path = Paths.get("src", "main", "resources", "data");
        String absolutePath = path.toFile().getAbsolutePath() + "/" + vectorStoreName;
        return new File(absolutePath);
    }
}
