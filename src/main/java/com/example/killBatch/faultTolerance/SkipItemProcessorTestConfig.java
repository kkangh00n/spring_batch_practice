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
public class SkipItemProcessorTestConfig {

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
    public Job skipItemProcessorTestJob(
            Step skipItemProcessorTestStep
    ) {
        return new JobBuilder("skipItemProcessorTestJob", jobRepository)
                .start(skipItemProcessorTestStep)
                .build();
    }

    /**
     * 1. ItemProcessor에서 예외가 발생
     * 2. 청크 트랜잭션 롤백
     * 3. 다시 예외가 발생한 item 접근 시, recoveryCallback 호출
     * 4. 청크 단위로 processor 처리 재시작
     */
    @Bean
    public Step skipItemProcessorTestStep(
            ItemReader<Post> skipItemProcessorTestReader,
            ItemProcessor<Post, Post> skipItemProcessorTestProcessor,
            ItemWriter<Post> skipItemProcessorTestWriter
    ) {
        return new StepBuilder("skipItemProcessorTestStep", jobRepository)
                .<Post, Post>chunk(10, transactionManager)
                .reader(skipItemProcessorTestReader)
                .processor(skipItemProcessorTestProcessor)
                .writer(skipItemProcessorTestWriter)
                //내결함성 기능 ON
                .faultTolerant()
                .skip(TestException.class)
                .skipLimit(2)
                .build();
    }

    @Bean
    public ItemReader<Post> skipItemProcessorTestReader() {
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
    public ItemProcessor<Post, Post> skipItemProcessorTestProcessor() {
        return new ItemProcessor<Post, Post>() {
            private static final int MAX_PATIENCE = 3;
            private int mercy = 0;  // 자비 카운트

            @Override
            public Post process(Post item) {
                if ((item.getId()==3 || item.getId() == 7) && mercy < MAX_PATIENCE) {
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
    public ItemWriter<Post> skipItemProcessorTestWriter() {
        return items -> {
            log.info("writer 진입");
            items.forEach(post -> log.info("item 처리"));
        };
    }
}