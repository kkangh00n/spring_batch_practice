package com.example.killBatch;

import java.time.LocalDateTime;
import java.util.Date;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 메타데이터란?
 * 1. 어떤 작업이 실행되었는지
 * 2. 해당 작업이 어디까지 진행되었는지
 * 3. 어느 지점에서 실패했는지
 * 기록들이 저장되어있는 테이블
 */

/**
 * 0. 배치 메타데이터 테이블 구조
 * BATCH_JOB_INSTANCE -> JobInstance
 * BATCH_JOB_EXECUTION -> JobExecution
 * BATCH_JOB_EXECUTION_PARAMS -> JobParameters
 * BATCH_JOB_EXECUTION_CONTEXT -> JobExecutionContext
 * BATCH_STEP_EXECUTION -> StepExecution
 * BATCH_STEP_EXECUTION_CONTEXT -> StepExecutionContext
 */

/**
 * 1. JobInstance
 * Job의 논리적 실행 단위
 *
 * ex)
 * Job: 월간 매출 정산 작업
 *   JobInstance#1: 2024년 1월 매출 정산
 *   JobInstance#2: 2024년 2월 매출 정산
 *   JobInstance#3: 2024년 3월 매출 정산
 *
 * JobInstance를 구분하는 기준을 다음과 같다
 * - 이름 -> 어떠한 Job에 속하는지 구분
 * - JobParameters -> 이름이 동일한 Job 내에서 구분
 *
 * 한 번 완료된 JobInstance는 재실행할 수 없다!
 */

@SpringBootApplication
public class KillBatchApplication {

    public static void main(String[] args) {

        ConfigurableApplicationContext context = SpringApplication
                .run(KillBatchApplication.class, args);

        JobLauncher jobLauncher = context.getBean(JobLauncher.class);
        JobRegistry jobRegistry = context.getBean(JobRegistry.class);

        try {
            /**
             * 2. JobParameters
             * - 이름이 동일한 Job을 서로 다른 JobInstance로 구분하는 핵심 요소
             * - JobParameters는 단순한 데이터 전달체가 아니다!
             */
            //JobParameter 생성
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("date", new Date())
                    .addJobParameter("system.target", "kill-batch", String.class)
                    .addJobParameter("system.destruction.level", 10L, Long.class)
                    .toJobParameters();

            /**
             * 3. JobExecution
             * - JobInstance의 실제 실행 이력
             * - 동일한 JobInstance가 여러번 실행될 수 있기에, 구분 필요
             * - 주요 실행 정보 포함
             *      - 현재 상태
             *      - 시작 시간
             *      - 종료 시간
             *      - 종료 코드(BatchStatus)
             *      - 실패 원인
             * - (JobExecutionListener의 각 메서드의 파라미터로 전달됨)
             */

            /**
             * 4. JobLauncher
             * Job과 JobParameters를 전달받아 실행
             *      - Job 이름과 JobParameters로 식별 가능하기 때문
             * JobExecution 반환
             */
            //특정 Job 실행 -> JobParameter 파라미터 전달
            JobExecution execution = jobLauncher.run(jobRegistry.getJob("skipItemWriterTestJob"), jobParameters);

            /**
             * 7. ExecutionContext
             * Job 혹은 Step의 비즈니스 로직 처리 중 발생하는 사용자 정의 데이터를 관리하는 저장소이다!
             *
             * 하나의 JobExecution은 하나의 JobExecutionContext 포함
             * 하나의 JobExecution은 여러 개의 StepExecution 포함
             *      -> 하나의 StepExecution이라도 실패하면 JobExecution도 실패
             * 하나의 StepExecution은 하나의 StepExecutionContext 포함
             *      -> 각 StepExecution이 다른 StepExecutionContext를 참조할 수 없다!
             *
             *
             * ex) 구조는 다음과 같다.
             * JobExecution -> JobExecutionContext 포함
             *      - StepExecution -> StepExecutionContext 포함
             *      - StepExecution -> StepExecutionContext 포함
             *      - StepExecution -> StepExecutionContext 포함
             */

            /**
             * 9. 실패와 재시작 시 StepExecution 동작
             *
             * ex)
             * JobExecution#1 (FAILED) {
             *     StepExecution#1 ("step1", COMPLETED)
             *     StepExecution#2 ("step2", FAILED) // -> 실패
             * }
             *
             * JobExecution#2 (COMPLETED) {
             *     // StepExecution#1 ("step1")은 이미 성공했으므로 다시 생성되지 않음
             *     StepExecution#3 ("step2", COMPLETED) // -> 재시작
             * }
             */
            ExecutionContext jobExecutionContext = execution.getExecutionContext();

            System.exit(SpringApplication.exit(context,
                    () -> execution.getStatus() == BatchStatus.COMPLETED ? 0 : 1));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(SpringApplication.exit(context, () -> 1));
        }
    }

}

/**
 * 5. 동일 작업 여러 번 실행하는 방법
 * 한 번 완료된 JobInstance는 재실행할 수 없다!
 * 만약 다시 실행하고 싶다면? -> jobParameter를 변경!
 *
 * JobParametersIncrementer를 통해서 동일 작업을 여러 번 실행할 수 있다.
 *
 * ex) Job 생성 시
 * @Bean
 * public Job brutalizedSystemJob() {
 *     return new JobBuilder("brutalizedSystemJob", jobRepository)
 *             .incrementer(new RunIdIncrementer()) // -> 추가
 *             .start(brutalizedSystemStep())
 *             .build();
 * }
 */

/**
 * 6. Job 재실행 통제
 * 비즈니스적인 이유로 작업이 한 번 실패하면 다시는 실행되지 않아아 햔다면
 *
 * ex) Job 생성 시
 * @Bean
 * public Job brutalizedSystemJob() {
 *     return new JobBuilder("brutalizedSystemJob", jobRepository)
 *             .start(brutalizedSystemStep())
 *             .preventRestart() // -> 추가
 *             .build();
 * }
 */