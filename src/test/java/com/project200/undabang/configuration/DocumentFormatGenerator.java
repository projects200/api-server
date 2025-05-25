package com.project200.undabang.configuration;

import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Attributes;

public interface DocumentFormatGenerator {

    static Attributes.Attribute getTypeFormat(JsonFieldType jsonFieldType) { // (2)
        return Attributes.key("type").value(jsonFieldType);
    }
}