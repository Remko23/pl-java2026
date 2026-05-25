package com.truthlens.backend.client;

import com.truthlens.backend.model.OcrExtractionResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.service.annotation.PostExchange;

public interface OcrServiceClient {
    
    @PostExchange(url = "/api/internal/v1/ocr:extract", contentType = MediaType.MULTIPART_FORM_DATA_VALUE)
    OcrExtractionResponse extractText(@RequestPart("image") org.springframework.core.io.Resource image);
}
