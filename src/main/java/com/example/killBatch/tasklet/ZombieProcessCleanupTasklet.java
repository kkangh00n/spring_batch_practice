package com.example.killBatch.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

public class ZombieProcessCleanupTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ZombieProcessCleanupTasklet.class);
    private final int processToKill = 10;
    private int killedProcess = 0;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        killedProcess++;
        log.info("☠️  프로세스 강제 종료... ({}/{})", killedProcess, processToKill);

        if (killedProcess >= processToKill) {
            log.info("💀 시스템 안정화 완료. 모든 좀비 프로세스 제거.");
            return RepeatStatus.FINISHED;
        }

        return RepeatStatus.CONTINUABLE;
    }
}
