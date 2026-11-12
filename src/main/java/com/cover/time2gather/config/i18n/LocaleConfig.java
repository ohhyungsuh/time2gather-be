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
 * 기본값: 영어 (en)
 * 지원 언어는 프론트엔드 locale registry와 함께 관리합니다.
 */
@Configuration
public class LocaleConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.ENGLISH);
        resolver.setSupportedLocales(List.of("en", "zh-CN", "zh", "hi", "es", "ar", "fr", "bn", "pt", "ru", "id", "ur", "de", "ja", "pcm", "ar-EG", "mr", "vi", "te", "ha", "tr", "ko", "th").stream().map(Locale::forLanguageTag).toList());
        return resolver;
    }
}
