package com.project200.undabang.configuration;

import org.springframework.restdocs.headers.HeaderDescriptor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.project200.undabang.configuration.DocumentFormatGenerator.getTypeFormat;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;

public class RestDocsUtils {


    /**
     * Access Token을 나타내는 HTTP 헤더를 정의합니다.
     * Header의 이름은 "Authorization"이며, 값은 "Bearer {accessToken}" 형식입니다.
     * <p>
     * 이 헤더는 클라이언트가 인증된 사용자임을 나타내기 위해 사용되며,
     * 요청 시 Access Token을 포함해야 합니다.
     */
    public static final HeaderDescriptor HEADER_ACCESS_TOKEN =
            headerWithName("Authorization")
                    .attributes(getTypeFormat(JsonFieldType.STRING))
                    .description("Bearer {accessToken} 형식의 Access Token입니다. /api로 시작하는 api의 사용자 인증 시 사용됩니다.");


    /**
     * ID Token을 나타내는 HTTP 헤더를 정의합니다.
     * Header의 이름은 "ID-TOKEN"이며, 값은 "Bearer {idToken}" 형식입니다.
     * <p>
     * 이 헤더는 클라이언트가 인증된 사용자임을 나타내기 위해 사용되며,
     * 요청 시 ID Token을 포함해야 합니다.
     */
    public static final HeaderDescriptor HEADER_ID_TOKEN =
            headerWithName("Authorization")
                    .attributes(getTypeFormat(JsonFieldType.STRING))
                    .description("Bearer {idToken} 형식의 ID Token입니다. /auth로 시작하는 api의 인증 시 사용됩니다.");

    /**
     * 공통적으로 사용되는 X-USER-ID 요청 헤더에 대한 HeaderDescriptor 상수
     */
    public static final HeaderDescriptor HEADER_X_USER_ID =
            headerWithName("X-USER-ID").attributes(getTypeFormat(JsonFieldType.STRING))
                    .description(" UUID 형식의 사용자 식별자입니다.");

    /**
     * 회원가입 진행시 사용되는 X-USER-EMAIL 요청에 대한 HeaderDescriptor 상수
     */
    public static final HeaderDescriptor HEADER_X_USER_EMAIL =
            headerWithName("X-USER-EMAIL").attributes(getTypeFormat(JsonFieldType.STRING))
                    .description("사용자 이메일입니다. 회원가입 시 사용됩니다. 중복된 이메일은 허용되지 않습니다.");


    /**
     * 공통 응답 필드 설명을 생성하고, 'data' 필드 내의 구체적인 필드 설명을 추가합니다.
     * @param dataFields 'data' 객체 내부에 포함될 필드들에 대한 FieldDescriptor 배열 (경로는 "data.필드명" 형태여야 함)
     * @return 공통 필드 + data 필드를 포함한 전체 FieldDescriptor 배열
     */
    public static FieldDescriptor[] commonResponseFields(FieldDescriptor... dataFields) {
        // 공통 응답 필드 정의
        List<FieldDescriptor> commonFields = new ArrayList<>(Arrays.asList(
                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN)
                        .description("API 호출 성공 여부입니다. 성공시 true, 실패시 false입니다."),
                fieldWithPath("code").type(JsonFieldType.STRING)
                        .description("API 결과 코드입니다. 예: SUCCESS, CREATED, UPDATED, DELETED 등"),
                fieldWithPath("message").type(JsonFieldType.STRING)
                        .description("API 결과 메시지입니다. 성공/실패에 대한 설명을 포함합니다."),
                // 'data' 필드 자체에 대한 설명 (타입은 보통 OBJECT 또는 VARIES)
                fieldWithPath("data").type(JsonFieldType.OBJECT).optional()
                        .description("실제 응답 데이터입니다. 성공 시 포함되며, 실패 시 null이거나 관련 오류 메시지를 포함할 수 있습니다.")
        ));

        // 'data' 내부의 구체적인 필드들 추가
        commonFields.addAll(Arrays.asList(dataFields));

        return commonFields.toArray(new FieldDescriptor[0]);
    }

    /**
     * 공통 응답 필드 설명을 생성하고, 'data'가 리스트일 경우 리스트 내부 항목의 필드 설명을 추가합니다.
     * @param dataListItemFields 'data' 리스트의 각 항목 내부에 포함될 필드들에 대한 FieldDescriptor 배열 (경로는 "data[].필드명" 형태여야 함)
     * @return 공통 필드 + data 리스트 필드를 포함한 전체 FieldDescriptor 배열
     */
    public static FieldDescriptor[] commonResponseFieldsForList(FieldDescriptor... dataListItemFields) {
        List<FieldDescriptor> commonFields = new ArrayList<>(Arrays.asList(
                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN)
                        .description("API 호출 성공 여부입니다. 성공시 true, 실패시 false입니다."),
                fieldWithPath("code").type(JsonFieldType.STRING)
                        .description("API 결과 코드입니다. 예: SUCCESS, CREATED, UPDATED, DELETED 등"),
                fieldWithPath("message").type(JsonFieldType.STRING)
                        .description("API 결과 메시지입니다. 성공/실패에 대한 설명을 포함합니다."),
                // 'data' 필드가 리스트임을 명시
                fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("실제 응답 데이터 배열입니다. " +
                        "성공 시 포함되며, 실패 시 빈 배열 []이거나 관련 오류 메시지를 포함할 수 있습니다. " +
                        "각 항목은 'data[].필드명' 형태로 정의됩니다.")
        ));

        // 'data[]' 내부의 구체적인 필드들 추가
        commonFields.addAll(Arrays.asList(dataListItemFields));

        return commonFields.toArray(new FieldDescriptor[0]);
    }

    /**
     * 공통 응답 필드 설명만 필요한 경우 (예: data가 없는 성공/실패 응답)
     * @return 공통 필드만을 포함한 FieldDescriptor 배열
     */
    public static FieldDescriptor[] commonResponseFieldsOnly() {
        return new FieldDescriptor[]{
                fieldWithPath("succeed").type(JsonFieldType.BOOLEAN)
                        .description("API 호출 성공 여부입니다. 성공시 true, 실패시 false입니다."),
                fieldWithPath("code").type(JsonFieldType.STRING)
                        .description("API 결과 코드입니다. 예: SUCCESS, CREATED, UPDATED, DELETED 등"),
                fieldWithPath("message").type(JsonFieldType.STRING)
                        .description("API 결과 메시지입니다. 성공/실패에 대한 설명을 포함합니다."),
                // 'data' 필드가 없거나 null임을 명시
                fieldWithPath("data").type(JsonFieldType.NULL)
                        .description("실제 응답 데이터가 없거나 null인 경우에 사용됩니다."). optional()
        };
    }
}