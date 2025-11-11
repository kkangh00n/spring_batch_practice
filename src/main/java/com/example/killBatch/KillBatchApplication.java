package com.example.killBatch;

import java.time.LocalDateTime;
import java.util.Date;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class KillBatchApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication
                .run(KillBatchApplication.class, args);

        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        JobRegistry jobRegistry = context.getBean(JobRegistry.class);

        try {
            //JobParameter 생성
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("date", new Date())
                    .addJobParameter("system.target", "kill-batch", String.class)
                    .addJobParameter("system.destruction.level", 10L, Long.class)
                    .toJobParameters();

            //특정 Job 실행 -> JobParameter 파라미터 전달
            JobExecution execution = jobLauncher.run(jobRegistry.getJob("itemProcessorTestJob"), jobParameters);

            System.exit(SpringApplication.exit(context,
                    () -> execution.getStatus() == BatchStatus.COMPLETED ? 0 : 1));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(SpringApplication.exit(context, () -> 1));
        }
    }

}
