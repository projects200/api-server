package com.project200.undabang.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.operation.preprocess.UriModifyingOperationPreprocessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
public class AbstractRestDocSupport {

    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected RestDocumentationResultHandler document;

    @Value("${restdocs.uris.scheme}")
    private String scheme;

    @Value("${restdocs.uris.host}")
    private String host;

    @Value("${restdocs.uris.port}")
    private int port;

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        boolean removePort = (scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80);

        UriModifyingOperationPreprocessor uriPreprocessor = Preprocessors.modifyUris()
                .scheme(scheme)
                .host(host);

        if (removePort) {
            uriPreprocessor = uriPreprocessor.removePort();
        } else {
            uriPreprocessor = uriPreprocessor.port(port);
        }

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentation)
                                .operationPreprocessors()
                                .withRequestDefaults(
                                        uriPreprocessor, // 포트 443은 HTTPS 기본 포트
                                        Preprocessors.modifyHeaders().remove("X-USER-ID"), // X-USER-ID 헤더 제거
                                        Preprocessors.prettyPrint()
                                )
                                .withResponseDefaults(Preprocessors.prettyPrint())
                )
                .alwaysDo(MockMvcResultHandlers.print()) // 콘솔에 요청/응답 출력
                .alwaysDo(document)
                .addFilters(new CharacterEncodingFilter("UTF-8", true)) // 한글 깨짐 방지
                .build();
    }
}
