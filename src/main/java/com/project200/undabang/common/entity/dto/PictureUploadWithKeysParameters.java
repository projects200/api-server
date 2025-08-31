package com.project200.undabang.common.entity.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PictureUploadWithKeysParameters(@NotNull List<PictureWithKeyRecord> pictureWithKeyRecordList) {

}
