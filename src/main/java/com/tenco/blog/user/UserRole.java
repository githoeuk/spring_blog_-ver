package com.tenco.blog.user;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "user_role_tb",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_role",
                        columnNames = {"user_id, role"})
        })
//  uniqueConstraints -  복합키
public class UserRole {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        // private Integer user_id 컬럼은 User(부모 엔티티)에서
        // 명시되어 자동 생성됨.

        @Enumerated(EnumType.STRING)  // Mysql - 상수로 관리하는데 우리는 문자열로 사용중이다.
        @Column(nullable = false,  length = 20)
        private Role role;

        @Builder
        public UserRole( Integer id, Role role) {
                this.id = id; //UserRole의 Pk
                this.role = role;
        }

} // end of class

