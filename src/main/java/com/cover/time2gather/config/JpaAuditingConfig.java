package com.cover.time2gather.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA Auditing 설정
 * @WebMvcTest에서 제외하기 위해 별도 Configuration으로 분리
 */
@Configuration
@EnableJpaAuditing
@Profile("!firestore")
public class JpaAuditingConfig {
}
