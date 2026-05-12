package com.tenco.blog.board;

import com.tenco.blog.user.User;
import com.tenco.blog.util.MyDateUtil;
import lombok.Data;

/**
 * 게시글 응답 DTO
 * <p>
 * Open Session In View 가 false 일 때
 * 트랜잭션이 종료 되는 시점에 LAZY 로딩이 불가능하다.
 * Service 단에서 필요한 데이터를 모두 조회 또는 일부로 호출(트리거)해서 응답 DTO 변환해서 반환
 * 엔티티를 직접 반환하지 않고(Controller,View) 서비스단에서 DTO 내려줄 예정(결합도 감소)
 */
public class BoardResponse {

    // 게시글 목록 응답 DTO
    @Data
    public static class ListDTO {
        private Integer id;
        private String title;
        // username 평탄화 작업 : SSR 설계시 권장 방법, CSR 일 경우는 계층구조로 내려주는게 좋다
        private String username;
        private String createdAt;

        public ListDTO(Board board) {
            this.id = board.getId();
            this.title = board.getTitle();
            // 방어적 코드 활용
            if (board.getUser() != null) {
                this.username = board.getUser().getUsername();
            }
            if (board.getCreatedAt() != null) {
                this.createdAt = MyDateUtil.timestampFormat(board.getCreatedAt());
            }
        }
    }  // end of ListDTO inner class

    // 게시글 상세 보기 응답 DTO
    @Data
    public static class DetailDTO {
        private Integer id; // board PK
        private String title;
        private String content;
        private String username;
        private Integer userId; //  user PK
        private boolean isOwner;

        public DetailDTO(Board board) {
            this.id = board.getId();
            this.title = board.getTitle();
            this.content = board.getContent();
            if(board.getUser() != null) {
                this.username = board.getUser().getUsername();
                this.userId = board.getUser().getId();
            }
        }

        // 소유자 확인
        public boolean checkIsOwner(Integer sessionUserId) {
            if(sessionUserId == null) {
                return false;
            }
            if (sessionUserId.equals(this.userId)) {
                return true;
            } else {
                return false;
            }
        }

    } // end of DetailDTO inner class

} // end of outer class
