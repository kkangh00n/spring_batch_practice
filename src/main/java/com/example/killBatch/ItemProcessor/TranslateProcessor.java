package com.example.killBatch.ItemProcessor;

import com.example.killBatch.jpaBatch.BlockedPost;
import com.example.killBatch.jpaBatch.Post;
import java.time.LocalDateTime;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * ItemProcessor
 * 3. 데이터 변환, 보강
 *
 */
@Component
public class TranslateProcessor implements ItemProcessor<Post, BlockedPost> {

    @Override
    public BlockedPost process(Post item) throws Exception {
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
