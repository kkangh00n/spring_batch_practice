package com.example.killBatch.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExternalApiMock {

    public void externalApi(String title) {
        try{
            log.info("{} 외부 API 호출 : {}", title, System.nanoTime());
            Thread.sleep(3000);
            log.info("외부 API 호출 완료 : {}", title, System.nanoTime());
        } catch (InterruptedException e) {
            log.info(e.getMessage());
        }
    }

}
