package com.wildcard.moderation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    private Long postId;
    private Long commentId;

    @NotBlank
    @Size(max = 1000)
    private String reason;
}
