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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.batch.item.validator.ValidatingItemProcessor;
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
            ItemProcessor<Post, Post> itemPostNullFilterProcessor,
            ItemWriter<Post> itemProcessorTestWriter
    ) {
        return new StepBuilder("itemProcessorTestStep", jobRepository)
                .<Post, Post>chunk(5, transactionManager)
                .reader(itemProcessorTestReader)
                .processor(itemPostNullFilterProcessor)
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

    /**
     * ItemProcessor
     *
     * 1-2. Validator를 이용한 필터링
     *
     * 2. 데이터 검증을 통한 실패 처리
     * - Step과 Job 모두 실패
     */
    @Bean
    public ItemProcessor<Post, Post> itemPostNullFilterProcessor(
            FilteringValidator filteringValidator
    ) {
        //validator 주입
        ValidatingItemProcessor<Post> validationItemProcessor = new ValidatingItemProcessor<>(
                filteringValidator);
        //필터링 수행
        // true -> 필터링
        // false -> 실패 처리
        validationItemProcessor.setFilter(false);

        return validationItemProcessor;
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