package com.docprocess.pojo;

import com.docprocess.constant.PdfQueueProcessingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class PdfGenerationQueueResponse {
    private String requestId;
    private PdfQueueProcessingStatus status;
}
