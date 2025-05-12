package com.project200.undabang.member.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project200.undabang.common.context.UserContextHolder;
import com.project200.undabang.member.dto.request.SignUpRequestDto;
import com.project200.undabang.member.dto.response.SignUpResponseDto;
import com.project200.undabang.member.enums.MemberGender;
import com.project200.undabang.member.service.MemberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@AutoConfigureRestDocs
@SpringBootTest
class AuthRestControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Mock
    private MemberService memberServiceMock;

    @Autowired
    private AuthRestController authRestController;

    private MockedStatic<UserContextHolder> userContextHolderMock;

    private final UUID TEST_UUID = UUID.randomUUID();
    private final String TEST_EMAIL = "test@email.com";
    private final String TEST_NICKNAME = "05년생헬창프로지망생";


    @BeforeEach
    void setUp(WebApplicationContext webApplicationContext, RestDocumentationContextProvider restDocumentation) {
        MockitoAnnotations.openMocks(this);
        // UserContextHolder 모킹 설정
        userContextHolderMock = Mockito.mockStatic(UserContextHolder.class);
        userContextHolderMock.when(UserContextHolder::getUserId).thenReturn(TEST_UUID);
        userContextHolderMock.when(UserContextHolder::getUserEmail).thenReturn(TEST_EMAIL);

        // AuthRestController의 MemberService 빈을 Mock 서비스로 교체
        ReflectionTestUtils.setField(authRestController, "memberService", memberServiceMock);

        // MockMvc 설정 - 인터셉터 건너뛰게 설정 (standaloneSetup)
        this.mockMvc = MockMvcBuilders.standaloneSetup(authRestController)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @AfterEach
    void tearDown(WebApplicationContext webApplicationContext) {
        if(userContextHolderMock != null){
            userContextHolderMock.close();
        }

        MemberService originalService = webApplicationContext.getBean(MemberService.class);
        ReflectionTestUtils.setField(authRestController, "memberService", originalService);
    }

    @Test
    @DisplayName("회원가입 API 테스트")
    void signUpAPITest() throws Exception{
        //given
        SignUpRequestDto requestDto = new SignUpRequestDto();
        requestDto.setMemberNickname(TEST_NICKNAME);
        requestDto.setMemberGender(MemberGender.M);
        requestDto.setMemberBday(LocalDate.of(2025,1,1));

        SignUpResponseDto respDto = SignUpResponseDto.builder()
                .memberId(TEST_UUID)
                .memberEmail(TEST_EMAIL)
                .memberNickname(TEST_NICKNAME)
                .memberGender("남")
                .memberBday(LocalDate.of(2025, 1, 1))
                .memberDesc("프로헬창이되기위한여정의시작")
                .memberScore(35)
                .memberCreatedAt(LocalDateTime.now())
                .build();

        // 가짜 서비스의 결과를 지정함
        Mockito.doReturn(respDto).when(memberServiceMock).memberSignUp(Mockito.any(SignUpRequestDto.class));

        //when, then
        mockMvc.perform(post("/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                        .header("X-USER-ID", TEST_UUID.toString())
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(TEST_UUID.toString()))
                .andExpect(jsonPath("$.memberEmail").value(TEST_EMAIL))
                .andExpect(jsonPath("$.memberNickname").value(TEST_NICKNAME))
                .andExpect(jsonPath("$.memberGender").value("남"))
                .andExpect(jsonPath("$.memberBday").exists())
                .andExpect(jsonPath("$.memberScore").value(35))
                .andExpect(jsonPath("$.memberCreatedAt").exists())
                .andDo(document("member-signup",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("memberNickname").description("사용자 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("memberGender").description("사용자 성별").type(JsonFieldType.STRING),
                                fieldWithPath("memberBday").description("사용자 생년월일").type(JsonFieldType.STRING)
                        ),responseFields(
                                fieldWithPath("memberId").description("가입된 회원 아이디").type(JsonFieldType.STRING),
                                fieldWithPath("memberEmail").description("가입된 이메일").type(JsonFieldType.STRING),
                                fieldWithPath("memberNickname").description("가입된 닉네임").type(JsonFieldType.STRING),
                                fieldWithPath("memberGender").description("가입된 성별").type(JsonFieldType.STRING),
                                fieldWithPath("memberBday").description("가입된 회원 생일").type(JsonFieldType.ARRAY),
                                fieldWithPath("memberDesc").description("회원 설명").type(JsonFieldType.STRING).optional(),
                                fieldWithPath("memberScore").description("회원 점수").type(JsonFieldType.NUMBER),
                                fieldWithPath("memberCreatedAt").description("가입 일시").type(JsonFieldType.ARRAY)
                        )
                ));
    }


    @Test
    @DisplayName("잘못된 요청에 대한 회원가입 API 테스트")
    void invalidSignUpAPITest() throws Exception {
        // 유효하지 않은 DTO 생성
        SignUpRequestDto invalidRequestDto = new SignUpRequestDto();
        // 필수 필드 누락

        mockMvc.perform(post("/v1/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequestDto)))
                .andExpect(status().isBadRequest());
    }
}