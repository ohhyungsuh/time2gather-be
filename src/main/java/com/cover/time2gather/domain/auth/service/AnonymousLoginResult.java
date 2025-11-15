package com.cover.time2gather.domain.auth.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AnonymousLoginResult {
    private final String jwtToken;
    private final boolean newUser;
    private final Long userId;
    private final String displayName;  // Meeting 스코프 제거한 이름
}

