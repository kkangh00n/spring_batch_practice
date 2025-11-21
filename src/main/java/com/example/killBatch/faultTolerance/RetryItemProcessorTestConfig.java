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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
public class RetryItemProcessorTestConfig {

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
    public Job retryItemProcessorTestJob(
            Step retryItemProcessorTestStep
    ) {
        return new JobBuilder("retryItemProcessorTestJob", jobRepository)
                .start(retryItemProcessorTestStep)
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
     * ItemProcessor에서 예외가 발생 -> 아이템 단위로 재시도 관리
     * -> ItemProcessor부터 처리가 재개
     */
    @Bean
    public Step retryItemProcessorTestStep(
            ItemReader<Post> retryItemProcessorTestReader,
            ItemProcessor<Post, Post> retryItemProcessorTestProcessor,
            ItemWriter<Post> retryItemProcessorTestWriter
    ) {
        return new StepBuilder("retryItemProcessorTestStep", jobRepository)
                .<Post, Post>chunk(10, transactionManager)
                .reader(retryItemProcessorTestReader)
                .processor(retryItemProcessorTestProcessor)
                .writer(retryItemProcessorTestWriter)
                //내결함성 기능 ON
                .faultTolerant()
                //재시도 대상 예외 추가
                .retry(TestException.class)
                .noRetry(NoRetryException.class)
                //재시도 횟수
                // -> 첫 번째 호출을 포함해서 3번
                // -> 실제 재시도 횟수는 2번!
                .retryLimit(3)
                //해당 옵션 설정 시, 청크 내에서 이미 처리 된 아이템은 다시 처리하지 않음
                //BUT, 예외가 발생하면, 여전히 청크 단위의 트랜잭션 롤백
                //ItemProcessor에서 재시도 시, 결과가 멱등하지 않다면 해당 옵션을 사용하자!!
//                .processorNonTransactional()
                .build();
    }

    @Bean
    public ItemReader<Post> retryItemProcessorTestReader() {
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
    public ItemProcessor<Post, Post> retryItemProcessorTestProcessor() {
        return new ItemProcessor<Post, Post>() {
            private static final int MAX_PATIENCE = 3;
            private int mercy = 0;  // 자비 카운트

            @Override
            public Post process(Post item) {
                if (item.getId() == 7 & mercy < MAX_PATIENCE) {
                    mercy ++;
                    System.out.println(item.getId() + " -> ❌ 처형 실패.");
                    throw new TestException();
                } else {
                    System.out.println(item.getId() + " -> ✅ 처형 완료");
                }
                return item;
            }
        };
    }

    @Bean
    public ItemWriter<Post> retryItemProcessorTestWriter() {
        return items -> {
            log.info("writer 진입");
            items.forEach(post -> log.info("item 처리"));
        };
    }
}