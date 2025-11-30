package com.cover.time2gather.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 리소스 파일 로드 유틸리티
 */
@Slf4j
public class ResourceLoader {

    private ResourceLoader() {
    }

    /**
     * classpath에서 텍스트 파일을 읽어 문자열로 반환
     * @param path 파일 경로 (예: "prompts/meeting-summary-kr.txt")
     * @return 파일 내용
     */
    public static String loadTextFile(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to load resource file: {}", path, e);
            throw new RuntimeException("Failed to load resource file: " + path, e);
        }
    }
}
