package com.project200.undabang.configuration;

import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.snippet.Attributes;

public interface DocumentFormatGenerator {

    static Attributes.Attribute getTypeFormat(JsonFieldType jsonFieldType) {
        return Attributes.key("type").value(jsonFieldType);
    }

    static Attributes.Attribute getTypeFormat(String value) {
        return Attributes.key("type").value(value);
    }
}