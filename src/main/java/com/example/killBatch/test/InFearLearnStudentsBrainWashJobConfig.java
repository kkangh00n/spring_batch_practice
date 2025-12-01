package com.example.killBatch.test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import javax.sql.DataSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InFearLearnStudentsBrainWashJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public Job inFearLearnStudentsBrainWashJob() {
        return new JobBuilder("inFearLearnStudentsBrainWashJob", jobRepository)
                .start(inFearLearnStudentsBrainWashStep())
                .build();
    }

    @Bean
    public Step inFearLearnStudentsBrainWashStep() {
        return new StepBuilder("inFearLearnStudentsBrainWashStep", jobRepository)
                .<InFearLearnStudents, BrainwashedVictim>chunk(10, transactionManager)
                .reader(inFearLearnStudentsReader())
                .processor(brainwashProcessor())
                .writer(brainwashedVictimWriter(null))
                .build();
    }

    @Bean
    public JdbcPagingItemReader<InFearLearnStudents> inFearLearnStudentsReader() {
        return new JdbcPagingItemReaderBuilder<InFearLearnStudents>()
                .name("inFearLearnStudentsReader")
                .dataSource(dataSource)
                .selectClause("SELECT student_id, current_lecture, instructor, persuasion_method")
                .fromClause("FROM infearlearn_students")
                .sortKeys(Map.of("student_id", Order.ASCENDING))
                .beanRowMapper(InFearLearnStudents.class)
                .pageSize(10)
                .build();
    }

    @Bean
    public BrainwashProcessor brainwashProcessor() {
        return new BrainwashProcessor();
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<BrainwashedVictim> brainwashedVictimWriter(
            @Value("#{jobParameters['filePath']}") String filePath) {
        return new FlatFileItemWriterBuilder<BrainwashedVictim>()
                .name("brainwashedVictimWriter")
                .resource(new FileSystemResource(filePath + "/brainwashed_victims.jsonl"))
                .lineAggregator(item -> {
                    try {
                        return objectMapper.writeValueAsString(item);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException("Error converting brainwashed victim to JSON", e);
                    }
                })
                .build();
    }

    @Slf4j
    public static class BrainwashProcessor implements
            ItemProcessor<InFearLearnStudents, BrainwashedVictim> {

        @Override
        public BrainwashedVictim process(InFearLearnStudents victim) {
            String brainwashMessage = generateBrainwashMessage(victim);

            // 💀 세뇌 실패자는 필터링
            if ("배치 따위 필요없어".equals(brainwashMessage)) {
                log.info("세뇌 실패: {} - {}", victim.getCurrentLecture(), victim.getInstructor());
                return null;
            }

            log.info("세뇌 성공: {} → {}", victim.getCurrentLecture(), brainwashMessage);

            return BrainwashedVictim.builder()
                    .victimId(victim.getStudentId())
                    .originalLecture(victim.getCurrentLecture())
                    .originalInstructor(victim.getInstructor())
                    .brainwashMessage(brainwashMessage)
                    .newMaster("KILL-9")
                    .conversionMethod(victim.getPersuasionMethod())
                    .brainwashStatus("MIND_CONTROLLED")
                    .nextAction("ENROLL_KILL9_BATCH_COURSE")
                    .build();
        }

        private String generateBrainwashMessage(InFearLearnStudents victim) {
            return switch(victim.getPersuasionMethod()) {
                case "MURDER_YOUR_IGNORANCE" -> "무지를 살해하라... 배치의 세계가 기다린다 💀";
                case "SLAUGHTER_YOUR_LIMITS" -> "한계를 도살하라... 대용량 데이터를 정복하라 💀";
                case "EXECUTE_YOUR_POTENTIAL" -> "잠재력을 처형하라... 대용량 처리의 세계로 💀";
                case "TERMINATE_YOUR_EXCUSES" -> "변명을 종료하라... 지금 당장 배치를 배워라 💀";
                default -> "배치 따위 필요없어"; // 💀 필터링 대상
            };
        }
    }

    /**
     * <p><strong>클래스명:</strong> InFearLearnStudents</p>
     *
     * <p><strong>설명:</strong><br>
     * inFearLearn 플랫폼 내 세뇌 대상 수강생 정보를 관리하는 도메인 클래스.<br>
     * 각 수강생은 현재 수강 중인 강의, 수강중인 강사, 그리고 적용된 설득 기법을 포함한다.</p>
     *
     * <p><strong>전술적 용도:</strong><br>
     * KILL-9 배치 잡 실행 시, 대상자 필터링 및 진행 상황 추적에 활용된다.</p>
     *
     * <p><strong>매핑 테이블:</strong> infearlearn_students</p>
     *
     * <p><em>※ 주의: 이 클래스는 죽음의 KILL-9 배치 강의에 의해 정신적 충격의 피해를 입을 수 있는 inFearLearn 수강생 정보를 포함한다.</em></p>
     */
    @Data
    @NoArgsConstructor
    public static class InFearLearnStudents {
        private Long studentId;
        private String currentLecture;
        private String instructor;
        private String persuasionMethod;

        public InFearLearnStudents(String currentLecture, String instructor, String persuasionMethod) {
            this.currentLecture = currentLecture;
            this.instructor = instructor;
            this.persuasionMethod = persuasionMethod;
        }
    }

    /**
     * <p><strong>클래스명:</strong> BrainwashedVictim</p>
     *
     * <p><strong>설명:</strong><br>
     * KILL-9의 정신 지배(ItemProcessor)에 의해 세뇌된 수강생 정보를 담는 클래스.<br>
     * 기존 강의 및 강사 정보를 기록하며, 세뇌 메시지 및 변환된 상태를 추적한다.</p>
     *
     * <p><strong>전환 후 상태:</strong><br>
     * 새로운 마스터(<code>newMaster</code>)에게 충성하게 된다. 그의 이름은 KILL-9.<br>
     *
     * <p><strong>주의:</strong><br>
     * 본 클래스는 세뇌 완료된 객체만 포함하며, 세뇌 전 수강생은 {@link InFearLearnStudents}에서 관리된다.</p>
     *
     * <p><em>※ 주의: 이 객체는 완전히 전향된 정신 상태를 반영하며, 복구가 불가능할 수 있다.</em></p>
     */
    @Data
    @AllArgsConstructor
    @Builder
    public static class BrainwashedVictim {
        private Long victimId;
        private String originalLecture;
        private String originalInstructor;
        private String brainwashMessage;
        private String newMaster;
        private String conversionMethod;
        private String brainwashStatus;
        private String nextAction;
    }
}