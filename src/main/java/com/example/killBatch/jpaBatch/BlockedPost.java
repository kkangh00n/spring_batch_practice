package com.example.killBatch.jpaBatch;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 차단된 게시글 - 처형 결과 보고서
 */
@Entity
@Table(name = "blocked_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlockedPost {
    @Id
    @Column(name = "post_id")
    private Long postId;

    private String writer;
    private String title;

    @Column(name = "report_count")
    private int reportCount;

    @Column(name = "block_score")
    private double blockScore;

    @Column(name = "blocked_at")
    private LocalDateTime blockedAt;

    @Builder
    public BlockedPost(Long postId, String writer, String title,
            int reportCount, double blockScore, LocalDateTime blockedAt) {
        this.postId = postId;
        this.writer = writer;
        this.title = title;
        this.reportCount = reportCount;
        this.blockScore = blockScore;
        this.blockedAt = blockedAt;
    }
}
