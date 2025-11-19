package com.example.killBatch.itemListener;

import com.example.killBatch.itemProcessor.FilteringValidator;
import com.example.killBatch.jpaBatch.Post;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 인터페이스 구현
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ListenerTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private static final List<Post> repository = List.of(new Post(), new Post());

    @Bean
    public Job listenerTestJob(
            Step listenerTestStep,
            JobExecutionListener customJobListener
    ) {
        return new JobBuilder("listenerTestJob", jobRepository)
                .start(listenerTestStep)
                .listener(customJobListener)
                .build();
    }

    @Bean
    public Step listenerTestStep(
            ItemReader<Post> listenerTestReader,
            ItemProcessor<Post, Post> listenerTestProcessor,
            ItemWriter<Post> listenerTestWriter,
            StepExecutionListener customStepListener,
            ChunkListener customChunkListener,
            ItemReadListener<Post> customItemReadListener,
            ItemProcessListener<Post, Post> customItemProcessListener,
            ItemWriteListener<Post> customItemWriteListener
    ) {
        return new StepBuilder("listenerTestStep", jobRepository)
                .<Post, Post>chunk(2, transactionManager)
                .reader(listenerTestReader)
                .processor(listenerTestProcessor)
                .writer(listenerTestWriter)
                .listener(customStepListener)
                .listener(customChunkListener)
                .listener(customItemReadListener)
                .listener(customItemProcessListener)
                .listener(customItemWriteListener)
                .build();
    }

    @Bean
    public ItemReader<Post> listenerTestReader() {
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
    public ItemProcessor<Post, Post> listenerTestProcessor(
            FilteringValidator filteringValidator
    ) {
        return item -> {
            log.info("processor 진입");
            return item;
        };
    }

    @Bean
    public ItemWriter<Post> listenerTestWriter() {
        return items -> {
            log.info("writer 진입");
            items.forEach(post -> log.info("item 처리"));
        };
    }


    /**
     * JobExecutionListener
     *
     */
    @Bean
    public JobExecutionListener customJobListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓");
                log.info("┃  🚀 JOB 시작: {}  ", jobExecution.getJobInstance().getJobName());
                log.info("┃  Job ID: {}", jobExecution.getJobId());
                log.info("┃  Job Parameters: {}", jobExecution.getJobParameters());
                log.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛");
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                long executionTime = Duration.between(
                        jobExecution.getStartTime(),
                        jobExecution.getEndTime()
                ).toMillis();

                log.info("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓");
                log.info("┃  ✅ JOB 종료: {}  ", jobExecution.getJobInstance().getJobName());
                log.info("┃  Status: {}", jobExecution.getStatus());
                log.info("┃  Exit Status: {}", jobExecution.getExitStatus().getExitCode());
                log.info("┃  실행 시간: {}ms", executionTime);
                log.info("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛");
            }
        };
    }

    /**
     * StepExecutionListener
     *
     */
    @Bean
    public StepExecutionListener customStepListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("  ┌─────────────────────────────────────────────────────────────────────┐");
                log.info("  │  📌 STEP 시작: {}  ", stepExecution.getStepName());
                log.info("  │  Step Execution ID: {}", stepExecution.getId());
                log.info("  └─────────────────────────────────────────────────────────────────────┘");
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("  ┌─────────────────────────────────────────────────────────────────────┐");
                log.info("  │  ✔️ STEP 종료: {}  ", stepExecution.getStepName());
                log.info("  │  Status: {}", stepExecution.getStatus());
                log.info("  │  Read Count: {}", stepExecution.getReadCount());
                log.info("  │  Write Count: {}", stepExecution.getWriteCount());
                log.info("  │  Commit Count: {}", stepExecution.getCommitCount());
                log.info("  └─────────────────────────────────────────────────────────────────────┘");
                return ExitStatus.COMPLETED;
            }
        };
    }

    /**
     * ChunkListener
     *
     */
    @Bean
    public ChunkListener customChunkListener() {
        return new ChunkListener() {
            private int chunkCount = 0;

            @Override
            public void beforeChunk(ChunkContext context) {
                chunkCount++;
                log.info("    ╭───────────────────────────────────────────────────────────╮");
                log.info("    │  🔄 CHUNK #{} 시작  ", chunkCount);
                log.info("    ╰───────────────────────────────────────────────────────────╯");
            }

            @Override
            public void afterChunk(ChunkContext context) {
                log.info("    ╭───────────────────────────────────────────────────────────╮");
                log.info("    │  ✓ CHUNK #{} 완료 (커밋됨)  ", chunkCount);
                log.info("    ╰───────────────────────────────────────────────────────────╯");
            }

            @Override
            public void afterChunkError(ChunkContext context) {
                log.error("    ╭───────────────────────────────────────────────────────────╮");
                log.error("    │  ❌ CHUNK #{} 실패 (롤백)  ", chunkCount);
                log.error("    ╰───────────────────────────────────────────────────────────╯");
            }
        };
    }

    /**
     * ItemReadListener
     *
     */
    @Bean
    public ItemReadListener<Post> customItemReadListener() {
        return new ItemReadListener<Post>() {
            private int readCount = 0;

            @Override
            public void beforeRead() {
                readCount++;
                log.info("      ├─ 📖 Read #{} 시작...", readCount);
            }

            @Override
            public void afterRead(Post item) {
                if (item != null) {
                    log.info("      ├─ ✓ Read #{} 성공: {}", readCount, item);
                } else {
                    log.info("      ├─ ○ Read #{} 완료: 더 이상 읽을 데이터 없음", readCount);
                }
            }

            @Override
            public void onReadError(Exception ex) {
                log.error("      ├─ ✗ Read #{} 에러: {}", readCount, ex.getMessage());
            }
        };
    }

    /**
     * ItemProcessListener
     *
     */
    @Bean
    public ItemProcessListener<Post, Post> customItemProcessListener() {
        return new ItemProcessListener<Post, Post>() {
            private int processCount = 0;

            @Override
            public void beforeProcess(Post item) {
                processCount++;
                log.info("      ├─ ⚙️ Process #{} 시작: {}", processCount, item);
            }

            @Override
            public void afterProcess(Post item, Post result) {
                if (result != null) {
                    log.info("      ├─ ✓ Process #{} 성공: {} → {}", processCount, item, result);
                } else {
                    log.info("      ├─ ⊘ Process #{} 필터링됨: {}", processCount, item);
                }
            }

            @Override
            public void onProcessError(Post item, Exception e) {
                log.error("      ├─ ✗ Process #{} 에러: {} | 원인: {}", processCount, item, e.getMessage());
            }
        };
    }

    /**
     * ItemWriteListener
     *
     */
    @Bean
    public ItemWriteListener<Post> customItemWriteListener() {
        return new ItemWriteListener<Post>() {
            private int writeCount = 0;

            @Override
            public void beforeWrite(Chunk<? extends Post> items) {
                writeCount++;
                log.info("      ├─ 💾 Write #{} 시작: {}개 아이템", writeCount, items.size());
            }

            @Override
            public void afterWrite(Chunk<? extends Post> items) {
                log.info("      ├─ ✓ Write #{} 성공: {}개 아이템 저장됨", writeCount, items.size());
            }

            @Override
            public void onWriteError(Exception exception, Chunk<? extends Post> items) {
                log.error("      ├─ ✗ Write #{} 에러: {}개 아이템 실패 | 원인: {}",
                        writeCount, items.size(), exception.getMessage());
            }
        };
    }
}