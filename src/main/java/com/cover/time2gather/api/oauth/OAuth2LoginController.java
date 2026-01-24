package com.cover.time2gather.api.oauth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * OAuth2 Authorization Server 로그인 페이지 컨트롤러
 */
@Controller
public class OAuth2LoginController {

    /**
     * 로그인 페이지
     * PlayMCP에서 OAuth2 인증 요청 시 이 페이지로 리다이렉트됨
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login"; // templates/login.html
    }
}
