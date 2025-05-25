package com.project200.undabang.test;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TestDto1 {
    private final String name;
    private final int age;
    private final MultipartFile file;
}
