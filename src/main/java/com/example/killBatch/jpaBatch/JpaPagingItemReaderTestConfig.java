package com.example.killBatch.jpaBatch;

import com.example.killBatch.jpaBatch.JpaCursorItemReaderTestConfig.PostBlockProcessor;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JpaPagingItemReaderTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job jpaPagingItemReaderTestJob(Step jpaPagingItemReaderTestStep) {
        return new JobBuilder("jpaPagingItemReaderTestJob", jobRepository)
                .start(jpaPagingItemReaderTestStep)
                .build();
    }

    @Bean
    public Step jpaPagingItemReaderTestStep(
            JpaPagingItemReader<Post> jpaPagingItemReader,
            PostBlockProcessor postBlockProcessor,
            ItemWriter<BlockedPost> jpaPagingItemReaderTestWriter
    ) {
        return new StepBuilder("jpaPagingItemReaderTestStep", jobRepository)
                .<Post, BlockedPost>chunk(5, transactionManager)
                .reader(jpaPagingItemReader)
                .processor(postBlockProcessor)
                .writer(jpaPagingItemReaderTestWriter)
                .build();
    }

    /**
     * JpaPagingItemReader
     * offset 방식 동작 - offset이 클수록 DB 메모리 사용량 증가
     *
     * 1. EntityManagerFactory - EntityManager 생성
     * 2. 메서드 호출마다 새로운 페이징 쿼리를 생성하고 실행
     *
     * 영속성 컨텍스트 생명주기
     * 준영속 상태 - 데이터를 읽는 즉시 detach
     *
     * 쿼리 형식
     * SELECT *
     * FROM victims
     * ORDER BY id
     * LIMIT 10
     * OFFSET 0
     */

    /**
     * 주의사항
     * doReadPage() - 메서드는 페이지를 읽기 전후로 새로운 트랜잭션을 시작, 데이터를 가져온 후 바로 커밋
     *
     * 왜 문제인가?
     * 1. 엔티티의 @BatchSize 미동작 -> LAZY 컬렉션 연관객체 탐색 시 N+1
     * 2. 이전 Step의 processor에서 엔티티 수정 -> 다음 Step의 reader에서 데이터 읽고 flush -> DB에 변경사항 반영 가능성
     *
     * 해결책 1
     * transacted(false) -> JpaPagingItemReader 트랜잭션 비활성화
     * processor, writer -> Lazy Loading 불가
     *
     * 해결책 2
     * Lazy -> Eager
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<Post> jpaPagingItemReader(
            @Value("#{jobParameters['startDateTime']}") LocalDateTime startDateTime,
            @Value("#{jobParameters['endDateTime']}") LocalDateTime endDateTime
    ) {
        return new JpaPagingItemReaderBuilder<Post>()
                .name("jpaPagingItemReader")
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
                //JpaPagingItemReader 트랜잭션 비활성화
                .transacted(false)
                .build();
    }

    @Bean
    public ItemWriter<BlockedPost> jpaPagingItemReaderTestWriter() {
        return items -> items.forEach(blockedPost -> {
            log.info("💀 TERMINATED: [ID:{}] '{}' by {} | 신고:{}건 | 점수:{} | kill -9 at {}",
                    blockedPost.getPostId(),
                    blockedPost.getTitle(),
                    blockedPost.getWriter(),
                    blockedPost.getReportCount(),
                    String.format("%.2f", blockedPost.getBlockScore()),
                    blockedPost.getBlockedAt().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        });
    }

}
