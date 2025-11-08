package com.example.killBatch.jpaBatch;

import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JpaCursorItemReaderTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job jpaCursorItemReaderTestJob(Step jpaCursorItemReaderTestStep) {
        return new JobBuilder("jpaCursorItemReaderTestJob", jobRepository)
                .start(jpaCursorItemReaderTestStep)
                .build();
    }

    @Bean
    public Step jpaCursorItemReaderTestStep(
            JpaCursorItemReader<Post> jpaCursorItemReader,
            PostBlockProcessor postBlockProcessor,
            ItemWriter<BlockedPost> jpaCursorItemReaderTestWriter
    ) {
        return new StepBuilder("jpaCursorItemReaderTestStep", jobRepository)
                .<Post, BlockedPost>chunk(5, transactionManager)
                .reader(jpaCursorItemReader)
                .processor(postBlockProcessor)
                .writer(jpaCursorItemReaderTestWriter)
                .build();
    }

    /**
     * JpaCursorItemReader - 스트리밍 처리
     * 1. EntityManagerFactory - EntityManager 생성
     * 2. 개별적으로 만든 EntityManager와 JpaQueryProvider를 통해 Query 객체 생성
     * 3. Query 객체 - 커서 순회로 실제 데이터를 한 건씩 가져옴
     * 4. hasNext() - 다음 데이터가 존재하는지 여부 확인
     *
     * 영속성 컨텍스트 생명주기
     * 준영속 상태 - 데이터를 읽는 즉시 detach
     *
     * JdbcCursorItemReader 공통점 - 동작 및 특징은 JdbcCursorItemReader와 동일
     * 차이점 - DataSource가 아닌 EntityManager를 통해 데이터를 읽어옴
     *
     */
    @Bean
    @StepScope
    public JpaCursorItemReader<Post> jpaCursorItemReader(
            @Value("#{jobParameters['startDateTime']}") LocalDateTime startDateTime,
            @Value("#{jobParameters['endDateTime']}") LocalDateTime endDateTime
    ) {
        return new JpaCursorItemReaderBuilder<Post>()
                .name("jpaCursorItemReader")
                // AutoConfiguration EntityManagerFactory - 청크 트랜잭션과 무관한 EntityManager 생성
                .entityManagerFactory(entityManagerFactory)
                // JPQL 전달
                .queryString("""
                        SELECT p FROM Post p JOIN FETCH p.reports r
                        WHERE r.reportedAt >= :startDateTime AND r.reportedAt < :endDateTime
                        """)
                .parameterValues(Map.of(
                        "startDateTime", startDateTime,
                        "endDateTime", endDateTime
                ))
                .build();
    }

    @Bean
    public ItemWriter<BlockedPost> jpaCursorItemReaderTestWriter() {
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

    @Component
    public static class PostBlockProcessor implements ItemProcessor<Post, BlockedPost> {

        @Override
        public BlockedPost process(Post post) {
            // 각 신고의 신뢰도를 기반으로 차단 점수 계산
            double blockScore = calculateBlockScore(post.getReports());

            // 차단 점수가 기준치를 넘으면 처형 결정
            if (blockScore >= 7.0) {
                return BlockedPost.builder()
                        .postId(post.getId())
                        .writer(post.getWriter())
                        .title(post.getTitle())
                        .reportCount(post.getReports().size())
                        .blockScore(blockScore)
                        .blockedAt(LocalDateTime.now())
                        .build();
            }

            return null;  // 무죄 방면
        }

        private double calculateBlockScore(List<Report> reports) {
            // 각 신고들의 정보를 시그니처에 포함시켜 마치 사용하는 것처럼 보이지만...
            for (Report report : reports) {
                analyzeReportType(report.getReportType());            // 신고 유형 분석
                checkReporterTrust(report.getReporterLevel());        // 신고자 신뢰도 확인
                validateEvidence(report.getEvidenceData());           // 증거 데이터 검증
                calculateTimeValidity(report.getReportedAt());        // 시간 가중치 계산
            }

            // 실제로는 그냥 랜덤 값을 반환
            return Math.random() * 10;  // 0~10 사이의 랜덤 값
        }

        // 아래는 실제로는 아무것도 하지 않는 메서드들
        private void analyzeReportType(String reportType) {
            // 신고 유형 분석하는 척
        }

        private void checkReporterTrust(int reporterLevel) {
            // 신고자 신뢰도 확인하는 척
        }

        private void validateEvidence(String evidenceData) {
            // 증거 검증하는 척
        }

        private void calculateTimeValidity(LocalDateTime reportedAt) {
            // 시간 가중치 계산하는 척
        }
    }
}
