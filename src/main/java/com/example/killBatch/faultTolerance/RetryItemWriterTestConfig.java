package com.example.killBatch.faultTolerance;

import com.example.killBatch.jpaBatch.Post;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Batch 기본 오류 처리
 *
 * 단, 한 건의 데이터만 실패하더라도 전체 배치를 실패시킨다.
 * 청크 지향 처리는 아이템 처리 중에서 예외 처리에 직접 개입할 수 없다.
 *
 * 해결책 -> 내결함성
 * 1. 재시도 - Retry
 * 2. 건너뛰기 - Skip
 *
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RetryItemWriterTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private static final List<Post> repository = List.of(
            new Post(1L, "test", "test", "test"),
            new Post(2L, "test", "test", "test"),
            new Post(3L, "test", "test", "test"),
            new Post(4L, "test", "test", "test"),
            new Post(5L, "test", "test", "test"),
            new Post(6L, "test", "test", "test"),
            new Post(7L, "test", "test", "test"),
            new Post(8L, "test", "test", "test"),
            new Post(9L, "test", "test", "test"),
            new Post(10L, "test", "test", "test")
            );

    @Bean
    public Job retryItemWriterTestJob(
            Step retryItemWriterTestStep
    ) {
        return new JobBuilder("retryItemWriterTestJob", jobRepository)
                .start(retryItemWriterTestStep)
                .build();
    }

    /**
     * 내결함성 기능 활성화 시, 내부적으로 RetryTemplate 장착
     * 1. canRetry() - 재시도 가능 여부 판단
     * 2. retryCallback() - 재시도 가능하다고 판단될 경우 호출
     *      - 청크 단위로 재시도!!
     *      - 롤백이 발생할 경우, 이미 Reader에서 읽어둔 데이터를 이용
     *      - ItemProcessor와 ItemWriter가 내부로 패키징!!
     *      - ItemReader까지 포함됨 (Batch 6.0 버전부터)!!
     * 3. recoveryCallback() - 재시도 불가능 할 경우 호출
     *      - 예외를 전파하거나 대체 로직 호출
     */

    /**
     * ItemWriter에서 예외가 발생 -> 청크 단위로 재시도 횟수 관리
     * -> 청크의 처음으로 돌아가 ItemProcessor -> ItemWriter 순으로 다시 수행
     */
    @Bean
    public Step retryItemWriterTestStep(
            ItemReader<Post> retryItemWriterTestReader,
            ItemProcessor<Post, Post> retryItemWriterTestProcessor,
            ItemWriter<Post> retryItemWriterTestWriter
    ) {
        return new StepBuilder("retryItemWriterTestStep", jobRepository)
                .<Post, Post>chunk(10, transactionManager)
                .reader(retryItemWriterTestReader)
                .processor(retryItemWriterTestProcessor)
                .writer(retryItemWriterTestWriter)
                .faultTolerant()
                .retry(TestException.class)
                .noRetry(NoRetryException.class)
                .retryLimit(3)
//                .processorNonTransactional()
                /**
                 * 재시도 간격 조정
                 */
                // 1초 간격으로 재시도
//                .backOffPolicy(new FixedBackOffPolicy() {{
//                    setBackOffPeriod(1000);
//                }})
                // 외부 시스템과의 통신 장애일 경우
                .backOffPolicy(new ExponentialBackOffPolicy() {{
                    setInitialInterval(1000L); //초기 대기 시간
                    setMultiplier(2.0);        //대기 시간 증가 배수
                    setMaxInterval(10000L);    //최대 대기 시간
                }})
                .build();
    }

    @Bean
    public ItemReader<Post> retryItemWriterTestReader() {
        return new ItemReader<Post>() {
            private int index = 0;

            @Override
            public Post read() {
                log.info("reader 진입 - 현재 인덱스: {}", index);

                if (index >= repository.size()) {
                    return null;
                }

                Post post = repository.get(index);
                index++;
                return post;
            }
        };
    }

    @Bean
    public ItemProcessor<Post, Post> retryItemWriterTestProcessor() {
        return new ItemProcessor<Post, Post>() {
            @Override
            public Post process(Post item) {
                log.info("id: {}", item.getId());
                return item;
            }
        };
    }

    @Bean
    public ItemWriter<Post> retryItemWriterTestWriter() {
        return new ItemWriter<Post>() {
            private static final int MAX_PATIENCE = 3;
            private int mercy = 0;  // 자비 카운트

            @Override
            public void write(Chunk<? extends Post> chunk) throws Exception {

                for (Post item : chunk) {
                    if (item.getId() == 7 & mercy < MAX_PATIENCE) {
                        mercy ++;
                        System.out.println(item.getId() + " -> ❌ 처형 실패.");
                        throw new TestException();
                    } else {
                        System.out.println(item.getId() + " -> ✅ 처형 완료");
                    }
                }
            }
        };
    }
}