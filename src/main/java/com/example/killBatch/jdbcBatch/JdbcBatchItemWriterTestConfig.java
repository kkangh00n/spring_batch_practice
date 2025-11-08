package com.example.killBatch.jdbcBatch;

import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class JdbcBatchItemWriterTestConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;

    @Bean
    public Job jdbcBatchItemWriterTestJob() {
        return new JobBuilder("jdbcBatchItemWriterTestJob", jobRepository)
                .start(jdbcBatchItemWriterStep())
                .build();
    }

    @Bean
    public Step jdbcBatchItemWriterStep() {
        return new StepBuilder("jdbcBatchItemWriterStep", jobRepository)
                .<HackedOrder, HackedOrder>chunk(10, transactionManager)
                .reader(jdbcBatchItemWriterTestReader())
                .processor(jdbcBatchItemWriterTestProcessor())
                .writer(jdbcBatchItemWriter())
                .build();
    }

    @Bean
    public JdbcPagingItemReader<HackedOrder> jdbcBatchItemWriterTestReader() {
        return new JdbcPagingItemReaderBuilder<HackedOrder>()
                .name("jdbcBatchItemWriterTestReader")
                .dataSource(dataSource)
                .pageSize(10)
                .selectClause("SELECT id, customer_id, order_datetime, status, shipping_id")
                .fromClause("FROM orders")
                .whereClause("WHERE (status = 'SHIPPED' and shipping_id is null) " +
                             "or (status = 'CANCELLED' and shipping_id is not null)")
                .sortKeys(Map.of("id", Order.ASCENDING))
                .beanRowMapper(HackedOrder.class)
                .build();
    }

    @Bean
    public ItemProcessor<HackedOrder, HackedOrder> jdbcBatchItemWriterTestProcessor() {
        return order -> {
            if (order.getShippingId() == null) {
                order.setStatus("READY_FOR_SHIPMENT");
            } else {
                order.setStatus("SHIPPED");
            }
            return order;
        };
    }

    /**
     * JdbcBatchItemWriter
     * 청크 단위로 묶인 데이터를 하나의 쿼리로 전송
     */
    @Bean
    public JdbcBatchItemWriter<HackedOrder> jdbcBatchItemWriter() {
        return new JdbcBatchItemWriterBuilder<HackedOrder>()
                .dataSource(dataSource)
                .sql("UPDATE orders SET status = :status WHERE id = :id")
                // 객체를 필드의 자동 매핑
                .beanMapped()
                //1. true -> 단 하나의 데이터라도 쓰기 혹은 업데이트에 실패하면 즉시 예외를 던짐
                //2. false -> 일부 데이터가 업데이트 되지 않아도 작전을 계속 진행
                .assertUpdates(true)
                .build();
    }

}

