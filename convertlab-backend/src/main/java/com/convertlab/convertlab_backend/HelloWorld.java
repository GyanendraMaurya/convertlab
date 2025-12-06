package com.convertlab.convertlab_backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hello")
public class HelloWorld {

    @GetMapping
    public String hello() {
        return "ConvertLab backend is running!";
    }
}
