package com.example.killBatch.jobParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SystemTerminatorConfig {

    private final static Logger log = LoggerFactory.getLogger(SystemTerminatorConfig.class);

    @Bean
    public Job processTerminatorJob(
            JobRepository jobRepository,
            Step terminationStep,
            SystemDestructionValidator systemDestructionValidator
    ) {
        return new JobBuilder("processTerminatorJob", jobRepository)
                .start(terminationStep)
//                .validator(new DefaultJobParametersValidator( //스프링 배치 기본 구현체
//                        new String[]{"system.target"}, //필수 파라미터
//                        new String[]{} //선택적 파라미터
//                ))
                .validator(systemDestructionValidator) //커스텀 validator
                .build();
    }

    @Bean
    public Step terminationStep(
            JobRepository jobRepository,
            PlatformTransactionManager platformTransactionManager,
            SystemDestructionTasklet systemDestructionTasklet
    ) {
        return new StepBuilder("terminationStep", jobRepository)
                .tasklet(systemDestructionTasklet, platformTransactionManager)
                .build();
    }


}
