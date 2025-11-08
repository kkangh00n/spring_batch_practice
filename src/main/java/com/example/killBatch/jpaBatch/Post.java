package com.example.killBatch.jpaBatch;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

@Entity
@Table(name = "posts")
@Getter
public class Post {
    @Id
    private Long id;
    private String title;         // 게시물 제목
    private String content;       // 게시물 내용
    private String writer;        // 작성자

    @OneToMany(mappedBy = "post")
    private List<Report> reports = new ArrayList<>();
}