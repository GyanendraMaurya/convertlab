package com.convertlab.convertlab_backend.service_core;
import com.convertlab.convertlab_backend.service_core.pojos.ExtractedFile;
import com.convertlab.convertlab_backend.service_web.controllers.dto.ExtractRequest;
import com.convertlab.convertlab_backend.service_web.controllers.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

public interface PdfService {
    UploadResponse uploadPdf(MultipartFile file) throws Exception;
    ExtractedFile extractPages(ExtractRequest request, List<Integer> pagesToKeep) throws Exception;
    ExtractedFile extractPages(File file, List<Integer> pagesToKeep) throws Exception;
}



