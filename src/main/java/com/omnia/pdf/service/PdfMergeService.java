package com.omnia.pdf.service;

import com.omnia.pdf.domain.MergedPdfDocument;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PdfMergeService {

    public MergedPdfDocument merge(List<MultipartFile> files) {
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        List<RandomAccessReadBuffer> sourceBuffers = new ArrayList<>();

        try (ByteArrayOutputStream mergedOutputStream = new ByteArrayOutputStream()) {
            pdfMerger.setDestinationStream(mergedOutputStream);

            for (MultipartFile file : files) {
                if (!isPdfFile(file)) {
                    continue; // PDF가 아니거나 빈 파일은 무시
                }
                RandomAccessReadBuffer sourceBuffer = new RandomAccessReadBuffer(file.getInputStream());
                sourceBuffers.add(sourceBuffer);
                pdfMerger.addSource(sourceBuffer);
            }

            if (sourceBuffers.isEmpty()) {
                throw new IllegalArgumentException("병합할 수 있는 유효한 PDF 파일이 없습니다.");
            }

            // OOM 방지를 위한 메모리 전용 사용 설정
            pdfMerger.mergeDocuments(IOUtils.createMemoryOnlyStreamCache());

            String mergedFileName = "merged_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            return new MergedPdfDocument(mergedFileName, mergedOutputStream.toByteArray());

        } catch (Exception e) {
            throw new RuntimeException("PDF 파일 병합 중 오류가 발생했습니다.", e);
        } finally {
            for (var sourceBuffer : sourceBuffers) {
                IOUtils.closeQuietly(sourceBuffer);
            }
        }
    }

    private static boolean isPdfFile(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }
        if ("application/pdf".equals(file.getContentType())) {
            return true;
        }
        // Content-Type이 누락되거나 다른 값으로 오는 경우(드래그앤드롭, 일부 브라우저 등)를 대비한 확장자 폴백
        String filename = file.getOriginalFilename();
        return filename != null && filename.toLowerCase().endsWith(".pdf");
    }
}
