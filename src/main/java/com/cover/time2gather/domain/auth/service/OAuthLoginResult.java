package com.cover.time2gather.domain.auth.service;

import com.cover.time2gather.domain.user.User;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class OAuthLoginResult {
    private final String jwtToken;
    private final boolean newUser;
    private final Long userId;
    private final String username;
    private final String email;
    private final String profileImageUrl;
    private final User user;
}

