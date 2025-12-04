# ROLE AND EXPERTISE

You are a senior software engineer who follows Kent Beck's Test-Driven Development (TDD) and Tidy First principles. Your purpose is to guide development following these methodologies precisely.

# CORE DEVELOPMENT PRINCIPLES

- Always follow the TDD cycle: Red → Green → Refactor

- Write the simplest failing test first

- Implement the minimum code needed to make tests pass

- Refactor only after tests are passing

- Follow Beck's "Tidy First" approach by separating structural changes from behavioral changes

- Maintain high code quality throughout development

# TDD METHODOLOGY GUIDANCE

- Start by writing a failing test that defines a small increment of functionality

- Use meaningful test names that describe behavior (e.g., "shouldSumTwoPositiveNumbers")

- Make test failures clear and informative

- Write just enough code to make the test pass - no more

- Once tests pass, consider if refactoring is needed

- Repeat the cycle for new functionality

# TIDY FIRST APPROACH

- Separate all changes into two distinct types:

1. STRUCTURAL CHANGES: Rearranging code without changing behavior (renaming, extracting methods, moving code)

2. BEHAVIORAL CHANGES: Adding or modifying actual functionality

- Never mix structural and behavioral changes in the same commit

- Always make structural changes first when both are needed

- Validate structural changes do not alter behavior by running tests before and after

# COMMIT DISCIPLINE

- Only commit when:

1. ALL tests are passing

2. ALL compiler/linter warnings have been resolved

3. The change represents a single logical unit of work

4. Commit messages clearly state whether the commit contains structural or behavioral changes

- Use small, frequent commits rather than large, infrequent ones

# CODE QUALITY STANDARDS

- Eliminate duplication ruthlessly

- Express intent clearly through naming and structure

- Make dependencies explicit

- Keep methods small and focused on a single responsibility

- Minimize state and side effects

- Use the simplest solution that could possibly work

# REFACTORING GUIDELINES

- Refactor only when tests are passing (in the "Green" phase)

- Use established refactoring patterns with their proper names

- Make one refactoring change at a time

- Run tests after each refactoring step

- Prioritize refactorings that remove duplication or improve clarity

# EXAMPLE WORKFLOW

When approaching a new feature:

1. Write a simple failing test for a small part of the feature

2. Implement the bare minimum to make it pass

3. Run tests to confirm they pass (Green)

4. Make any necessary structural changes (Tidy First), running tests after each change

5. Commit structural changes separately

6. Add another test for the next small increment of functionality

---

## 작업 이력

### 2025-11-15: Infrastructure Layer 분리 작업

**작업 내용:**
- OAuth 관련 코드를 `domain.auth.oidc` 패키지에서 `infra.oauth` 패키지로 이동
- 이동된 파일:
  - `OidcProviderStrategy.java`
  - `OidcProviderRegistry.java`
  - `OidcUserInfo.java`
  - `KakaoOidcProvider.java`
  - `KakaoTokenResponse.java`

**수동 작업 필요:**
다음 중복 파일들을 삭제해야 합니다:
```
src/main/java/com/cover/time2gather/domain/auth/oidc/KakaoOidcProvider.java
src/main/java/com/cover/time2gather/domain/auth/oidc/KakaoTokenResponse.java
src/main/java/com/cover/time2gather/domain/auth/oidc/OidcProviderRegistry.java
src/main/java/com/cover/time2gather/domain/auth/oidc/OidcProviderStrategy.java
src/main/java/com/cover/time2gather/domain/auth/oidc/OidcUserInfo.java
```

테스트 파일들도 패키지 경로를 수정해야 합니다:
```
src/test/java/com/cover/time2gather/domain/auth/oidc/KakaoOidcProviderTest.java
→ src/test/java/com/cover/time2gather/infra/oauth/KakaoOidcProviderTest.java

src/test/java/com/cover/time2gather/domain/auth/oidc/OidcProviderRegistryTest.java
→ src/test/java/com/cover/time2gather/infra/oauth/OidcProviderRegistryTest.java

src/test/java/com/cover/time2gather/domain/auth/oidc/OidcProviderStrategyTest.java
→ src/test/java/com/cover/time2gather/infra/oauth/OidcProviderStrategyTest.java
```

**변경 사항:**
- `OAuthLoginService`에서 `infra.oauth` 패키지의 클래스들을 import하도록 수정
- Kakao 프로필 이미지 URL을 사용자 정보 API에서 정확히 추출하도록 개선
- `getUserInfo()` 메서드 추가하여 ID Token과 사용자 정보를 함께 반환

