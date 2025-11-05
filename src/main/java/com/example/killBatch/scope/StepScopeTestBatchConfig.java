package com.example.killBatch.scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class StepScopeTestBatchConfig {

    private static final Logger log = LoggerFactory.getLogger(StepScopeTestBatchConfig.class);

    @Bean
    public Job stepScopeTestJob(
            JobRepository jobRepository,
            Step infiltrationStep
    ) {
        return new JobBuilder("stepScopeTestJob", jobRepository)
                .start(infiltrationStep)
                .build();
    }

    @Bean
    public Step infiltrationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet systemInfiltrationTasklet
    ) {
        return new StepBuilder("infiltrationStep", jobRepository)
                .tasklet(systemInfiltrationTasklet, transactionManager)
                .build();
    }

    //Step이 실행될 때 생성
    //Step이 종료할 때 소멸
    //병렬 처리 지원 -> Step이 생성될 때, 독립적인 인스턴스가 생성
    @Bean
    @StepScope
    public Tasklet systemInfiltrationTasklet(
            @Value("#{jobParameters['system.target']}") String target
    ) {
        return (contribution, chunkContext) -> {
            String[] targets = target.split("-");
            log.info("시스템 침투 시작");
            log.info("주 타겟: {}", targets[0]);
            log.info("보조 타겟: {}", targets[1]);
            log.info("침투 완료");
            return RepeatStatus.FINISHED;
        };
    }


}
