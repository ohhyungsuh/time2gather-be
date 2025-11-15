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

7. Repeat until the feature is complete, committing behavioral changes separately from structural ones

Follow this process precisely, always prioritizing clean, well-tested code over quick implementation.

Always write one test at a time, make it run, then improve structure. Always run all the tests (except long-running tests) each time.

# Rust-specific

Prefer functional programming style over imperative style in Rust. Use Option and Result combinators (map, and_then, unwrap_or, etc.) instead of pattern matching with if let or match when possible.

# JPA Guide

Do not use relationship like @oneToMany. We have to use only unique identifier as a reference key

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
- [ ] Phase 3 시작 전 - 기존 구현 코드 삭제 완료, TDD로 재시작

---

---

## 일정(Schedule) 기능 추가 (Deprecated - Meeting으로 대체됨)
- 엔티티 `Schedule` 생성 (모임 코드, 제목, 설명, 시작/종료 시각, 감사 필드)
- 레포지토리 `ScheduleRepository` 및 기간 필터 조회 메서드 추가
- 서비스 `ScheduleService` 일정 생성/조회 로직 (endAt 검증 포함)
- DTO `ScheduleCreateRequest`, `ScheduleResponse` 생성
- 컨트롤러 `ScheduleController` 작성 (생성, 목록 조회, 단순 검증 & 예외 처리)
- Swagger 어노테이션 적용 (Tag, Operation, Schema)
- 추후 계획: 일정 수정/삭제, 겹치는 일정 검증, 타임존 표준화(UTC 저장, KST 변환) 필요 여부 검토
