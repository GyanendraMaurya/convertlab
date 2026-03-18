package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.Data;

@Data
public class CropImageRequest {
    private String fileId;

    /** Crop origin X in natural (full-resolution) image pixels */
    private int x;

    /** Crop origin Y in natural (full-resolution) image pixels */
    private int y;

    /** Crop width in natural image pixels */
    private int width;

    /** Crop height in natural image pixels */
    private int height;

    /** Rotation to apply BEFORE cropping: 0, 90, 180, 270 */
    private int rotation;

    private boolean flipHorizontal;
    private boolean flipVertical;

    /** "JPEG" or "PNG" — defaults to JPEG if null */
    private String outputFormat;

    /** JPEG quality 1–100, defaults to 90 */
    private int quality = 90;
}