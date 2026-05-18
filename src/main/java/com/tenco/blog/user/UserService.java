package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.Exception403;
import com.tenco.blog._core.errors.Exception404;
import com.tenco.blog._core.errors.Exception500;
import com.tenco.blog._core.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            log.warn("회원가입 실패 - 중복된 사용자명 : {}", user.getUsername());
            throw new Exception400("이미 존재하는 사용자 이름입니다");
        });

        // 프로필 이미지 저장 기능 구현 (선택 사항 -> 분기 처리 )
        String profileImageFilename = null;
        if (joinDTO.getProfileImage() != null && !joinDTO.getProfileImage().isEmpty()) {
            // 사용자가 프로필 이미지를 업로드 한 경우
            // 파일 처리 기능은 별도 유틸리티 클래스를 따로 만들어서 사용 (추후 동통 사용되는 구분)
            try {
                // 이미지 파일이 맞는지
                if (FileUtil.isImageFile(joinDTO.getProfileImage()) == false) {
                    throw new Exception400("이미지 파일만 업로드 가능합니다.");
                }
                // 프로필 이미지 이름 저장
               profileImageFilename = FileUtil.saveFile(joinDTO.getProfileImage(), FileUtil.IMAGES_DIR);
            } catch (Exception e) {
                // 디스크 공간 부족 or 권한 없음 시 발생
                throw new Exception500("프로필 이미지 저장 실패");
            }


        } // end of 프로필 이미지 저장

        // 코드 수정
        User user = joinDTO.toEntity(profileImageFilename);

        // 암호화 처리
        String hashPwd = passwordEncoder.encode(joinDTO.getPassword()); // 문자열로 값이 들어옴
        // 암호화 확인
        System.out.println("rawPwd : " + joinDTO.getPassword());
        System.out.println("hashPwd : " + hashPwd);

        user.setPassword(hashPwd);


        // 기본 권한 추가 (일반 사용자 )
        user.addRole(Role.USER);

        return userRepository.save(user);
    } // end of 회원가입

    /**
     * 로그인 처리
     *
     * @param loginDTO (사용자가 요청한 로그인 정보)
     * @return User(조회된 정보 세션 저장용)
     */
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        log.info("로그인 서비스 시작");

        // 1. 사용자 계정 여부 확인
        User userEntity = userRepository.findByUsernameWithRoles(loginDTO.getUsername())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 사용자 이름 또는 사용자 비번 잘못 입력");
                    return new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
                });

        // 2. 암호화 된 비밀번호 검증
        // passwordEncoder.matches(평문 비밀번호, 암호화된 비밀번호)

        if (! passwordEncoder.matches(loginDTO.getPassword(),userEntity.getPassword())){
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
        }

        return userEntity;
    }

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
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자 정보를 찾을 수 없습니다"));



        String uuidImageFileName = null;
        // 1. 파일이 들어왔는지  파일 이름으로 확인
        if (updateDTO.getProfileImage() != null && !updateDTO.getProfileImage().isEmpty()){
            try {
                // 2.1 기존 프로필 사진이 존재하는지
                String oldProfileImage = userEntity.getProfileImage(); // null or 기존이미지명
                // String newProfileImage = updateDTO.getProfileImage().getOriginalFilename(); //

                // 2.2 이미지 파일이 들어왔는지 확인 ,
                if (!FileUtil.isImageFile(updateDTO.getProfileImage())){
                    throw new Exception400("이미지 파일만 업로드 가능합니다.");
                }

                // 3. 정상 통과했다면 매개 변수로 파일 이름과 저장 디렉토리를 넣어서 FileUtil.saveFile 실행
                uuidImageFileName = FileUtil.saveFile(updateDTO.getProfileImage(),FileUtil.IMAGES_DIR);

                // 기존 이미지  삭제 처리(있을 시)
                if (oldProfileImage != null){
                    FileUtil.deleteFile(oldProfileImage,FileUtil.IMAGES_DIR);
                }

            } catch (Exception e) {
                // 디스크 부족이나 권한이 없을 때 오류 발생
                throw new Exception500("프로필 이미지 저장 실패 ");
            }


        } // end of 이미지 파일 수정

        // 더티 체킹 활용
        userEntity.update(updateDTO,uuidImageFileName); // 새로운 이미지 파일명
        return userEntity;
    }

    @Transactional
    public User 프로필이미지삭제(Integer id) {

        // 1. 정보 조회

        User userEntity =userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자를 찾을 수 없습니다.")
        );

        // 인가 처리
        if(userEntity.getId().equals(id) == false){
            throw new Exception403("프로필 이미지 삭제 권한 없음");
        }

        // 3. 이미지가 등록되어 있으면 삭제처리
        String profileImage = userEntity.getProfileImage();
        if (profileImage != null && !profileImage.isEmpty()){
            // 내 서버 컴퓨터에 저장된 (c:/upload) 파일 삭제
            try {
                FileUtil.deleteFile(profileImage,FileUtil.IMAGES_DIR);
            } catch (IOException e) {
                System.err.println("프로필 이미지 삭제 시 오류 발생 : " + e.getMessage() );
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
} // end of class




