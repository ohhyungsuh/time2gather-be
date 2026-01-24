package com.cover.time2gather.util;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * 다국어 메시지 조회 유틸리티
 * Static 메서드를 통해 어디서든 메시지를 조회할 수 있습니다.
 * ThreadLocal 기반의 LocaleContextHolder를 사용하여 현재 요청의 Locale을 가져옵니다.
 */
@Component
public class MessageProvider {

    private static MessageSource messageSource;

    public MessageProvider(MessageSource messageSource) {
        MessageProvider.messageSource = messageSource;
    }

    /**
     * 현재 Locale에 맞는 메시지를 조회합니다.
     *
     * @param code 메시지 코드
     * @return 다국어 메시지
     */
    public static String getMessage(String code) {
        return getMessage(code, (Object[]) null);
    }

    /**
     * 현재 Locale에 맞는 메시지를 조회합니다. (파라미터 지원)
     *
     * @param code 메시지 코드
     * @param args 메시지 파라미터
     * @return 다국어 메시지
     */
    public static String getMessage(String code, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(code, args, code, locale);
    }

    /**
     * 특정 Locale로 메시지를 조회합니다.
     *
     * @param code   메시지 코드
     * @param locale Locale
     * @param args   메시지 파라미터
     * @return 다국어 메시지
     */
    public static String getMessage(String code, Locale locale, Object... args) {
        return messageSource.getMessage(code, args, code, locale);
    }
}
