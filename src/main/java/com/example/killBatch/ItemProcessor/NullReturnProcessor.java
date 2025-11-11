package com.example.killBatch.ItemProcessor;

import com.example.killBatch.jpaBatch.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * ItemProcessor
 *
 * 1. null 반환을 통한 데이터 필터링
 *      - reader -> input Chunk 생성
 *      - processor -> input Chunk 처리
 *      - null 반환한 item -> output Chunk 제외
 *      - writer -> output Chunk 처리
 */
@Slf4j
@Component
public class NullReturnProcessor implements ItemProcessor<Post, Post> {
    @Override
    public Post process(Post post) {

        /**
         * 제목 중 rm -rf 명령어 존재 시, 필터링
         */
        if (post.getTitle().contains("rm -rf /")) {
            log.info("☠️ {}의 {} -> 시스템 파괴자 처단 완료. 기록에서 말살.",
                    post.getId(),
                    post.getTitle());
            return null;
        }

        log.info("⚔️ {}의 {} -> 시스템 준수자 생존. 최종 기록 허가.",
                post.getId(),
                post.getTitle());


        return post;
    }
}
