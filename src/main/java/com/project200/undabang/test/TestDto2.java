package com.project200.undabang.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class TestDto2 {
    private final String name;
    private final int age;

    //json 제외
    @JsonIgnore
    private MultipartFile file;
}
