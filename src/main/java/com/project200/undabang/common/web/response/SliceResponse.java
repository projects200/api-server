package com.project200.undabang.common.web.response;

import lombok.Getter;
import org.springframework.data.domain.Slice;

import java.util.List;

@Getter
public class SliceResponse<T> {
    private final List<T> content; // 실제 데이터 리스트
    private final boolean hasNext; // 다음 페이지 존재 유무

    public SliceResponse(Slice<T> slice) {
        this.content = slice.getContent();
        this.hasNext = slice.hasNext();
    }
}
