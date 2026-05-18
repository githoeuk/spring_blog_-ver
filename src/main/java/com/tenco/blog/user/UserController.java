package com.tenco.blog.user;

import com.tenco.blog._core.util.Define;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;


@Slf4j
@Controller // IoC
@RequiredArgsConstructor // DI 처리
public class UserController {

    private final UserService userService;

    // 초기 파라미터 값을 가져오는 방법
    @Value("${oauth.kakao.client-id}")// lombok아님
    private String kakaoClientId;

    @Value("${oauth.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${tenco.key}")
    private String tencoKey;
    // 테스트용 <-- 서버가 실행되면 한번 호출 (테스트 끝나면 삭제)
    @PostConstruct
    public void init() {
        log.info("현재 적용된 클라이언트 ID 확인 : " + kakaoClientId);
        log.info("현재 적용된 클라이언트 secret 확인 : " + kakaoClientSecret);
        log.info("현재 적용된 텐코 키 확인 : " + tencoKey);
    }

    // 응답 값 확인하기
    // 1. 인가 코드 받음

    // 2. 토큰 발급 요청 (JWT -> CSR)
    // Http서버 요청 해야기 때문에
    @GetMapping("/kakao-redirect")
    public String kakaoCallback(@RequestParam(name = "code") String code, HttpSession session) {
        System.out.println("카카오 리다이렉트 값 확인 : ");

        RestTemplate restTemplate1 = new RestTemplate();

        // 헤더
        HttpHeaders headers1 = new HttpHeaders();
        headers1.add("Content-Type",
                "application/x-www-form-urlencoded;charset=utf-8");

        // 바디
        // 1. 방식 - application/json;
        // 2. 방식 -  application/x-www-form-urlencoded;
        // Map 구조로 받음 - {key=value, key=value...}
        // 장점 : URLEncoding을 알아서 해준다.

        LinkedMultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap();
        multiValueMap.add("grant_type", "authorization_code");
        multiValueMap.add("client_id", kakaoClientId);
        multiValueMap.add("redirect_uri", "http://localhost:8080/kakao-redirect");
        multiValueMap.add("code", code);
        // 최신사항 : 반드시 시크릿키 body 설정
        multiValueMap.add("client_secret", kakaoClientSecret);


        // 순서 중요!
        // (바디 , 헤더) 결합 (Http 요청 메세지 구축)  구조로 삽입해줘야 함
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity(multiValueMap, headers1);

        // 전송
        ResponseEntity<UserResponse.OAuthToken> response1 =
                restTemplate1.exchange(
                        "https://kauth.kakao.com/oauth/token",
                        HttpMethod.POST,
                        request, // Http 메세지 주입
                        UserResponse.OAuthToken.class
                );


        // ------------------------------------------------
        // 발급 받은 엑세스 토큰으로 해당 사용자의 정보 요청
        String accessToken = response1.getBody().getAccessToken(); // 토큰
        RestTemplate restTemplate2 = new RestTemplate();

        HttpHeaders headers2 = new HttpHeaders();
        // 주의! 반드시 Bearer + "공백 한칸" + 토큰
        headers2.add("Authorization", "Bearer " + accessToken);
        headers2.add("Content-Type",
                "application/x-www-form-urlencoded;charset=utf-8");

        HttpEntity request2 = new HttpEntity(headers2);

        // HTTP 요청 2
        ResponseEntity<UserResponse.KakaoProfile> response2 =
                restTemplate2.exchange(
                        "https://kapi.kakao.com/v2/user/me",
                        HttpMethod.POST,
                        request2,
                        UserResponse.KakaoProfile.class
                );

        System.out.println(response2.getStatusCode());
        System.out.println(response2.getHeaders());
        System.out.println(response2.getBody());

        // --------------------------------------------

        // 소셜 로그인 설계 방식
        // 1. 최초 사용자라면 우리 서버에 회원 가입 처리
        // 2. 회원 가입이 되어 잇는 소셜 로그인 사용자라면 바로 로그인 처리

        // 소셜 가입자 닉네임 형태 결정 -> 난수+이름 -> 123_이름
        UserResponse.KakaoProfile.KakaoAccount.Profile profile
                = response2.getBody().getKakaoAccount().getProfile();
        String username = profile.getNickname() + "_" + response2.getBody().getId();

        // 추후 변경 예정
        User userEntity = userService.사용자이름조회(username);

        // 최초 사용자일 시 회원 자동 가입
        if (userEntity == null) {
            System.out.println("최초 사용자라고 판단 ");
            // 우리 DB에 자동 회원가입 처리

            UserRequest.JoinDTO joinDTO = new UserRequest.JoinDTO();
            joinDTO.setUsername(username);
            joinDTO.setEmail(null);
            joinDTO.setPassword("1234");

            userEntity = userService.소셜회원가입(joinDTO, profile.getProfileImageUrl());

        }

        // 세션 정보 저장
        session.setAttribute(Define.SESSION_USER,userEntity);

        return "redirect:/board/list";
    }


