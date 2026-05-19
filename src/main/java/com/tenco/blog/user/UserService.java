package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog._core.errors.Exception500;
import com.tenco.blog._core.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * User 관련 비즈니스 로직을 처리하는 Service 계층
 * Controller 와 Repository 사이에서 실제 업무 로직을 담당
 */
@Slf4j
@Service // IoC
@RequiredArgsConstructor // DI
@Transactional(readOnly = true) // 기본적인 읽기 전용 트랜잭션 처리 , 조회시 더티 체킹 안 일어남
public class UserService {

    private final UserRepository userRepository;

    // DI처리 - 암호화 처리
    private final PasswordEncoder passwordEncoder;

    // 초기 파라미터 값을 가져오는 방법 ( 소셜 로그인 관련 )
    @Value("${oauth.kakao.client-id}")// lombok아님
    private String kakaoClientId;
    @Value("${oauth.kakao.client-secret}")
    private String kakaoClientSecret;
    @Value("${tenco.key}")
    private String tencoKey;


    /**
     * 회원 가입 처리
     *
     * @param joinDTO (사용자 회원가입 요청 정보)
     * @return User (저장된 사용자 정보)
     */
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        log.info("회원가입 서비스 시작");

        // 회원가입 시 사용자 이름 중복 체크
        userRepository.findByUsername(joinDTO.getUsername()).ifPresent(user -> {
            log.warn("회원 가입 실패 - 중복된 사용자명 : {}", user.getUsername());
            throw new Exception400("이미 존재하는 사용자 이름입니다.");
        });

        String profileImageName = null;
        if (joinDTO.getProfileImage() != null && !joinDTO.getProfileImage().isEmpty()) {
            try {
                if (!FileUtil.isImageFile(joinDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 가능합니다");
                }
                profileImageName = FileUtil.saveFile(joinDTO.getProfileImage(), FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                throw new Exception500("프로필 이미지 저장 실패 ");
            }
        } // 이미지 저장

        // userEntity로 저장
        User userEntity = joinDTO.toEntity(profileImageName);

        // 비밀번호 암호화
        String encodePWD = passwordEncoder.encode(joinDTO.getPassword());
        userEntity.setPassword(encodePWD);

        return userRepository.save(userEntity);
    } // end of 회원가입



    /**
     * 로그인 처리
     *
     * @param loginDTO (사용자가 요청한 로그인 정보)
     * @return User(조회된 정보 세션 저장용)
     */
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        log.info("로그인 서비스 시작");

        // 사용자 계정 확인
        User userEntity = userRepository.findByUsernameWithRoles(loginDTO.getUsername()).orElseThrow(() -> {
            return new Exception400("사용자명 혹은 비밀번호가 틀렸습니다.");
        });

        // 사용자 비밀번호 확인
        if (!passwordEncoder.matches(loginDTO.getPassword(), userEntity.getPassword())) {
            throw new Exception400("사용자명 혹은 비밀번호가 틀렸습니다.");
        }

        return userEntity;
    } //end of login

    /**
     * 사용자 정보 조회 (프로필 정보 보기 활용)
     *
     * @param id (User PK)
     * @return UserEntity
     */
    public User 회원정보수정화면(Integer id) {
        log.info("사용자 정보 서비스 시작");
        User userEntity = userRepository.findById(id).orElseThrow(() -> {
            log.warn("사용자 정보 조회 실패");
            return new Exception404("사용자 정보를 찾을 수 없습니다");
        });
        return userEntity;
    }


    /**
     * 사용자 정보 수정 처리 (프로필 업데이트)
     *
     * @param id        (User PK)
     * @param updateDTO (사용자가 요청한 데이터)
     * @return User
     */
    @Transactional
    public User 회원정보수정(Integer id, UserRequest.UpdateDTO updateDTO) {
        log.info("회원정보 서비스 시작");

        String newPassword = null;
        String newProfileImageFilename = null;

        // 조회부터 시작
        User userEntity = userRepository.findById(id).orElseThrow(() -> new Exception404("사용자 정보를 찾을 수 없습니다"));

        // 인가 처리 - 권한 확인
        if (!userEntity.getId().equals(id)) {
            throw new Exception403("권한이 없습니다.");
        }

        // 로직 처리 1 - 사용자가 비밀번호를 입력 했을 경우 갱신
        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isBlank()) {
            // 유효성 검사 (위치 변경 - controller에서 service로 )
            updateDTO.validate();

            String rawPassword = updateDTO.getPassword();
            updateDTO.setPassword(passwordEncoder.encode(rawPassword));
        } else {
            updateDTO.setPassword(null);
        }

