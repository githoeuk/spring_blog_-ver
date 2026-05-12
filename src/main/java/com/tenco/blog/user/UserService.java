package com.tenco.blog.user;

import com.tenco.blog._core.errors.Exception400;
import com.tenco.blog._core.errors.Exception404;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * 회원 가입 처리
     * @param joinDTO (사용자 회원가입 요청 정보)
     * @return User (저장된 사용자 정보)
     */
    @Transactional
    public User 회원가입(UserRequest.JoinDTO joinDTO) {
        log.info("회원가입 서비스 시작");

        userRepository.findByUsername(joinDTO.getUsername()).ifPresent(user -> {
            log.warn("회원가입 실패 - 중복된 사용자명 : {}", user.getUsername());
            throw new Exception400("이미 존재하는 사용자 이름입니다");
        });
        User user = joinDTO.toEntity();

        return userRepository.save(user);
    }

    /**
     * 로그인 처리
     * @param loginDTO (사용자가 요청한 로그인 정보)
     * @return User(조회된 정보 세션 저장용)
     */
    public User 로그인(UserRequest.LoginDTO loginDTO) {
        log.info("로그인 서비스 시작");
        User userEntity = userRepository.findByUsernameAndPassword(loginDTO.getUsername(), loginDTO.getPassword())
                .orElseThrow(() -> {
                    log.warn("로그인 실패 - 사용자 이름 또는 사용자 비번 잘못 입력");
                    return new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
                });

        return userEntity;
    }

    /**
     * 사용자 정보 조회 (프로필 정보 보기 활용)
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
     * @param id  (User PK)
     * @param updateDTO (사용자가 요청한 데이터)
     * @return User
     */
    @Transactional
    public User 회원정보수정(Integer id, UserRequest.UpdateDTO updateDTO) {
        log.info("회원정보 서비스 시작");
        User userEntity = userRepository.findById(id).orElseThrow(
                () -> new Exception404("사용자 정보를 찾을 수 없습니다"));
        // 더티 체킹 활용
        userEntity.update(updateDTO);
        return userEntity;
    }
}




