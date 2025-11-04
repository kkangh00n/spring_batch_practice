package com.example.killBatch.tasklet;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ZombieCleanBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager platformTransactionManager;

    public ZombieCleanBatchConfig(
            JobRepository jobRepository,
            PlatformTransactionManager platformTransactionManager
    ) {
        this.jobRepository = jobRepository;
        this.platformTransactionManager = platformTransactionManager;
    }

    @Bean
    public Job zombieCleanJob() {
        return new JobBuilder("zombieCleanupJob", jobRepository)
                .start(zombieCleanStep())
                .build();
    }

    @Bean
    public Step zombieCleanStep() {
        return new StepBuilder("zombieCleanupStep", jobRepository)
                //execute() 메서드 실행 중 발생하는 DB 작업을 하나의 트랜잭션으로 관리하기 위함
                .tasklet(zombieProcessCleanupTasklet(), platformTransactionManager)
                .build();
    }

    @Bean
    public Tasklet zombieProcessCleanupTasklet() {
        return new ZombieProcessCleanupTasklet();
    }
}
