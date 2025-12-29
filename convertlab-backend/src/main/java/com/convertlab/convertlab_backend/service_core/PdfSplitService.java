package com.convertlab.convertlab_backend.service_core;

import com.convertlab.convertlab_backend.api.enums.SplitType;
import com.convertlab.convertlab_backend.exception.InvalidPageInputException;
import com.convertlab.convertlab_backend.service_util.PdfUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
@Service
public class PdfSplitService {

    /**
     * Split PDF into individual pages or ranges and write directly to ZIP stream
     *
     * @param sourcePdf  The source PDF file
     * @param pageRange  Page range string (e.g., "1-3,5,7-9")
     * @param splitType  Type of split (EACH_PAGE or BY_RANGE)
     * @param zipOut     ZIP output stream to write to
     * @throws IOException If an error occurs during processing
     */
    public void splitIntoZip(
            File sourcePdf,
            String pageRange,
            SplitType splitType,
            ZipOutputStream zipOut
    ) throws IOException {
        log.debug("Starting PDF split for file: {}, pageRange: {}, splitType: {}",
                sourcePdf.getName(), pageRange, splitType);

        try (PDDocument sourceDoc = Loader.loadPDF(sourcePdf)) {

            int totalPages = sourceDoc.getNumberOfPages();
            log.debug("Source PDF has {} pages", totalPages);

            if (splitType == SplitType.EACH_PAGE) {
                splitEachPage(sourceDoc, totalPages, zipOut);
            } else {
                splitByRange(sourceDoc, pageRange, totalPages, zipOut);
            }

            log.info("PDF split completed successfully");
        }
    }

    /**
     * Split every page into a separate PDF
     */
    private void splitEachPage(PDDocument sourceDoc, int totalPages, ZipOutputStream zipOut)
            throws IOException {
        log.debug("Splitting into individual pages (total: {})", totalPages);

        for (int i = 1; i <= totalPages; i++) {

            String entryName = String.format("page-%03d.pdf", i);
            zipOut.putNextEntry(new ZipEntry(entryName));

            try (PDDocument target = new PDDocument()) {

                PDPage sourcePage = sourceDoc.getPage(i - 1);

                PDPage importedPage = target.importPage(sourcePage);
                importedPage.setRotation(sourcePage.getRotation());

                target.save(zipOut);
            }

            zipOut.closeEntry();
            log.debug("Created PDF for page {}", i);
        }
    }

    /**
     * Split by page ranges - each range becomes a separate PDF
     * Format: "1-3,5,7-9" creates 3 PDFs:
     *   - pages-1-3.pdf (pages 1,2,3)
     *   - page-5.pdf (page 5)
     *   - pages-7-9.pdf (pages 7,8,9)
     */
    private void splitByRange(PDDocument sourceDoc, String pageRange, int totalPages, ZipOutputStream zipOut)
            throws IOException {
        log.debug("Splitting by ranges: {}", pageRange);

        List<PageRangeSegment> segments = parseRangeSegments(pageRange, totalPages);

        for (PageRangeSegment segment : segments) {
            String entryName = generateFileName(segment);
            zipOut.putNextEntry(new ZipEntry(entryName));

            try (PDDocument rangeDoc = new PDDocument()) {

                for (int pageNum : segment.pages) {
                    PDPage sourcePage = sourceDoc.getPage(pageNum - 1);
                    PDPage importedPage = rangeDoc.importPage(sourcePage);
                    importedPage.setRotation(sourcePage.getRotation());
                }
                rangeDoc.save(zipOut);
            }

            zipOut.closeEntry();
            log.debug("Created PDF for range: {}", segment);
        }
    }

    /**
     * Parse page range string into segments
     * Example: "1-3,5,7-9" → [{1,2,3}, {5}, {7,8,9}]
     */
    private List<PageRangeSegment> parseRangeSegments(String input, int totalPages) {
        List<PageRangeSegment> segments = new ArrayList<>();

        if (input == null || input.isBlank()) {
            throw new InvalidPageInputException("Page range cannot be empty for BY_RANGE split type");
        }

        String[] parts = input.split(",");

        for (String part : parts) {
            part = part.trim();

            if (part.contains("-")) {
                // Range: "1-3"
                String[] range = part.split("-");
                if (range.length != 2) {
                    throw new InvalidPageInputException("Invalid range format: '" + part + "'. Expected format: 4-7");
                }

                int start, end;
                try {
                    start = Integer.parseInt(range[0].trim());
                    end = Integer.parseInt(range[1].trim());
                } catch (NumberFormatException e) {
                    throw new InvalidPageInputException("Range contains non-numeric values: '" + part + "'");
                }

                // Normalize if reversed
                if (start > end) {
                    int temp = start;
                    start = end;
                    end = temp;
                }

                // Validate range
                if (start < 1 || end > totalPages) {
                    throw new InvalidPageInputException(
                            "Range '" + part + "' is outside valid page numbers (1 to " + totalPages + ").");
                }

                List<Integer> pages = new ArrayList<>();
                for (int i = start; i <= end; i++) {
                    pages.add(i);
                }
                segments.add(new PageRangeSegment(start, end, pages));

            } else {
                // Single page: "5"
                int pageNum;
                try {
                    pageNum = Integer.parseInt(part.trim());
                } catch (NumberFormatException e) {
                    throw new InvalidPageInputException("Invalid page value: '" + part + "'. Only numbers allowed.");
                }

                if (pageNum < 1 || pageNum > totalPages) {
                    throw new InvalidPageInputException(
                            "Page '" + pageNum + "' is outside valid range 1 to " + totalPages + ".");
                }

                List<Integer> pages = new ArrayList<>();
                pages.add(pageNum);
                segments.add(new PageRangeSegment(pageNum, pageNum, pages));
            }
        }

        if (segments.isEmpty()) {
            throw new InvalidPageInputException("No valid page ranges found in input: " + input);
        }

        return segments;
    }

    /**
     * Generate filename for a range segment
     */
    private String generateFileName(PageRangeSegment segment) {
        if (segment.start == segment.end) {
            return String.format("page-%d.pdf", segment.start);
        } else {
            return String.format("pages-%d-%d.pdf", segment.start, segment.end);
        }
    }

    /**
     * Validate page range input pattern before processing
     */
    public void validatePageRange(String pageRange, SplitType splitType) {
        if (splitType == SplitType.BY_RANGE) {
            PdfUtils.validateInputRangePattern(pageRange);
        }
    }

    /**
     * Internal class to represent a page range segment
     */
    private static class PageRangeSegment {
        final int start;
        final int end;
        final List<Integer> pages;

        PageRangeSegment(int start, int end, List<Integer> pages) {
            this.start = start;
            this.end = end;
            this.pages = pages;
        }

        @Override
        public String toString() {
            return start == end ? String.valueOf(start) : start + "-" + end;
        }
    }
}