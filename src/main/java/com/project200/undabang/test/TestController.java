package com.project200.undabang.test;

import com.project200.undabang.common.web.response.CommonResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class TestController {

    @PostMapping(value = "/test1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<String> handleFormSubmit2(@ModelAttribute TestDto1 testDto1) {
        return CommonResponse.success("success");
    }

    @PostMapping(
            value = "/test2",
            consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE}
    )
    public CommonResponse<String> handleFormSubmit1(@ModelAttribute TestDto1 testDto1) {
        return CommonResponse.success("success");
    }

    @PostMapping(value = "/test3", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CommonResponse<String> handleFormSubmit3(@RequestPart TestDto2 testDto,
                                                    @RequestPart List<MultipartFile> files) {
        return CommonResponse.success("success");
    }
}
