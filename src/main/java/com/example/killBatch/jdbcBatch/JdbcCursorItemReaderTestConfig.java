package com.example.killBatch.jdbcBatch;

import java.time.LocalDateTime;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JdbcCursorItemReaderTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job jdbcCursorItemReaderTestJob() {
        return new JobBuilder("jdbcCursorItemReaderTestJob", jobRepository)
                .start(jdbcCursorItemReaderTestStep())
                .build();
    }

    @Bean
    public Step jdbcCursorItemReaderTestStep() {
        return new StepBuilder("jdbcCursorItemReaderTestStep", jobRepository)
                .<Victim, Victim>chunk(5, transactionManager)
                .reader(jdbcCursorItemReader())
                .writer(jdbcCursorItemReaderTestWrite())
                .build();
    }

    /**
     * JdbcCursorItemReader 동작
     * 1. sql 전달
     * 2. preparedStatement -> sql 실행
     * 3. resultSet -> 내부 버퍼에 결과 보관
     * 4. cursor -> read() 호출 시 resultSet 내부 버퍼에서 데이터를 한 개씩 반환
     * 5. resultSet 내부 버퍼가 비게되면 -> 쿼리를 통해 재 요청
     *
     * 단점 : 1~5까지 커넥션을 오래 유지하는 부담이 있다.
     */

    /**
     * 커서 연속성
     * 커서의 트랜잭션과 청크의 트랜잭션은 별도
     * 청크 단위로 트랜잭션 되더라도 커서는 닫히지 않고 안전하게 데이터를 읽음
     */

    /**
     * 스냅샷
     * 동일한 Step 내에서 Processor나 Writer를 통해 데이터를 변경한다고 하더라도
     * JdbcCursorItemReader를 통해 처음 조회되었던 데이터는 항상 일관된다.
     */

    /**
     * ORDER BY 중요성
     * Step 재시작 하게되는 경우, 실패 지점까지 커서를 이동시킨다.
     * 이 때, ORDER BY 정렬이 되어있지 않다면, 동일한 데이터 순서를 보장할 수 없어서 중복처리 혹은 누락될 수 있다
     */
    @Bean
    public JdbcCursorItemReader<Victim> jdbcCursorItemReader() {
        return new JdbcCursorItemReaderBuilder<Victim>()
                .name("jdbcCursorItemReader")
                //AutoConfiguration DataSource
                .dataSource(dataSource)
                //native sql
                .sql("SELECT * FROM victims WHERE status = ? AND terminated_at <= ? ORDER BY id")
                //파라미터
                .queryArguments(List.of("TERMINATED", LocalDateTime.now()))
                //쿼리를 통해 갖고올 데이터 갯수 힌트
                .fetchSize(100)
                //결과 데이터를 객체로 변환
                .beanRowMapper(Victim.class)
//                .rowMapper((rs, rowNum) -> {
//                    Victim victim = new Victim();
//                    victim.setId(rs.getLong("id"));
//                    victim.setName(rs.getString("name"));
//                    victim.setProcessId(rs.getString("process_id"));
//                    victim.setTerminatedAt(rs.getTimestamp("terminated_at").toLocalDateTime());
//                    victim.setStatus(rs.getString("status"));
//                    return victim;
//                })
                .build();
    }

    @Bean
    public ItemWriter<Victim> jdbcCursorItemReaderTestWrite() {
        return items -> {
            for (Victim victim : items) {
                log.info("{}", victim);
            }
        };
    }

}
