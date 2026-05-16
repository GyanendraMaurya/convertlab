package com.convertlab.convertlab_backend.service_ai;

import com.convertlab.convertlab_backend.service_ai.exception.AiException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class DocMindPromptService {

    private static final String PROMPT_ROOT = "prompts/docmind/";

    public String systemPrompt() {
        return load("system.md");
    }

    public String directQueryPrompt(String document, String question) {
        return render("direct-query.md", Map.of(
                "document", document,
                "question", question
        ));
    }

    public String ragQueryPrompt(String context, String question) {
        return render("rag-query.md", Map.of(
                "context", context,
                "question", question
        ));
    }

    private String render(String fileName, Map<String, String> values) {
        String prompt = load(fileName);
        for (Map.Entry<String, String> entry : values.entrySet()) {
            prompt = prompt.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return prompt;
    }

    private String load(String fileName) {
        try {
            ClassPathResource resource = new ClassPathResource(PROMPT_ROOT + fileName);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new AiException("Unable to load DocMind prompt: " + fileName, "PROMPT_LOAD_FAILED", e);
        }
    }
}
