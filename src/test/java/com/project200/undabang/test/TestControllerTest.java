package com.project200.undabang.test;

import com.project200.undabang.configuration.AbstractRestDocSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.restdocs.payload.JsonFieldType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestPartFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@Disabled
@WebMvcTest(TestController.class)
class TestControllerTest extends AbstractRestDocSupport {

    @Test
    void handleFormSubmit1_1() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a test file".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/test1")
                        .file(mockMultipartFile)
                        .param("name", "testName")
                        .param("age", "30")
                        .header("X-USER-ID", UUID.randomUUID())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestParts( // 파일 파트 문서화
                                partWithName("file").description("The file to upload")
                        ),
                        formParameters( // 폼 필드 파라미터 문서화!
                                parameterWithName("name").description("User's name"),
                                parameterWithName("age").description("User's age")
                        )
                ));
    }

    @Test
    void handleFormSubmit1_2() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a test file".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/test1")
                        .file(mockMultipartFile)
                        .queryParam("name", "testName")
                        .queryParam("age", "30")
                        .header("X-USER-ID", UUID.randomUUID())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestParts( // 파일 파트 문서화
                                partWithName("file").description("The file to upload")
                        ),
                        queryParameters( // 폼 필드 파라미터 문서화!
                                parameterWithName("name").description("User's name"),
                                parameterWithName("age").optional().description("User's age")
                        )
                ));
    }

    @Test
    void handleFormSubmit2_1() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a test file".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/test2")
                        .file(mockMultipartFile)
                        .param("name", "testName")
                        .param("age", "30")
                        .header("X-USER-ID", UUID.randomUUID())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestParts( // 파일 파트 문서화
                                partWithName("file").description("The file to upload")
                        ),
                        formParameters( // 폼 필드 파라미터 문서화!
                                parameterWithName("name").description("User's name"),
                                parameterWithName("age").description("User's age")
                        )
                ));
    }

    @Test
    void handleFormSubmit2_2() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a test file".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/test2")
                        .file(mockMultipartFile)
                        .formField("name", "testName")
                        .formField("age", "30")
                        .header("X-USER-ID", UUID.randomUUID())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestParts( // 파일 파트 문서화
                                partWithName("file").attributes(getTypeFormat(JsonFieldType.OBJECT)).description("The file to upload")
                        ),
                        formParameters( // 폼 필드 파라미터 문서화!
                                parameterWithName("name").attributes(getTypeFormat(JsonFieldType.STRING)).description("User's name"),
                                parameterWithName("age").attributes(getTypeFormat(JsonFieldType.STRING)).optional().description("User's age")
                        )
                ));
    }

    @Test
    void handleFormSubmit3() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a test file".getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "This is a test file".getBytes(StandardCharsets.UTF_8)
        );

        TestDto2 testDto = new TestDto2("testName", 30);
        MockMultipartFile jsonFile = new MockMultipartFile(
                "testDto", "testDto.json", MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(testDto));

        mockMvc.perform(multipart("/test3")
                        .file(file1)
                        .file(file2)
                        .file(jsonFile)
                        .header("X-USER-ID", UUID.randomUUID())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andDo(this.document.document(
                        requestParts( // 파일 파트 문서화
                                partWithName("files").attributes(getTypeFormat(JsonFieldType.ARRAY))
                                        .description("The file to upload"),
                                partWithName("testDto").attributes(getTypeFormat(JsonFieldType.OBJECT))
                                        .description("JSON representation of TestDto2 object")
                        ),
                        requestPartFields("testDto", // JSON 파트 필드 문서화
                                fieldWithPath("name").type(JsonFieldType.STRING)
                                        .description("User's name"),
                                fieldWithPath("age").optional().description("User's age")
                        )
//                        requestPartFields("files", // 파일 파트 필드 문서화
//                                fieldWithPath("[]").attributes(getTypeFormat(JsonFieldType.OBJECT))
//                                        .description("Uploaded files")
//                        )
                ));
    }

}