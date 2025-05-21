package com.project200.undabang.common.service;

public enum FileType {
    EXERCISE("exercises"),
    PROFILE("profiles"),
    THUMBNAIL("thumbnails");

    private final String path;

    FileType(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
