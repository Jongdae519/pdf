package com.omnia.pdf.controller;

import com.omnia.pdf.domain.MergedPdfDocument;
import com.omnia.pdf.service.PdfMergeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class PdfController {

    private final PdfMergeService pdfMergeService;

    public PdfController(PdfMergeService pdfMergeService) {
        this.pdfMergeService = pdfMergeService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @PostMapping("/pdf/merge")
    public ResponseEntity<?> mergePdfs(@RequestParam("pdfFiles") List<MultipartFile> files) {
        try {
            if (files == null || files.isEmpty() || files.get(0).isEmpty()) {
                return createErrorResponse("업로드된 파일이 없습니다.");
            }

            MergedPdfDocument mergedDocument = pdfMergeService.merge(files);

            // RFC 9110: 브라우저가 화면 이동 없이 즉시 다운로드를 수행하도록 헤더 강제
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + mergedDocument.fileName() + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(mergedDocument.content());

        } catch (Exception e) {
            return createErrorResponse("병합 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * JS 없이 에러를 처리하기 위해, 실패 시 브라우저 화면을 덮어씌울 에러 HTML을 즉석에서 반환합니다.
     */
    private ResponseEntity<byte[]> createErrorResponse(String message) {
        String errorHtml = """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>오류 발생</title>
                <script src="https://cdn.tailwindcss.com"></script>
            </head>
            <body class="bg-gray-50 flex items-center justify-center min-h-screen p-4">
                <div class="bg-white p-8 rounded-2xl shadow-xl text-center max-w-lg w-full border border-gray-100">
                    <h3 class="text-xl font-bold text-gray-900">오류 발생</h3>
                    <p class="text-sm text-red-500 mt-2">%s</p>
                    <a href="/" class="mt-6 inline-block w-full py-3 px-4 rounded-xl text-sm font-bold text-white bg-indigo-600 hover:bg-indigo-700 transition-colors">돌아가기</a>
                </div>
            </body>
            </html>
            """.formatted(message);

        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_HTML)
                .body(errorHtml.getBytes(StandardCharsets.UTF_8));
    }
}
