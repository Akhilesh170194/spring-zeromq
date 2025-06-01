package com.aoneconsultancy.zeromqpoc.model.payload;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple payload used for demo purposes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoPayload {
    private long id;
    private String name;
    private LocalDateTime createdAt;
}