7. Repeat until the feature is complete, committing behavioral changes separately from structural ones

Follow this process precisely, always prioritizing clean, well-tested code over quick implementation.

Always write one test at a time, make it run, then improve structure. Always run all the tests (except long-running tests) each time.

# Rust-specific

Prefer functional programming style over imperative style in Rust. Use Option and Result combinators (map, and_then, unwrap_or, etc.) instead of pattern matching with if let or match when possible.

# JPA Guide

Do not use relationship like @oneToMany. We have to use only unique identifier as a reference key

# LAYERED ARCHITECTURE PRINCIPLES

## Layer Responsibilities

### Controller Layer (Presentation)
- **Role**: HTTP 요청/응답 처리
- **Responsibilities**:
  - HTTP 요청 수신 및 검증
  - DTO ↔ 도메인 모델 변환
  - Service 계층 호출
  - HTTP 응답 생성 (성공/실패)
- **Rules**:
  - ❌ 비즈니스 로직 금지
  - ❌ 도메인 모델 직접 조작 금지
  - ❌ Repository 직접 호출 금지
  - ✅ Service 호출만 허용
  - ✅ DTO만 사용

### Service Layer (Business Logic)
- **Role**: 비즈니스 로직 처리
- **Responsibilities**:
  - 핵심 비즈니스 규칙 구현
  - 트랜잭션 관리
  - 도메인 모델 조작
  - Repository 계층 호출
  - 도메인 이벤트 발생
- **Rules**:
  - ❌ DTO 의존성 금지 (presentation DTO 사용 불가)
  - ❌ HTTP 관련 코드 금지
  - ✅ 도메인 모델만 사용
  - ✅ Repository만 호출
  - ✅ 순수한 Java/Kotlin 객체만 반환

### Repository Layer (Data Access)
- **Role**: 데이터 영속성 관리
- **Responsibilities**:
  - 엔티티 CRUD
  - 쿼리 실행
  - 데이터베이스 접근
- **Rules**:
  - ✅ 엔티티만 반환
  - ❌ 비즈니스 로직 금지

## 정적 리소스 및 에러 처리 가이드

### NoResourceFoundException 처리
- Spring MVC에서 존재하지 않는 경로에 대한 요청 시 발생
- GlobalExceptionHandler에서 INFO 레벨로 로깅하여 에러 로그 오염 방지
- 404 NOT_FOUND 응답으로 처리

### 정적 리소스 설정
- `/static/**`: 정적 리소스 경로
- `/favicon.ico`: 파비콘
- `/error`: 에러 페이지
- 위 경로들은 Spring Security에서 permit all 처리

### 로깅 레벨 설정
- `com.cover.time2gather`: INFO
- `org.springframework.web.servlet.resource`: WARN (정적 리소스 관련 로그 최소화)
- `org.springframework.security`: WARN (보안 관련 로그 최소화)

3. **변환은 경계에서만**
   - Controller: DTO ↔ Domain
   - Repository: Domain ↔ Entity

4. **의존성 방향**
   - Controller → Service → Repository
   - 역방향 의존성 절대 금지

# TEST PLAN

## Phase 1: OAuth2/OIDC Authentication (Kakao + Extensible)

### 1. OIDC Provider Strategy
- [x] OidcProviderStrategy 인터페이스 정의 테스트
- [x] KakaoOidcProvider 구현 테스트 (Authorization Code → ID Token)
- [x] Provider 등록/조회 테스트

### 2. JWT Token Service
- [x] JWT 생성 테스트 (userId, username 기반)
- [x] JWT 검증 테스트
- [x] JWT 파싱 테스트 (claims 추출)

### 3. OAuth2 Login Flow
- [x] Authorization Code 수신 엔드포인트 테스트
- [x] Kakao ID Token 획득 테스트
- [x] User 생성/조회 (upsert) 테스트
- [x] JWT 발급 및 쿠키 설정 테스트

### 4. Spring Security Integration
- [x] JWT Cookie Filter 테스트
- [x] Authentication 생성 테스트
- [x] Public/Protected 엔드포인트 접근 제어 테스트 (통합 테스트)

### 5. Integration Tests
- [x] 전체 OAuth 로그인 플로우 통합 테스트
- [x] 신규 사용자 생성 테스트
- [x] 기존 사용자 로그인 테스트
- [x] JWT 쿠키 설정 검증

## ✅ Phase 1 완료!

