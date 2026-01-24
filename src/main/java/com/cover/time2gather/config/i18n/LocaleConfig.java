package com.cover.time2gather.config.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.List;
import java.util.Locale;

/**
 * Locale 설정
 * Accept-Language 헤더 기반으로 언어를 결정합니다.
 * 기본값: 한국어 (ko)
 * 지원 언어: 한국어 (ko), 영어 (en)
 */
@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setSupportedLocales(List.of(Locale.KOREAN, Locale.ENGLISH));
        return resolver;
    }
}
