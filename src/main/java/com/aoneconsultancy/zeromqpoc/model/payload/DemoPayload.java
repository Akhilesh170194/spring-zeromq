package com.aoneconsultancy.zeromqpoc.model.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Simple payload used for demo purposes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoPayload {
    private long id;
    private String name;
    private LocalDateTime createdAt;
}
