package com.example.killBatch.ItemProcessor;

import com.example.killBatch.jpaBatch.Post;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ItemProcessorTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job itemProcessorTestJob(Step itemProcessorTestStep) {
        return new JobBuilder("itemProcessorTestJob", jobRepository)
                .start(itemProcessorTestStep)
                .build();
    }

    @Bean
    public Step itemProcessorTestStep(
            JpaCursorItemReader<Post> itemProcessorTestReader,
            NullReturnProcessor itemProcessor,
            ItemWriter<Post> itemProcessorTestWriter
    ) {
        return new StepBuilder("itemProcessorTestStep", jobRepository)
                .<Post, Post>chunk(5, transactionManager)
                .reader(itemProcessorTestReader)
                .processor(itemProcessor)
                .writer(itemProcessorTestWriter)
                .build();
    }

    @Bean
    public JpaCursorItemReader<Post> itemProcessorTestReader() {
        return new JpaCursorItemReaderBuilder<Post>()
                .name("itemProcessorTestReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
                        SELECT p FROM Post p JOIN FETCH p.reports r
                        """)
                .build();
    }


    @Bean
    public ItemWriter<Post> itemProcessorTestWriter() {
        return items -> items.forEach(post -> {
            log.info("💀 TERMINATED: [ID:{}] '{}' by {}",
                    post.getId(),
                    post.getTitle(),
                    post.getWriter());
        });
    }
}