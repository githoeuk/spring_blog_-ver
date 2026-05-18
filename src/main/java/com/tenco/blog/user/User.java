package com.tenco.blog.user;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Table(name = "user_tb")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    // 사용자명 중복 방지를 위한 유니크 제약 조건 설정
    @Column(unique = true)
    private String username;

    private String password;
    private String email;
    // 엔티티가 영속화 될 때 자동으로 현재 시간을 주입해라 pc -> db
    @CreationTimestamp
    private Timestamp createdAt;

    // User 테이블에는 이미지 파일명만 저장할 예정 (실제 데이터는 내 서버 컴퓨터 로컬에 저장할 예정)
    @Column(nullable = true) // null 허용, 기본값
    private String profileImage; // 프로필 이미지는 선택 사항

    // 새로 추가
    /*
        사용자 권한 목록
        User(1) : UserRole(N) 연관 관계를 정의함

        1. @OneToMany + @JoinCoiumn (name " user_id")
        - User 가 UserRole 리스트를 관리 한다. (단방향)
        - 실제 DB user_role_tb 테이블에 FK 컬럼은
        - user_id 명이 user_role_tb에 생성된다.

        2. CaseType.ALL (운명 공동체)
        - java 기준에서 User 저장하면 Role도 자동 저장되고
        - User 삭제하면 가지고 있던 Role들도 다 삭제가 된다.
        - DB에서 실제 delete 쿼리가 발생한다.

        3. orphanRemoval (리스트와 db를 동기화)
       - DB에서 실제 delete 쿼리가 발생한다. = true 처리

        4. fetch = FetchType.EAGER (특별 취급)
        - EAGER 전략을 쓰는 이유 - 데이터 양이 적기 떄문
        -  그래서 한번에 가져오는것이 편리하다

     */

    // 연관 관계 설정
    // 사용자 1명이 한개 이상의 권한을 가질 수 있음
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private List<UserRole> roles = new ArrayList<>();

    // DB에서는 Enum타입을 문자열로 바라보도록
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) // null 허용 안함
    @ColumnDefault("'LOCAL'") // 어노테이션으로 디폴트값 선언 방법 ( 문자열 일 경우  '' 사용ㄴ)
    private OAuthProvider oAuthProvider;

    @Builder
    public User(Integer id, String username, String password,
                String email, Timestamp createdAt,
                String profileImage) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.createdAt = createdAt;
        this.profileImage = profileImage;
    }

    // 편의 기능 추가 - 회원 정보 수정
    public void update(UserRequest.UpdateDTO updateDTO, String  newProfileImageFileName) {
        this.password = updateDTO.getPassword();
        this.profileImage = newProfileImageFileName;
        // Dirty Checking 처리
    }


    // USER 엔티티 권한 관련 편의 기능 만들기
    
    // Role = Role.ADMINm Role.USER
    public void addRole(Role role){
        this.roles.add(UserRole.builder()
                .role(role)
                .build());
        //this.roles.get(0) = new UserRole(1,Role.USER);
    } // end of addRole

    // 해당 Role를 가지고 있는지 여부 확인
    // boolean isAdmin = user.hasRole(Role.ADMIN);
    public boolean hasRole(Role role){
        // 1. 방어적 코드 작성
        if(this.roles == null || this.roles.isEmpty()){
            // Role이 설정되지 않은 상태를 의미
            return false;
        }

        // list형식이기 때문에 반복문 사용해서 확인해야 한다.
        // this.roles 크기만큼 전부 확인해서 권한이 같은지 확인
        for(UserRole userRole:this.roles){
            if (userRole.getRole() == role){
                return true;
            }
        }
        return false;
    } // end of hasRole

    // get,setter 에서 'is' 가 붙는다
    // - 머스태치에서 is 생략하고 admin으로 접근이 가능하다?
    // boolean을 통해 관리자 여부 확인 메서드
    public boolean isAdmin(){
        return hasRole(Role.ADMIN);
    }

    // 머스태치 화면에서 사용할 편의 메서드 1
    public String getRoleDisplay(){
        return isAdmin() ? "ADMIN" : "USER";
    }

    // 머스태치 화면에서 사용할 편의 메서드 2
    // OAuthProvider 값에 따라 경로 변수를 다르게 리턴
    public String getProfilePath(){
        if (this.profileImage == null){
            return null;
        }
        // 이미지 경로가 http로 시작 ( 소셜 가입 )
        if (this.profileImage.startsWith("http")){
            return this.profileImage;
        }
        // 로컬 이미지 (서버 기준 경로)
        return "/images/" + this.profileImage;
    } // getProfilePath

} // end of class