    // 프로필 이미지 삭제 요청
    @PostMapping("/user/profile-image/delete")
    public String deleteProfileImage(HttpSession session) {
        // 인증 검사
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        // 프로필 이미지 삭제
        User updateUser = userService.프로필이미지삭제(sessionUser.getId());

        // session에 저장되어 있던 프로필 이미지를 삭제한걸 세션 동기화 처리
        session.setAttribute(Define.SESSION_USER, updateUser);

        return "redirect:/user/detail";
    }

    // 마이페이지 요청  화면
    @GetMapping("/user/detail")
    public String detailPage(Model model, HttpSession session) {

        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        model.addAttribute("user", sessionUser);

        return "user/detail";
    }

    // 회원 정보 수정 기능 요청
    @PostMapping("/user/update")
    public String updateProc(UserRequest.UpdateDTO updateDTO, HttpSession session) {
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);

        // 회원 정보 수정 요청 시 기본 비밀번호 nul 이고 프로필 이미지만 수정할 경우
        if (updateDTO.getPassword() == null || updateDTO.getPassword().isBlank()) {
            updateDTO.setPassword(sessionUser.getPassword());
        }

        updateDTO.validate();

        User updateUser = userService.회원정보수정(sessionUser.getId(), updateDTO);
        session.setAttribute(Define.SESSION_USER, updateUser);
        return "redirect:/";
    }

    // 회원 정보 화면 요청
    @GetMapping("/user/update-form")
    public String updateFormPage(HttpSession session, Model model) {
        User sessionUser = (User) session.getAttribute("sessionUser");
        User user = userService.회원정보수정화면(sessionUser.getId());
        model.addAttribute("user", user);
        return "user/update-form";
    }

    // 로그인 화면 요청
    // 주소 설계 - http://localhost:8080/login-form
    @GetMapping("/login-form")
    public String loginFormPage() {
        return "user/login-form";
    }

    // 로그인 기능 요청
    @PostMapping("/login")
    public String loginProc(UserRequest.LoginDTO reqLoginDTO, HttpSession session) {
        // 인증 검사 x, 유효성 검사 o
        reqLoginDTO.validate();
        User user = userService.로그인(reqLoginDTO);
        session.setAttribute("sessionUser", user);
        return "redirect:/";
    }


    // 로그아웃 기능 요청
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // 세션 메모리에 내 정보를 없애 버림
        session.invalidate();
        return "redirect:/";
    }

    // 회원 가입 화면 요청
    // 주소 설계 - http://localhost:8080/join-form
    @GetMapping("/join-form")
    public String joinFormPage() {
        return "user/join-form";
    }

    // 회원 가입 기능 요청
    // 주소 설계 - http://localhost:8080/join
    @PostMapping("/join")
    public String joinProc(UserRequest.JoinDTO joinDTO) throws IOException {
        //  인증검사 x, 유효성 검사 하기 o


        joinDTO.validate();

        userService.회원가입(joinDTO);
        return "redirect:/login-form";
    }

} // end of class
