package com.truthlens.ocr.controller;

import com.truthlens.ocr.model.OcrExtractionResponse;
import com.truthlens.ocr.service.VisionExtractionService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class OcrController {

    private final VisionExtractionService visionExtractionService;

    public OcrController(VisionExtractionService visionExtractionService) {
        this.visionExtractionService = visionExtractionService;
    }

    @PostMapping(value = "/api/internal/v1/ocr:extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OcrExtractionResponse extract(@RequestParam("image") MultipartFile image) {
        return visionExtractionService.extractText(image);
    }
}