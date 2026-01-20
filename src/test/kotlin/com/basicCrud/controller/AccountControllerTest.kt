package com.basicCrud.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.basicCrud.service.AccountService
import com.basicCrud.controller.dto.SignupRequest
import com.basicCrud.security.JwtProvider
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given // given() 함수 (Borders 기반의 Mocking을 위해 필요)
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.* // GET, POST, PATCH 함수
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath // jsonPath() 함수 (응답 본문의 JSON 데이터 검증시 사용)
import org.springframework.restdocs.headers.HeaderDocumentation.* // Header 문서화
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.restdocs.operation.preprocess.Preprocessors.* // 요청/응답 데이터 가공
import org.springframework.restdocs.request.RequestDocumentation.parameterWithName
import org.springframework.restdocs.request.RequestDocumentation.queryParameters

@WebMvcTest(AccountController::class) // 컨트롤러 계층만 테스트
@AutoConfigureRestDocs // Rest Docs 설정 활성화
class AccountControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val objectMapper: ObjectMapper
) {

    @MockitoBean // 스프링부트 3.4.0 이상에서 @MockBean deprecated
    private lateinit var accountService: AccountService

    @MockitoBean
    private lateinit var jwtProvider: JwtProvider

    @Test // 회원가입 성공
    @WithMockUser
    @DisplayName("회원가입 API 호출 시 문서가 생성되어야 한다")
    fun signupApiTest() {
        // 1. Given: 테스트 데이터 준비
        val requestMap = mapOf("id" to "testuser", "password" to "pass123")

        // 2. When & Then: API 호출 및 검증
        mockMvc.perform(
                post("/api/v1/accounts/signup")
                    .with(csrf()) // 보안 검사를 통과하기 위한 가짜 토큰
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(requestMap))
            )
            .andExpect(status().isCreated) // 201 응답 확인
            .andDo(
                document("signup-account", // 문서 스니펫 이름
                    requestFields(
                        fieldWithPath("id").description("가입할 사용자 아이디"),
                        fieldWithPath("password").description("가입할 사용자 비밀번호")
                    )
                )
            )
    }

    @Test // 회원가입 실패
    @WithMockUser
    @DisplayName("회원가입 시 아이디가 중복되면 400 에러와 메시지를 반환해야 한다")
    fun duplicateApiTest() {
        // 1. Given: 서비스에서 IllegalArgumentException을 던지도록 설정 (Mocking)
        val errorMessage = AccountService.DUPLICATE_ID_MESSAGE
        val requestMap = mapOf("id" to "duplicateUser", "password" to "password123")

        given(accountService.signUp(any(SignupRequest::class.java)))
            .willThrow(IllegalArgumentException(AccountService.DUPLICATE_ID_MESSAGE))

        // 2. When & Then: API 호출 및 400 Bad Request 검증 및 문서화
        mockMvc.perform(
            post("/api/v1/accounts/signup")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMap))
            )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value(errorMessage)) // 컨트롤러에서 보낸 에러 메시지 확인
            .andDo(
                document("signup-account-duplicate", // 새로운 문서 조각 폴더명
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    // 문서에 응답 필드 설명 추가
                    responseFields(
                        fieldWithPath("error").description("에러 메시지")
                    )
                )
            )
    }

    @Test // 로그인 성공
    @WithMockUser
    @DisplayName("로그인 시 회원 ID가 존재하고 비밀번호가 일치하면 JWT 인증 토큰을 발행해야 한다")
    fun loginApiTest() {
        val token = "eyJhbGciOiJIUzI1NiJ9..." // 가짜 토큰
        given(accountService.login(anyString(), anyString())).willReturn(token)

        mockMvc.perform(
            get("/api/v1/accounts/login")
                .param("id", "testuser")
                .param("password", "pass123")
        )
            .andExpect(status().isOk)
            .andDo(
                document("login-account",
                    queryParameters(
                        parameterWithName("id").description("사용자 아이디"),
                        parameterWithName("password").description("사용자 비밀번호")
                    ),
                    responseFields(
                        fieldWithPath("accessToken").description("발급된 JWT 인증 토큰")
                    )
                )
            )
    }

    @Test // 로그인 실패
    @WithMockUser
    @DisplayName("로그인 시 회원 ID가 존재하지 않거나 비밀번호가 일치하지 않을 경우 401 에러를 반환해야 한다")
    fun loginFailTest() {
        // 1. Given: 서비스에서 로그인 실패 시 예외를 던지도록 설정
        val errorMessage = AccountService.DIFFERENT_ID_OR_PW_MESSAGE

        // 어떤 문자열이 들어와도 예외를 던지도록 설정
        given(accountService.login(anyString(), anyString()))
            .willThrow(IllegalArgumentException(errorMessage))

        // 2. When & Then
        mockMvc.perform(
            get("/api/v1/accounts/login")
                .param("id", "wrongUser")
                .param("password", "wrongPass")
        )
            .andExpect(status().isUnauthorized) // 401 Unauthorized 확인
            .andDo(
                document("login-account-fail",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint()),
                    queryParameters(
                        parameterWithName("id").description("로그인 시도 아이디"),
                        parameterWithName("password").description("로그인 시도 비밀번호")
                    )
                )
            )
    }

    @Test // 정보 수정 성공
    @WithMockUser
    @DisplayName("정보 수정 성공시 상태 204(정보 변경 성공)를 반환해야 한다")
    fun updateApiTest() {
        val updateRequest = mapOf("newPassword" to "newPass123")

        given(jwtProvider.validateToken(anyString())).willReturn(true) // 토큰 유효성 검사 결과를 true로 고정
        given(jwtProvider.getId(anyString())).willReturn("testuser") // 토큰에서 ID "testuser" 반환

        mockMvc.perform(
            patch("/api/v1/accounts/me")
                .header("Authorization", "Bearer YOUR_TOKEN_HERE") // 인증 헤더 추가
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()) // PATCH도 상태 변경이므로 필요
        )
            .andExpect(status().isNoContent)
            .andDo(
                document("update-account",
                    requestHeaders(
                        headerWithName("Authorization").description("JWT 인증 토큰 (Bearer 타입)")
                    ),
                    requestFields(
                        fieldWithPath("newPassword").description("변경할 새로운 비밀번호")
                    )
                )
            )
    }

    @Test // 정보 수정 실패
    @WithMockUser // 시큐리티 컨텍스트는 만들되, 헤더는 보내지 않음
    @DisplayName("인증 헤더(토큰) 없이 정보 수정 요청을 보낼 경우 403 Forbidden을 반환해야 한다")
    fun updateApiFailNoTokenTest() {
        val updateRequest = mapOf("newPassword" to "newPass123")

        mockMvc.perform(
            patch("/api/v1/accounts/me")
                // .header("Authorization", ...) <- 테스트를 위해 헤더 누락
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
                .with(csrf()) // CSRF 토큰은 넣어주어야 403(CSRF 위반)과 403(권한 부족)이 헷갈리지 않음
        )
            .andExpect(status().isForbidden) // 403 Forbidden 확인
            .andDo(
                document("update-account-fail",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint())
                )
            )
    }

    @Test // 회원 탈퇴 성공
    @WithMockUser
    @DisplayName("회원 탈퇴 성공시 상태 204(정보 변경 성공)를 반환해야 한다")
    fun deleteApiTest() {
        given(jwtProvider.validateToken(anyString())).willReturn(true) // 토큰 유효성 검사 결과를 true로 고정
        given(jwtProvider.getId(anyString())).willReturn("testuser") // 토큰에서 ID "testuser" 반환

        mockMvc.perform(
            delete("/api/v1/accounts/me")
                .header("Authorization", "Bearer VALID_TOKEN")
                .with(csrf())
        )
            .andExpect(status().isNoContent)
            .andDo(
                document("delete-account",
                    requestHeaders(
                        headerWithName("Authorization").description("JWT 인증 토큰 (Bearer 타입)")
                    )
                )
            )
    }

    @Test // 회원 탈퇴 실패
    @WithMockUser // 시큐리티 컨텍스트는 만들되, 헤더는 보내지 않음
    @DisplayName("인증 헤더(토큰) 없이 회원 탈퇴 요청을 보낼 경우 403 Forbidden을 반환해야 한다")
    fun deleteApiFailNoTokenTest() {
        mockMvc.perform(
            delete("/api/v1/accounts/me")
                // .header("Authorization", ...) <- 테스트를 위해 헤더 누락
                .with(csrf())
        )
            .andExpect(status().isForbidden) // 403 Forbidden 확인
            .andDo(
                document("delete-account-fail",
                    preprocessRequest(prettyPrint()),
                    preprocessResponse(prettyPrint())
                )
            )
    }

    private fun <T> any(type: Class<T>): T = org.mockito.ArgumentMatchers.any(type)
}