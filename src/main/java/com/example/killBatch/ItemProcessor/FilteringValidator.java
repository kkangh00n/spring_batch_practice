package com.example.killBatch.ItemProcessor;

import com.example.killBatch.jpaBatch.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.validator.ValidationException;
import org.springframework.batch.item.validator.Validator;
import org.springframework.stereotype.Component;

/**
 * Validator
 * 1-2.
 */
@Slf4j
@Component
public class FilteringValidator implements Validator<Post> {

    @Override
    public void validate(Post post) throws ValidationException {
        /**
         * 제목 중 rm -rf 명령어 존재 시, 필터링
         */
        if (post.getTitle().contains("rm -rf /")) {

            //ValidationException 던져서 필터링
            throw new ValidationException(
                    "☠️ " + post.getId() + "의 " + post.getTitle() +
                    "-> 시스템 파괴자 처단 완료. 기록에서 말살."
            );
        }

        log.info("⚔️ {}의 {} -> 시스템 준수자 생존. 최종 기록 허가 -> Validator.",
                post.getId(),
                post.getTitle());
    }
}
