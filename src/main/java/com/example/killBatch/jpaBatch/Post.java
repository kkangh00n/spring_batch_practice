package com.example.killBatch.jpaBatch;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@RequiredArgsConstructor
public class Post {
    @Id
    private final Long id;
    private final String title;         // 게시물 제목
    private final String content;       // 게시물 내용
    private final String writer;        // 작성자

    /**
     * JpaPagingItemReader 한계 -> 페이징과 fetch join 함께 사용 자제
     * transacted(false) -> 트랜잭션 미참여로 인한 @BatchSize 미동작
     * 결론 -> JpaPagingItemReader에서 컬렉션 Lazy Loading을 위한 Eager 설정
     */
    @OneToMany(mappedBy = "post", fetch = FetchType.EAGER)
//    @BatchSize(size = 10)
    private final List<Report> reports = new ArrayList<>();
}