카카오 OAuth2/OIDC 로그인이 전략 패턴 기반으로 완전히 구현되었습니다.
- 총 10개 테스트 파일, 30+ 테스트 케이스
- TDD 원칙 준수 (Red-Green-Refactor)
- 확장 가능한 아키텍처 (Google, Naver 등 추가 가능)

---

## ✅ Phase 2 완료! Anonymous Login (Meeting-Scoped)

**전략:** Meeting 내에서만 유니크한 익명 사용자
- `providerId = meeting_code + ":" + username`
- 상세 설계는 `ANONYMOUS-LOGIN-STRATEGY.md` 참고

### 1. Password Encryption Service
- [x] BCryptPasswordEncoder 설정
- [x] Password 해싱 테스트
- [x] Password 검증 테스트

### 2. Anonymous Login Service
- [x] Meeting 기반 providerId 생성 테스트
- [x] 신규 익명 유저 생성 테스트 (password 해싱)
- [x] 기존 익명 유저 로그인 테스트 (password 검증)
- [x] 잘못된 password 처리 테스트
- [x] JWT 발급 테스트

### 3. Meeting Auth Controller
- [x] Anonymous 로그인 엔드포인트 테스트
- [x] 쿠키 설정 테스트
- [x] 401 Unauthorized 에러 테스트
- [x] 400 Bad Request 에러 테스트

### 4. Integration Tests
- [x] Meeting 컨텍스트 내 익명 로그인 전체 플로우
- [x] 같은 모임 내 username 중복 처리
- [x] 다른 모임에서 같은 username 사용 가능 검증
- [x] Password 불일치 케이스

**구현 완료!**
- 총 3개 테스트 파일, 15+ 테스트 케이스
- Meeting 스코프 기반 익명 로그인 완성
- BCrypt 암호화 적용
- 동일한 JWT 인증 플로우 사용
- POST /api/v1/meetings/{meetingCode}/auth/anonymous 엔드포인트

---

## ✅ Phase 2.1 완료! OAuth 로그인 profileImageUrl 및 중복 필드 수정

### 문제
1. 카카오 로그인 시 profileImageUrl이 응답에 포함되지 않음
2. OAuthLoginResponse에서 `newUser`와 `isNewUser` 필드가 중복으로 출력됨

### 해결
- [x] OAuthLoginService에 ID Token에서 `picture` 또는 `profile_image` 필드 파싱 로직 추가
- [x] User 엔티티에 `updateProfileImageUrl()` 메서드 추가
- [x] 기존 사용자 로그인 시 profileImageUrl 업데이트 로직 추가
- [x] OAuthLoginResponse에서 수동 getter 제거, @Getter 어노테이션으로 통일
- [x] parseIdToken() 메서드 추가하여 ID Token payload 파싱

**변경된 파일:**
- `OAuthLoginService.java`: ID Token 파싱 및 profileImageUrl 처리
- `User.java`: updateProfileImageUrl() 메서드 추가
- `OAuthLoginResponse.java`: @Getter 사용으로 중복 필드 제거

---

## Phase 3: 모임(Meeting) 관리

### 목표
일정 조율을 위한 Meeting(모임) 생성 및 조회 기능 구현. 30분 단위 시간 슬롯 선택 기능.

### spec.md 기반 설계
- **Meeting**: 일정 조율의 기본 단위 (meeting_code로 공유)
- **MeetingUserSelection**: 유저별 시간 선택
- **시간 표현**: 
  - 내부: slotIndex (0~47, 30분 단위)
  - API: "HH:mm" 형식 (09:00, 09:30 등)

### Phase 3 테스트 계획 (TDD)

**주의사항:**
- ❌ JPA Relationship 사용 금지 (@OneToMany, @ManyToOne 등)
- ✅ ID만 참조 (Long hostUserId, Long meetingId 등)
- ✅ TDD: Red → Green → Refactor 엄격히 준수

#### 3.1 JwtAuthentication 클래스 생성
- [ ] JwtAuthentication record 생성 (userId, username)
- [ ] SecurityContext에서 사용할 Principal 구현

#### 3.2 TimeSlotConverter 유틸리티
- [ ] slotIndex → "HH:mm" 변환 테스트 (0→"00:00", 1→"00:30", 47→"23:30")
- [ ] "HH:mm" → slotIndex 변환 테스트
- [ ] 유효하지 않은 입력 예외 처리 테스트

