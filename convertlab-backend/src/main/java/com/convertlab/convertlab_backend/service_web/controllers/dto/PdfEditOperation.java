package com.convertlab.convertlab_backend.service_web.controllers.dto;

import lombok.Data;

@Data
public class PdfEditOperation {
    private int pageNumber;
    private double x;
    private double y;
    private double width;
    private double height;
    private String text;
    private String fontFamily;
    private double fontSize;
    private boolean bold;
    private boolean italic;
    private String textColor;
    private String coverColor;
    private boolean coverEnabled;
    private String alignment;
}
