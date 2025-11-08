package com.example.killBatch.jdbcBatch;

import java.time.LocalDateTime;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JdbcPagingItemReaderTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job jdbcPagingItemReaderTestJob() {
        return new JobBuilder("jdbcPagingItemReaderTestJob", jobRepository)
                .start(jdbcPagingItemReaderTestStep())
                .build();
    }

    @Bean
    public Step jdbcPagingItemReaderTestStep() {
        return new StepBuilder("jdbcPagingItemReaderTestStep", jobRepository)
                .<Victim, Victim>chunk(5, transactionManager)
                .reader(jdbcPagingItemReader())
                .writer(jdbcPagingItemReaderTestWrite())
                .build();
    }

    /**
     * JdbcPageItemReader 동작
     * keyset 방식 동작
     *
     *
     * 쿼리 형식
     * SELECT id, name, process_id, terminated_at, status
     * FROM victims
     * WHERE (status = :status AND terminated_at <= :terminatedAt)
     * AND ((id > :_id)) -> id를 기반으로 페이징
     * ORDER BY id ASC LIMIT 5
     */

    @Bean
    public JdbcPagingItemReader<Victim> jdbcPagingItemReader() {
        return new JdbcPagingItemReaderBuilder<Victim>()
                .name("jdbcPagingItemReader")
                .dataSource(dataSource)
                //limit절 반영 -> pageSize를 chunkSize와 동일한 값으로 설정하는 것을 권장
                .pageSize(5)
                .selectClause("SELECT id, name, process_id, terminated_at, status")
                .fromClause("FROM victims")
                .whereClause("WHERE status = :status AND terminated_at <= :terminatedAt")
                .sortKeys(Map.of("id", Order.ASCENDING))
                .parameterValues(Map.of(
                        "status", "TERMINATED",
                        "terminatedAt", LocalDateTime.now()
                ))
                .beanRowMapper(Victim.class)
                .build();
    }

    @Bean
    public ItemWriter<Victim> jdbcPagingItemReaderTestWrite() {
        return items -> {
            for (Victim victim : items) {
                log.info("{}", victim);
            }
        };
    }

}
