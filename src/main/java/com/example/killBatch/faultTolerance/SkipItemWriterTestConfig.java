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
public class SkipItemWriterTestConfig {

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
    public Job skipItemWriterTestJob(
            Step skipItemWriterTestStep
    ) {
        return new JobBuilder("skipItemWriterTestJob", jobRepository)
                .start(skipItemWriterTestStep)
                .build();
    }

    /**
     * 1. ItemWriter에서 예외가 발생
     * 2. catRetry() -> 재시도 불가능 판단, 청크 트랜잭션 롤백
     * 3. 스캔모드 돌입!!
     *      - 스캔 모드란? 청크 내 아이템을 하나씩 개별 처리하는 특수 모드
     *      - Item을 처리 후, 개별적으로 쓰고 커밋
     *      - 단일 아이템 쓰기 중 예외가 발생하면 해당 아이템을 skip
     * 4. 청크 내에서 문제가 되는 특정 아이템만 정확히 식별 후 건너 뜀
     */
    @Bean
    public Step skipItemWriterTestStep(
            ItemReader<Post> skipItemWriterTestReader,
            ItemProcessor<Post, Post> skipItemWriterTestProcessor,
            ItemWriter<Post> skipItemWriterTestWriter
    ) {
        return new StepBuilder("skipItemWriterTestStep", jobRepository)
                .<Post, Post>chunk(10, transactionManager)
                .reader(skipItemWriterTestReader)
                .processor(skipItemWriterTestProcessor)
                .writer(skipItemWriterTestWriter)
                .faultTolerant()
                .skip(TestException.class)
                .skipLimit(2)

                /**
                 * noRollback
                 *
                 * 1. Reader에서 처리
                 *      - skip이 가능하다면 건너뜀
                 *      - skip이 불가능하다면 noRollback
                 *      - (skip과 noRollback이 둘다 설정되어있을 경우, skip 우선)
                 * 2. Processor에서 처리
                 *      - noRollback이 아닐 경우, 청크 트랜잭션 롤백 -> 기존의 skip 처리
                 *      - noRollback일 경우, 청크 트랜잭션 롤백하지 않음 -> 해당 item 건너뜀
                 *
                 *      - processor에서는 skip과 noRollback이 모두 설정되어 있어야 함!!
                 * 3. Writer에서 처리
                 *      - 스캔 모드의 개별 쓰기 작업 중 발생
                 *      - skip이 가능할 경우, skip 우선
                 *      - skip 처리에 포함되지 않은 경우, 없던 일 처리
                 *
                 *      - 핵심: skip과 noRollback 둘 다 설정 시, skip만 동작
                 */
                .noRollback(TestException.class)
                .build();
    }

    @Bean
    public ItemReader<Post> skipItemWriterTestReader() {
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
    public ItemProcessor<Post, Post> skipItemWriterTestProcessor() {
        return new ItemProcessor<Post, Post>() {
            @Override
            public Post process(Post item) {
                log.info("id: {}", item.getId());
                return item;
            }
        };
    }

    @Bean
    public ItemWriter<Post> skipItemWriterTestWriter() {
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