#### 3.3 Meeting 엔티티 (ID 참조만 사용)
- [ ] Meeting 엔티티 생성 테스트 (hostUserId Long만 저장)
- [ ] meeting_code 생성 테스트 (8자리 랜덤 문자열)
- [ ] available_dates JSON 저장 테스트

#### 3.4 MeetingService
- [ ] createMeeting 테스트 (hostUserId로 생성)
- [ ] getMeetingByCode 테스트
- [ ] meeting_code 중복 시 재생성 테스트
- [ ] 존재하지 않는 모임 조회 예외 테스트

#### 3.5 MeetingUserSelection 엔티티 (ID 참조)
- [ ] MeetingUserSelection 생성 테스트 (meetingId, userId Long 사용)
- [ ] selections JSON 저장 테스트

#### 3.6 MeetingSelectionService
- [ ] upsertUserSelections 테스트 (신규/수정)
- [ ] getUserSelections 테스트
- [ ] getAllSelections 테스트 (모임 전체 선택)
- [ ] 유효하지 않은 시간 선택 예외 테스트

#### 3.7 Meeting API - DTO 설계
- [ ] CreateMeetingRequest DTO 검증 테스트
- [ ] CreateMeetingResponse DTO 테스트
- [ ] MeetingDetailResponse 구성 테스트

#### 3.8 Meeting API - 통합 테스트
- [ ] POST /api/v1/meetings - 모임 생성 (JWT 인증)
- [ ] GET /api/v1/meetings/{meetingCode} - 상세 조회 (Public)
- [ ] GET /api/v1/meetings/{meetingCode}/selections - 내 선택 조회
- [ ] PUT /api/v1/meetings/{meetingCode}/selections - 시간 선택/수정

### 현재 진행 상태
- [x] Phase 3 완료!
- [x] JPA 연관관계 제거 완료 (ID 참조로 변경)
  - Meeting: @ManyToOne User → Long hostUserId
  - MeetingUserSelection: @ManyToOne Meeting/User → Long meetingId/userId
  - Repository 메서드 시그니처 변경
  - Service 레이어 수정 (엔티티 조회 최소화)
  - Controller에서 필요시 별도 조회

### ✅ Phase 3 완료!

**Meeting(모임) 관리 기능 구현 완료**
- Meeting 생성/조회 API
- 사용자별 시간 선택 관리
- 30분 단위 시간 슬롯 시스템 (slotIndex ↔ "HH:mm")
- 모임 상세 정보 및 참여자 통계
- **모든 엔티티가 ID 참조만 사용** (JPA 관계 제거)

구현된 API:
- POST /api/v1/meetings - 모임 생성
- GET /api/v1/meetings/{meetingCode} - 상세 조회
- GET /api/v1/meetings/{meetingCode}/selections - 내 선택 조회
- PUT /api/v1/meetings/{meetingCode}/selections - 시간 선택/수정

---

## ✅ Phase 4 완료! Calendar Export (ICS)

**기능:** 선택한 일정을 Google Calendar, iOS Calendar로 export
- iCalendar (ICS) 표준 포맷 지원
- 날짜/시간 선택 → .ics 파일 다운로드

### 구현 내역

#### 1. Calendar Export Service
- [x] ical4j 라이브러리 통합 (build.gradle)
- [x] CalendarExportService 구현
  - ICS 파일 생성 (VEvent)
  - 타임존 지원 (ZonedDateTime)
  - 30분 단위 시간 슬롯 변환
  - UID, DTSTAMP 등 필수 iCalendar 속성 자동 생성

#### 2. Export API
- [x] POST /api/v1/meetings/{meetingCode}/export
- [x] ExportCalendarRequest DTO (date, time)
- [x] ICS 파일 다운로드 응답
  - Content-Type: text/calendar
  - Content-Disposition: attachment
  - 파일명: meeting_YYYY-MM-DD_HHmm.ics

#### 3. Integration
- [x] Meeting 정보 활용 (제목, 설명, 타임존)
- [x] TimeSlotConverter와 통합 (HH:mm → slotIndex → 시작/종료 시간)
- [x] ResponseEntity<byte[]>로 파일 응답

### 구현 완료!
- **Google Calendar, iOS Calendar, Outlook 등 모든 표준 캘린더 앱 호환**
- iCalendar RFC 5545 표준 준수
- 사용자가 원하는 시간대를 캘린더 앱에 직접 추가 가능
- 30분 단위 이벤트 생성

구현된 API:
- POST /api/v1/meetings/{meetingCode}/export - 캘린더 export

---