        // 로직 처리 2 - 사용자가 새로운 이미지를 등록했을 경우
        if (updateDTO.getProfileImage() != null && !updateDTO.getProfileImage().isEmpty()) {
            try {

                // 유효성 검사
                if (!FileUtil.isImageFile(updateDTO.getProfileImage())) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다.");
                }

                // 새이미지 로컬 폴더에 저장 ( 중복되지 않을 이미지 파일 이름을 리턴 )
                newProfileImageFilename = FileUtil.saveFile(updateDTO.getProfileImage(), FileUtil.IMAGES_DIR);
                updateDTO.setProfileImageFilename(newProfileImageFilename);

                // 기존 이미지 삭제
                String oldProfileImageName = userEntity.getProfileImage();
                if (oldProfileImageName != null) {
                    FileUtil.deleteFile(userEntity.getProfileImage(), FileUtil.IMAGES_DIR);
                }

            } catch (IOException e) {
                throw new Exception400("파일 저장이 실패했습니다. ");
            }

        } else {
            updateDTO.setProfileImageFilename(userEntity.getProfileImage());
        }

        // 더티체킹
        userEntity.update(updateDTO);
        return userEntity;
    }

    @Transactional
    public User 프로필이미지삭제(Integer id) {

        // 1. 정보 조회

        User userEntity = userRepository.findById(id).orElseThrow(() -> new Exception404("사용자를 찾을 수 없습니다."));

        // 인가 처리
        if (userEntity.getId().equals(id) == false) {
            throw new Exception403("프로필 이미지 삭제 권한 없음");
        }

        // 3. 이미지가 등록되어 있으면 삭제처리
        String profileImage = userEntity.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()) {
            // 내 서버 컴퓨터에 저장된 (c:/upload) 파일 삭제
            try {
                FileUtil.deleteFile(profileImage, FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                System.err.println("프로필 이미지 삭제 시 오류 발생 : " + e.getMessage());
            }
        }

        // UserEntity.profileImage = null 처리 ( 더티 체킹 )
        // 1차 캐쉬에 저장된 User 정보 수정 - 트랜잭션이 종료되면 반영 (더티 체킹)
        userEntity.setProfileImage(null);


        return userEntity;
    } // end of 프로필이미지삭제


    public User 사용자이름조회(String username) {
        return userRepository.findByUsername(username).orElse(null);
    } // 사용자 이름 조회


    // 1. 발급 받은 인가 코드(code)로  액세스 토큰 발급 요청(인가권한(코드)필요)
    private UserResponse.OAuthToken 카카오엑세스토큰발급(String code) {
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

        // http 요청 후 응답 대기
        ResponseEntity<UserResponse.OAuthToken> response1 =
                restTemplate1.exchange(
                        "https://kauth.kakao.com/oauth/token",
                        HttpMethod.POST,
                        request, // Http 메세지 주입
                        UserResponse.OAuthToken.class
                );

        UserResponse.OAuthToken oAuthToken = response1.getBody();
        return oAuthToken;
    } // end of 카카오엑세스토큰발급(oAuthToken.getAccessToken());


    // 2. 발급 받은 엑세스 토큰으로 사용자 카카오 프로필 조회
    private UserResponse.KakaoProfile 카카오프로필조회(String token) {

        // 발급 받은 엑세스 토큰으로 해당 사용자의 정보 요청
        String accessToken = token; // 토큰
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

        UserResponse.KakaoProfile kakaoProfile = response2.getBody();
        return kakaoProfile;
    } // end of 카카오프로필조회

    private User 카카오조회및자동회원가입처리(UserResponse.KakaoProfile kakaoProfile) {

        // 고유한 username 생성( 중복 방지 용)
        String username = kakaoProfile.getKakaoAccount().getProfile().getNickname() + "_" + kakaoProfile.getId();

        // 회원 가입 여부 확인 ( 이름으로 확인 )
        User user = 사용자이름조회(username);

        if (user == null) {
            log.info("기존 회원 아님 , 자동 회원 가입 진행");
            User newUser = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(tencoKey)) // 임시 비밀번호 (노출 방지)
                    .email(username + "@kakao.com") // 임의로 이메일 생성(추후 DB 제약 방지)
                    .oAuthProvider(OAuthProvider.KAKAO) // 로그인 경로 설정
                    .build();

            String profileImage = kakaoProfile.getKakaoAccount().getProfile().getProfileImageUrl();
            if (profileImage != null && !profileImage.isEmpty()) {
                newUser.setProfileImage(profileImage); // 카카오 서버에서 받은 이미지 설정
            }

            // 회원 강비 후 상태 저장
            user = userRepository.save(newUser);
        } else {
            System.out.println("이미 가입된 사용자입니다.");
        }

        return user;
    } // end of 카카오조회및자동회원가입처리


    @Transactional
    public User 카카오소셜로그인(String code) {

        // private메서드로 작성

        // 1. 발급 받은 인가 코드(code)로  액세스 토큰 발급 요청(인가권한(코드)필요)
        UserResponse.OAuthToken oAuthToken = 카카오엑세스토큰발급(code);

        // 2. 발급 받은 엑세스 토큰으로 사용자 카카오 프로필 조회
        UserResponse.KakaoProfile kakaoProfile = 카카오프로필조회(oAuthToken.getAccessToken());

        // 3. 응답 받은 결과로 우리 서버에 가입 여부 조회 및 자동 회원 가입 처리
        User userEntity = 카카오조회및자동회원가입처리(kakaoProfile);

        // 4. 컨트롤러로 User 반환
        return userEntity;

    } // end of 카카오소셜로그인


} // end of class




