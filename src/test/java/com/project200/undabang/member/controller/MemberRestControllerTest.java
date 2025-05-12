package com.project200.undabang.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.service.impl.MemberServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;


@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@AutoConfigureRestDocs
@SpringBootTest
class MemberRestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private MemberServiceImpl memberService;

    private MockedStatic<UserContextHolder> userContextHolderMock;

    private final UUID TEST_UUID = UUID.randomUUID();
    private final String TEST_EMAIL = "test@email.com";
    private final String TEST_NICKNAME = "00년생프로헬창지망생";

    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        // UserContextHolder 모킹 설정
        userContextHolderMock = Mockito.mockStatic(UserContextHolder.class);
        userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
        userContextHolderMock.when(UserContextHolder::getUserEmail).thenReturn(TEST_EMAIL);

        // Spring 애플리케이션 컨텍스트에서 실제 MemberServiceImpl 빈을 가로채서 Mock 서비스로 교체
        MemberServiceImpl service = webApplicationContext.getBean(MemberServiceImpl.class);
        MemberServiceImpl spyService = Mockito.spy(service);

        // MockMvc 설정
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @AfterEach
    void tearDown() {
        if(userContextHolderMock != null){
            userContextHolderMock.close();
        }
    }

    @Test
    @DisplayName("회원가입 API 테스트")
    void signUpAPITest() throws Exception{
        //given
        SignUpRequestDto requestDto = new SignUpRequestDto();
        requestDto.setMemberNickname(TEST_NICKNAME);
        requestDto.setMemberGender(MemberGender.M);
        requestDto.setMemberBday(LocalDate.of(1900,01,01));

        SignUpResponseDto respDto = new SignUpResponseDto();
        respDto.builder()
                .memberId(TEST_UUID.toString())
                .memberEmail(TEST_EMAIL)
                .memberNickname(TEST_NICKNAME)
                .memberGender("남")
                .build();
        //when

        //then
    }
}