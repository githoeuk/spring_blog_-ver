package com.tenco.blog.user;


import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

public class UserResponse {


//    {"access_token":"d2os43AwteFT366laAyO_b9Dx9D4Qm8sAAAAAQoXAc8AAAGeKk9cPK-b-4epDDEo",
//            "token_type":"bearer",
//            "refresh_token":"206tYCDw3N-rkQVzPLk8whkDSgxBrbn_AAAAAgoXAc8AAAGeKk9cNq-b-4epDDEo",
//            "expires_in":21599,
//            "scope":"profile_image profile_nickname",
//            "refresh_token_expires_in":5183999}
    @Data
    @NoArgsConstructor
    @JsonNaming(value = PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OAuthToken{

        private String accessToken;
        private String tokenType;
        private String refreshToken;
        private String expiresIn;
        private String scope;
        private String refreshTokenExpiresIn;
    }

    /**
     * 카카오 사용자 정보 응답
     */
    @Data
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class KakaoProfile {
        private Long id;
        private KakaoAccount kakaoAccount;

        @Data
        @NoArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class KakaoAccount {
            private Profile profile;

            @Data
            @NoArgsConstructor
            @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
            public static class Profile {
                private String nickname;
                private String thumbnailImageUrl;
                private String profileImageUrl;
                private Boolean isDefaultImage;
                private Boolean isDefaultNickname;
            }
        }
    }

} // end of outer class
