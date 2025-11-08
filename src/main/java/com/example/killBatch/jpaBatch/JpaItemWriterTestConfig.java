package com.example.killBatch.jpaBatch;

import com.example.killBatch.jpaBatch.JpaCursorItemReaderTestConfig.PostBlockProcessor;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JpaItemWriterTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job jpaItemWriterTestJob(Step jpaItemWriterTestStep) {
        return new JobBuilder("jpaItemWriterTestJob", jobRepository)
                .start(jpaItemWriterTestStep)
                .build();
    }

    @Bean
    public Step jpaItemWriterTestStep(
            JpaPagingItemReader<Post> jpaItemWriterTestReader,
            PostBlockProcessor postBlockProcessor,
            ItemWriter<BlockedPost> jpaItemWriter
    ) {
        return new StepBuilder("jpaItemWriterTestStep", jobRepository)
                .<Post, BlockedPost>chunk(5, transactionManager)
                .reader(jpaItemWriterTestReader)
                .processor(postBlockProcessor)
                .writer(jpaItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Post> jpaItemWriterTestReader(
            @Value("#{jobParameters['startDateTime']}") LocalDateTime startDateTime,
            @Value("#{jobParameters['endDateTime']}") LocalDateTime endDateTime
    ) {
        return new JpaPagingItemReaderBuilder<Post>()
                .name("jpaItemWriterTestReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("""
                    SELECT DISTINCT p FROM Post p
                    JOIN p.reports r
                    WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime
                    ORDER BY p.id ASC
                    """)
                .parameterValues(Map.of(
                        "startDateTime", startDateTime,
                        "endDateTime", endDateTime
                ))
                .pageSize(5)
                .transacted(false)
                .build();
    }

    /**
     * JpaItemWriter
     *
     * 주의사항 - 엔티티를 수정할 경우
     * 1. JpaPagingItemReader 사용 X -> flush() 호출 때문에 위험
     * 2. 영속성 컨텍스트에 해당 데이터 존재 여부 알 수 없음 (준영속 상태)
     *      -> 따라서 chunk 크기 만큼 select 쿼리 후, bulkUpdate
     */
    @Bean
    public JpaItemWriter<BlockedPost> jpaItemWriter() {
        return new JpaItemWriterBuilder<BlockedPost>()
                .entityManagerFactory(entityManagerFactory)
                //true -> persist (새로운 데이터 추가)
                //false -> merge (기존 데이터 수정)
                .usePersist(true)
                .build();
    }

}
