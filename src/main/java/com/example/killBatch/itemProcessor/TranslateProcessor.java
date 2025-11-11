package com.example.killBatch.itemProcessor;

import com.example.killBatch.external.ExternalApiMock;
import com.example.killBatch.jpaBatch.BlockedPost;
import com.example.killBatch.jpaBatch.Post;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * ItemProcessor
 * 3. 데이터 변환, 보강
 *
 * 주의사항
 * 만약 호출 시간 3초인 외부 api를 통해 데이터를 보강한다고 가정한다면,
 * item 10개 -> 3*10 = 30초
 * item 100만개 -> 3*1000000 = 3000000초..
 *
 * 해결책
 * ItemWriterListener -> Chunk 단위로 Item 받음
 * Chunk 단위로 벌크 API 호출!
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TranslateProcessor implements ItemProcessor<Post, BlockedPost> {

    private final ExternalApiMock externalApiMock;

    @Override
    public BlockedPost process(Post item) {

        externalApiMock.externalApi(item.getTitle());

        return BlockedPost.builder()
                .postId(item.getId())
                .writer(item.getWriter())
                .title(item.getTitle())
                .reportCount(0)
                .blockScore(1)
                .blockedAt(LocalDateTime.now())
                .build();
    }
}
