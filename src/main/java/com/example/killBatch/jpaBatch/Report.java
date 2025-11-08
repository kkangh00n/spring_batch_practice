package com.example.killBatch.jpaBatch;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Table(name = "reports")
@Getter
public class Report {
    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    private String reportType;     // SPAM, ABUSE, ILLEGAL, FAKE_NEWS ...
    private int reporterLevel;     // 신고자 신뢰도 (1~5)
    private String evidenceData;   // 증거 데이터 (URL 등)
    private LocalDateTime reportedAt;
}
