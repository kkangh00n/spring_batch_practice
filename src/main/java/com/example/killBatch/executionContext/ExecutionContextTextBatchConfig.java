package com.example.killBatch.executionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ExecutionContextTextBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(
            ExecutionContextTextBatchConfig.class);

    @Bean
    public Job executionContextAccessJob(
            JobRepository jobRepository,
            Step executionContextAccessStep,
            Step executionContextAccessAnotherStep
    ) {

        return new JobBuilder("executionContextAccessJob", jobRepository)
                .start(executionContextAccessStep)
                .next(executionContextAccessAnotherStep)
                .build();
    }

    //step 시작 전, executionContext값 초기화
    @Bean
    public Step executionContextAccessStep(
            JobRepository jobRepository,
            PlatformTransactionManager platformTransactionManager,
            Tasklet enableJobAndStep
    ) {
        return new StepBuilder("executionContextAccessStep", jobRepository)
                .tasklet(enableJobAndStep, platformTransactionManager)
                .listener(new StepExecutionListener() {
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        //1. JobExecutionContext 값 설정
                        ExecutionContext jobExecutionContext = stepExecution.getJobExecution()
                                .getExecutionContext();
                        jobExecutionContext.putString("jobNickname", "funnyJob");

                        //2. StepExecutionContext 값 설정 -> 같은 Step의 컴포넌트만 접근 가능
                        ExecutionContext stepExecutionContext = stepExecution.getExecutionContext();
                        stepExecutionContext.putString("stepNickname", "uglyStep");
                    }
                })
                .build();
    }

    @Bean
    @StepScope
    public Tasklet enableJobAndStep(
            @Value("#{jobExecutionContext['jobNickname']}") String jobNickname,
            @Value("#{stepExecutionContext['stepNickname']}") String stepNickname
    ) {
        return ((contribution, chunkContext) ->
        {
            //jobExecutionContext 접근 -> 성공
            log.info("jobNickname 조회 가능? -> {}", jobNickname != null);

            //stepExecutionContext 접근 -> 성공
            log.info("stepNickname 조회 가능? -> {}", stepNickname != null);

            return RepeatStatus.FINISHED;
        }
        );
    }

    @Bean
    public Step executionContextAccessAnotherStep(
            JobRepository jobRepository,
            PlatformTransactionManager platformTransactionManager,
            Tasklet enableJobAndWithoutStep
    ) {
        return new StepBuilder("executionContextAccessAnotherStep", jobRepository)
                .tasklet(enableJobAndWithoutStep, platformTransactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet enableJobAndWithoutStep(
            @Value("#{jobExecutionContext['jobNickname']}") String jobNickname,
            @Value("#{stepExecutionContext['stepNickname']}") String stepNickname
    ) {
        return ((contribution, chunkContext) ->
        {
            //jobExecutionContext 접근 -> 성공
            log.info("jobNickname 조회 가능? -> {}", jobNickname != null);

            //stepExecutionContext 접근 -> 실패
            log.info("stepNickname 조회 가능? -> {}", stepNickname != null);

            return RepeatStatus.FINISHED;
        }
        );
    }

}
