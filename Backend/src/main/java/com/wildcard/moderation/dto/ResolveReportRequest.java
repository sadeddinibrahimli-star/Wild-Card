package com.wildcard.moderation.dto;

import com.wildcard.moderation.ReportStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveReportRequest {

    @NotNull
    private ReportStatus status;

    @Size(max = 1000)
    private String resolutionNote;

    private boolean removeContent;
}
