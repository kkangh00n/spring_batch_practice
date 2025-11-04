package com.example.killBatch.jobParameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
public class SystemDestructionTasklet implements Tasklet {

    private final static Logger log = LoggerFactory.getLogger(SystemDestructionTasklet.class);

    //Job Parameter 직접 접근
    //StepExecution -> JobExecution -> JobParameters
    @Override
    public RepeatStatus execute(
            StepContribution contribution,
            ChunkContext chunkContext
    ) throws Exception {
        JobParameters jobParameters = chunkContext.getStepContext()
                .getStepExecution()
                .getJobParameters();

        String targetSystem = jobParameters.getString("system.target");
        long destructionLevel = jobParameters.getLong("system.destruction.level");

        log.info("타겟 시스템: {}", targetSystem);
        log.info("파괴 레벨: {}", destructionLevel);

        return RepeatStatus.FINISHED;
    }
}
