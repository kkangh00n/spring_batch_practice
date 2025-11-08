package com.example.killBatch.dbBatch;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class HackedOrder {
    private Long id;
    private Long customerId;
    private LocalDateTime orderDateTime;
    private String status;
    private String shippingId;
}
