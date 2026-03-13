package com.convertlab.convertlab_backend.service_storage.impl;

import com.convertlab.convertlab_backend.service_storage.StorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Log4j2
@Service
@Profile("prod")
public class S3StorageService implements StorageService {

    private static final String PDF_PREFIX    = "pdf/";
    private static final String IMAGE_PREFIX  = "images/";
    private static final String THUMB_PREFIX  = "thumbnails/";

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    @Override
    public String saveTempPdf(MultipartFile file) throws Exception {
        String key = PDF_PREFIX + UUID.randomUUID() + "_" + file.getOriginalFilename();
        upload(key, file.getInputStream(), file.getSize(), "application/pdf");
        log.info("PDF uploaded to S3: {}", key);
        return key;
    }

    @Override
    public String saveTempImage(MultipartFile file) throws Exception {
        String key = IMAGE_PREFIX + UUID.randomUUID() + "_" + file.getOriginalFilename();
        upload(key, file.getInputStream(), file.getSize(), file.getContentType());
        log.info("Image uploaded to S3: {}", key);
        return key;
    }

    @Override
    public String saveThumbnail(String assetId, BufferedImage image) throws IOException {
        String key = THUMB_PREFIX + assetId + ".png";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("image/png")
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(bytes));
        log.info("Thumbnail uploaded to S3: {}", key);
        return key;
    }

    // ── Download (to temp File — will be replaced with InputStream later) ───

    @Override
    public File loadPdf(String fileId) {
        return downloadToTempFile(fileId, ".pdf");
    }

    @Override
    public File loadImage(String fileId) {
        String extension = resolveExtension(fileId);
        return downloadToTempFile(fileId, extension);
    }

    @Override
    public File loadThumbnail(String fileId) {
        // fileId here is the full key returned by saveThumbnail
        return downloadToTempFile(fileId, ".png");
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Override
    public void delete(String fileId) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileId)
                    .build();
            s3Client.deleteObject(request);
            log.info("Deleted from S3: {}", fileId);
        } catch (Exception e) {
            log.error("Failed to delete S3 object: {}", fileId, e);
        }
    }

    private void upload(String key, InputStream inputStream, long contentLength, String contentType)
            throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, contentLength));
    }

    /**
     * Downloads an S3 object to a local temp file.
     * The file is marked deleteOnExit so the JVM cleans it up eventually.
     * TODO: replace with InputStream-based approach to cut latency.
     */
    private File downloadToTempFile(String key, String suffix) {
        try {
            Path tempFile = Files.createTempFile("s3-", suffix);
            tempFile.toFile().deleteOnExit();

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            try (InputStream s3Stream = s3Client.getObject(request)) {
                Files.copy(s3Stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            }

            log.debug("Downloaded S3 object {} to temp file {}", key, tempFile);
            return tempFile.toFile();

        } catch (Exception e) {
            log.error("Failed to download S3 object: {}", key, e);
            throw new RuntimeException("Failed to load file from S3: " + key, e);
        }
    }

    private String resolveExtension(String key) {
        int dot = key.lastIndexOf('.');
        if (dot != -1) {
            return key.substring(dot); // e.g. ".jpg"
        }
        return "";
    }
}
