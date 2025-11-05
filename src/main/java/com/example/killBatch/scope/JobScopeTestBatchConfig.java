package com.example.killBatch.scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class JobScopeTestBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(JobScopeTestBatchConfig.class);

    @Bean
    public Job scopeTestJob(
            JobRepository jobRepository,
            Step scopeTestStep
    ) {
        return new JobBuilder("scopeTestJob", jobRepository)
                .start(scopeTestStep)
                .build();
    }

    //Job이 실행될 때 생성
    //Job이 종료할 때 소멸
    //병렬 처리 지원 -> Job 시작 요청으로 다르게 넘어오는 파라미터들이 있을 때, 독립적인 빈을 생성하여 동시성 문제 해결
    @Bean
    @JobScope
    public Step scopeTestStep(
            JobRepository jobRepository,
            PlatformTransactionManager platformTransactionManager,
            @Value("#{jobParameters['system.destruction.level']}") Long level
    ) {
        return new StepBuilder("scopeTestStep", jobRepository)
                .tasklet(((contribution, chunkContext) -> {
                    log.info("시스템 파괴 프로세스가 시작되었습니다. 파괴력: {}", level);
                    return RepeatStatus.FINISHED;
                }), platformTransactionManager)
                .build();
    }


}
