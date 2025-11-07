package com.example.killBatch.dbBatch;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Victim {
    private Long id;
    private String name;
    private String processId;
    private LocalDateTime terminatedAt;
    private String status;

}
