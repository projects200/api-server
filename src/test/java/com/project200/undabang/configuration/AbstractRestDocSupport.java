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
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.OperationRequestFactory;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessorAdapter;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.restdocs.operation.preprocess.UriModifyingOperationPreprocessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@ExtendWith(RestDocumentationExtension.class)
@AutoConfigureRestDocs
@Import(RestDocsConfiguration.class)
public abstract class AbstractRestDocSupport {

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

    @Value("${spring.profiles.active:default}")
    protected String[] activeProfiles;

    /**
     * 테스트 환경을 설정하는 메서드로, MockMvc 및 RestDocs 관련 설정을 초기화합니다.
     *
     * @param webApplicationContext 웹 애플리케이션의 ApplicationContext를 제공하는 매개변수
     * @param restDocumentation     Spring RestDocs의 RestDocumentationContextProvider 객체
     */
    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        boolean removePort = (scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80);

        UriModifyingOperationPreprocessor uriPreprocessor = Preprocessors.modifyUris()
                .scheme(scheme)
                .host(host);

        if (removePort) {
            uriPreprocessor = uriPreprocessor.removePort();
        } else {
            uriPreprocessor = uriPreprocessor.port(port);
        }

        List<OperationPreprocessor> requestPreprocessors = new ArrayList<>();
        requestPreprocessors.add(uriPreprocessor);  // 기본 URI 변경 (scheme, host, port)

        // "dev" 프로파일이 활성 상태일 때만 경로에 "/dev" 추가
        if (Arrays.asList(activeProfiles).contains("dev")) {
            addDevPathPrefixPreprocessor(requestPreprocessors);
        }

        // 공통 요청 전처리기 (항상 적용)
        requestPreprocessors.add(Preprocessors.modifyHeaders().remove("X-USER-ID"));
        requestPreprocessors.add(Preprocessors.modifyHeaders().remove("X-USER-EMAIL"));
        requestPreprocessors.add(Preprocessors.prettyPrint());

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(MockMvcRestDocumentation.documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(requestPreprocessors.toArray(new OperationPreprocessor[0]))
                        .withResponseDefaults(Preprocessors.prettyPrint())
                )
                .alwaysDo(MockMvcResultHandlers.print()) // 콘솔에 요청/응답 출력
                .alwaysDo(document)
                .addFilters(new CharacterEncodingFilter("UTF-8", true)) // 한글 깨짐 방지
                .build();
    }

    private static void addDevPathPrefixPreprocessor(List<OperationPreprocessor> requestPreprocessors) {
        OperationPreprocessor pathPrefixPreprocessor = new OperationPreprocessorAdapter() {
            private final OperationRequestFactory requestFactory = new OperationRequestFactory();

            @Override
            public OperationRequest preprocess(OperationRequest request) {
                URI originalUri = request.getUri();
                URI modifiedUri;
                try {
                    String originalRawPath = originalUri.getRawPath();
                    String pathToPrepend = "/dev";
                    String newPath;

                    if (originalRawPath == null || originalRawPath.isEmpty() || originalRawPath.equals("/")) {
                        newPath = pathToPrepend;
                    } else {
                        if (originalRawPath.startsWith(pathToPrepend + "/")) {
                            newPath = originalRawPath;
                        } else if (originalRawPath.startsWith("/")) {
                            newPath = pathToPrepend + originalRawPath;
                        } else {
                            newPath = pathToPrepend + "/" + originalRawPath;
                        }
                    }
                    newPath = newPath.replaceAll("//+", "/");

                    modifiedUri = new URI(
                            originalUri.getScheme(),
                            originalUri.getUserInfo(),
                            originalUri.getHost(),
                            originalUri.getPort(),
                            newPath,
                            originalUri.getRawQuery(),
                            originalUri.getRawFragment()
                    );
                } catch (URISyntaxException e) {
                    throw new IllegalStateException("Failed to modify URI path with /dev prefix", e);
                }
                return this.requestFactory.create(
                        modifiedUri,
                        request.getMethod(),
                        request.getContent(),
                        request.getHeaders(),
                        request.getParts(),
                        request.getCookies()
                );
            }
        };
        requestPreprocessors.add(pathPrefixPreprocessor);
    }
}
