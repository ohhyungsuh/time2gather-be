일정 조율 서비스 개발 문서 (v2, 30분 단위 & HH:mm API)
1. 서비스 개요
   1.1. 목적

여러 사람이 각자 가능한 시간을 입력하면, 공통으로 가능한 시간대를 한눈에 볼 수 있는 일정 조율 서비스.

When2meet / Lettuce 같은 서비스 느낌

글로벌 서비스 확장 가능 (타임존 기반)

모임 정보는 항상 퍼블릭

링크만 알고 있으면 누구나 모임 페이지 조회 가능

일정 선택은 로그인 필요

1.2. 핵심 기능 요약

방장:

모임 생성

가능한 날짜/시간대(30분 단위) 지정

참여자:

로그인(익명 / Kakao / Google)

각 날짜별 30분 단위 시간대 선택

모임 상세 페이지:

날짜/시간별로 누가 가능한지 확인

가장 많은 사람이 가능한 “베스트 슬롯” 계산

2차 스펙:

Redis 기반으로 “특정 날짜/시간에 가능한 유저 목록”을 빠르게 조회

2. 도메인 개념
   2.1. User

서비스의 계정 주체

로그인 방식:

익명(아이디 + 비밀번호)

OAuth (Kakao / Google)

가진 속성:

username, email, profileImageUrl

provider: anonymous / kakao / google

provider_id: OAuth provider의 user id

2.2. Meeting (모임)

방장이 생성하는 일정 조율 단위

특징:

meeting_code: 공유용 고유 코드 (해시/난수)

title, description

host_user_id

timezone (예: "Asia/Seoul")

available_dates: 날짜별 가능한 시간대 (30분 단위, DB에는 slotIndex, API에는 HH:mm)

2.3. Selection (유저의 시간 선택)

Meeting x User 조합마다 한 개의 selections JSON

구조:

날짜별로 유저가 선택한 시간 리스트

DB: slotIndex 배열

API: "HH:mm" 문자열 배열

3. 시간 표현 방식
   3.1. 내부 표현: slotIndex (0 ~ 47)

하루 24시간을 30분 단위 48칸으로 나누고 각 칸에 인덱스를 부여:

slotIndex	시작 시각	구간
0	00:00	00:00~00:30
1	00:30	00:30~01:00
2	01:00	01:00~01:30
…	…	…
18	09:00	09:00~09:30
19	09:30	09:30~10:00
…	…	…
47	23:30	23:30~24:00

계산식:

slotIndex = hour * 2 + (minute == 30 ? 1 : 0)

총 분 = slotIndex * 30

hour = 총 분 / 60, minute = 총 분 % 60

3.2. API 표현: "HH:mm" 문자열

외부(API)에서는 항상 "HH:mm" 형식 사용

예) "09:00", "09:30", "23:30"

minute은 00 또는 30만 허용

API 요청/응답은 날짜 + 시간 문자열 기준
→ 컨트롤러에서만 "HH:mm" ↔ slotIndex 변환

3.3. 변환 헬퍼 예시 (Kotlin)
fun timeStrToSlotIndex(time: String): Int {
// "HH:mm"
val (hh, mm) = time.split(":").map { it.toInt() }
require(hh in 0..23) { "hour must be 0..23" }
require(mm == 0 || mm == 30) { "minute must be 0 or 30" }
return hh * 2 + if (mm == 30) 1 else 0
}

fun slotIndexToTimeStr(slot: Int): String {
require(slot in 0..47) { "slotIndex must be 0..47" }
val totalMinutes = slot * 30
val hh = totalMinutes / 60
val mm = totalMinutes % 60
return "%02d:%02d".format(hh, mm)
}

4. DB 스키마 (MySQL 8 기준)
   4.1. users
   CREATE TABLE users (
   id BIGINT AUTO_INCREMENT PRIMARY KEY,
   username VARCHAR(100) UNIQUE NOT NULL,
   password VARCHAR(255), -- 익명 로그인용 (bcrypt 해시)
   email VARCHAR(255),
   profile_image_url VARCHAR(500),
   provider ENUM('anonymous', 'kakao', 'google') NOT NULL DEFAULT 'anonymous',
   provider_id VARCHAR(255),
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
   INDEX idx_provider (provider, provider_id)
   );

4.2. meetings
CREATE TABLE meetings (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
meeting_code VARCHAR(100) UNIQUE NOT NULL, -- 공유용 코드
title VARCHAR(255) NOT NULL,
description TEXT,
host_user_id BIGINT NOT NULL,
timezone VARCHAR(50) DEFAULT 'Asia/Seoul',
available_dates JSON NOT NULL,
/* 저장 예시 (slotIndex 기준):
{
"2024-02-15": [18, 19, 20, 21],
"2024-02-16": [22, 23, 24]
}
*/
is_active BOOLEAN DEFAULT TRUE,
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
FOREIGN KEY (host_user_id) REFERENCES users(id),
INDEX idx_meeting_code (meeting_code)
);


주의:
API는 "09:00" 같은 문자열을 주고받지만,
available_dates에는 정수 slotIndex 배열로 변환되어 저장된다.

4.3. meeting_user_selections
CREATE TABLE meeting_user_selections (
id BIGINT AUTO_INCREMENT PRIMARY KEY,
meeting_id BIGINT NOT NULL,
user_id BIGINT NOT NULL,
selections JSON NOT NULL,
/* 저장 예시 (slotIndex 기준):
{
"2024-02-15": [18, 19, 21],
"2024-02-16": [22, 23]
}
*/
created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
FOREIGN KEY (meeting_id) REFERENCES meetings(id) ON DELETE CASCADE,
FOREIGN KEY (user_id) REFERENCES users(id),
UNIQUE KEY unique_user_meeting (meeting_id, user_id),
INDEX idx_meeting_id (meeting_id)
);

5. 인증 & OAuth 설계
   5.1. 토큰 전략

Access Token: JWT, 짧은 만료 (예: 1시간)

Refresh Token: JWT 또는 랜덤 토큰, 긴 만료 (예: 30일)

클라이언트:

Authorization: Bearer <accessToken> 헤더 사용

5.2. 익명 로그인

username + password 기반

미존재 → 계정 생성 후 로그인

존재 → password 검증 후 로그인

Endpoint

POST /api/v1/auth/anonymous

5.3. OAuth 로그인 (Authorization Code Flow, Code만 전달)

플로우:

프론트가 Kakao/Google authorize URL로 redirect

OAuth provider가 프론트의 redirectUri로 code 전달

프론트가 authorizationCode만 백엔드로 전송

Endpoint

POST /api/v1/auth/oauth/{provider}

{provider}: kakao 또는 google

Request Body:

{
"authorizationCode": "CODE_FROM_PROVIDER"
}


백엔드 동작:

authorizationCode를 provider 토큰 엔드포인트에 전달

provider access token & user info 조회

(provider, provider_id) 기준으로 users upsert

서비스 자체 accessToken / refreshToken 발급 후 응답

5.4. 토큰 재발급

Endpoint

POST /api/v1/auth/refresh

Request:

{
"refreshToken": "JWT_REFRESH"
}


Response:

{
"success": true,
"data": {
"accessToken": "NEW_JWT_ACCESS",
"refreshToken": "NEW_JWT_REFRESH"
}
}

6. API 설계

Base URL: /api/v1

6.1. Auth
6.1.1. 익명 로그인

POST /auth/anonymous

Request:

{
"username": "user123",
"password": "password123"
}


Response 200:

{
"success": true,
"data": {
"userId": 1,
"username": "user123",
"profileImageUrl": null,
"accessToken": "JWT_ACCESS",
"refreshToken": "JWT_REFRESH"
}
}

6.1.2. OAuth 로그인

POST /auth/oauth/{provider}

Request:

{
"authorizationCode": "CODE_FROM_PROVIDER"
}


Response 200:

{
"success": true,
"data": {
"userId": 2,
"username": "kakao_12345",
"email": "user@kakao.com",
"profileImageUrl": "https://k.kakaocdn.net/profile.jpg",
"provider": "kakao",
"accessToken": "JWT_ACCESS",
"refreshToken": "JWT_REFRESH",
"isNewUser": false
}
}

6.1.3. 토큰 재발급

POST /auth/refresh

Request:

{
"refreshToken": "JWT_REFRESH"
}

6.2. Meetings
6.2.1. 모임 생성

POST /meetings

인증 필요 (방장)

Request:

{
"title": "프로젝트 킥오프 미팅",
"description": "2월 신규 프로젝트 시작 회의",
"timezone": "Asia/Seoul",
"availableDates": {
"2024-02-15": ["09:00", "09:30", "10:00", "10:30"],
"2024-02-16": ["11:00", "11:30", "12:00"]
}
}


서버:

"09:00" → slotIndex 18 등으로 변환

available_dates JSON에 slotIndex 배열로 저장

meeting_code 생성 (mtg_ + 난수/해시)

Response 201:

{
"success": true,
"data": {
"meetingId": 1,
"meetingCode": "mtg_a3f8k2md9x",
"shareUrl": "https://when2meet.com/mtg_a3f8k2md9x"
}
}

6.2.2. 모임 상세 조회 (Public)

GET /meetings/{meetingCode}

인증 불필요 (모임은 항상 public)

Response 200 예시:

{
"success": true,
"data": {
"meeting": {
"id": 1,
"code": "mtg_a3f8k2md9x",
"title": "프로젝트 킥오프 미팅",
"description": "2월 신규 프로젝트 시작 회의",
"host": {
"id": 1,
"username": "jinwoo",
"profileImageUrl": "https://..."
},
"timezone": "Asia/Seoul",
"availableDates": {
"2024-02-15": ["09:00", "09:30", "10:00", "10:30"],
"2024-02-16": ["11:00", "11:30", "12:00"]
}
},
"participants": [
{
"userId": 1,
"username": "jinwoo",
"profileImageUrl": "https://..."
},
{
"userId": 2,
"username": "lee",
"profileImageUrl": null
}
],
"schedule": {
"2024-02-15": {
"09:00": [
{ "userId": 1, "username": "jinwoo", "profileImageUrl": "https://..." },
{ "userId": 2, "username": "lee", "profileImageUrl": null }
],
"09:30": [
{ "userId": 1, "username": "jinwoo", "profileImageUrl": "https://..." }
]
}
},
"summary": {
"totalParticipants": 2,
"bestSlots": [
{
"date": "2024-02-15",
"time": "09:00",
"count": 2,
"percentage": 100.0
}
]
}
}
}


schedule / summary는 1차 구현에서는 DB JSON을 파싱해서 계산,
2차에서는 Redis 기반으로 최적화 가능.

6.3. Selections (내 선택)
6.3.1. 내 선택 조회

GET /meetings/{meetingCode}/selections

인증 필요

Response 200:

{
"success": true,
"data": {
"selections": {
"2024-02-15": ["09:00", "09:30", "10:30"],
"2024-02-16": ["11:00", "11:30"]
}
}
}

6.3.2. 선택/수정 (Upsert)

PUT /meetings/{meetingCode}/selections

인증 필요

이 요청은 해당 모임에서 내 선택 전체를 교체하는 개념

Request:

{
"selections": {
"2024-02-15": ["09:00", "09:30", "10:30"],
"2024-02-16": ["11:00", "11:30"],
"2024-02-17": []
}
}


서버 처리:

meetingCode → meeting_id 조회

"09:00" 등 → slotIndex로 변환

각 날짜/slotIndex가 meetings.available_dates 내에 있는지 검증

meeting_user_selections에 Upsert (JSON 전체 교체)

(2차 스펙) Redis 데이터 동기화 (추가/삭제 슬롯 반영)

Response 200:

{
"success": true,
"message": "일정이 업데이트되었습니다."
}

6.4. Slots (2차 스펙, Redis 기반)
6.4.1. 특정 날짜/시간 참가자 조회

GET /meetings/{meetingCode}/slots?date=YYYY-MM-DD&time=HH:mm

인증은 선택:

모임이 public 이라서 public으로 둬도 되고

필요시 host만 호출하도록 바꿀 수도 있음

예:

GET /api/v1/meetings/mtg_a3f8k2md9x/slots?date=2024-02-15&time=09:00


서버:

"09:00" → slotIndex 18 변환

meetingCode → meeting_id

Redis meeting:{meetingId}:slot:2024-02-15:18에서 userId 목록 조회

users 테이블 join해서 username, profileImageUrl 가져오기

Response 200:

{
"success": true,
"data": {
"meetingCode": "mtg_a3f8k2md9x",
"date": "2024-02-15",
"time": "09:00",
"participants": [
{ "userId": 1, "username": "jinwoo", "profileImageUrl": "https://..." },
{ "userId": 2, "username": "lee", "profileImageUrl": null }
],
"participantCount": 2
}
}

7. 2차 스펙 – Redis 설계
   7.1. 목표

DB JSON 파싱 없이, date + time 기준으로 가능한 유저 목록을 O(1)에 가깝게 조회

7.2. Key 설계
meeting:{meetingId}:slot:{date}:{slotIndex}
예) meeting:1:slot:2024-02-15:18


Value 타입: Set<userId>

예:

key = meeting:1:slot:2024-02-15:18
SMEMBERS key = [1, 2, 5]

7.3. 선택 업데이트 시 Redis 동기화 알고리즘

DB에서 기존 selections 로드 (slotIndex 기준)

Map<String /*date*/, Set<Int> /*slotIndex*/>


새 selections (요청 바디 → time → slotIndex 변환)

날짜별로 removed / added 계산

의사 코드는:

val old = loadSelectionsFromDB(meetingId, userId)      // Map<date, Set<slot>>
val new = payloadSelections                            // Map<date, Set<slot>>

val allDates = (old.keys + new.keys).toSet()

for (date in allDates) {
val oldSlots = old[date] ?: emptySet()
val newSlots = new[date] ?: emptySet()

    val removed = oldSlots - newSlots
    val added   = newSlots - oldSlots

    for (slot in removed) {
        val key = "meeting:$meetingId:slot:$date:$slot"
        redis.srem(key, userId)
    }

    for (slot in added) {
        val key = "meeting:$meetingId:slot:$date:$slot"
        redis.sadd(key, userId)
    }
}

// 마지막에 DB에 new selections 저장
saveSelectionsToDB(meetingId, userId, new)

7.4. Redis 장애 시 폴백

Redis 조회 실패:

GET /slots 요청 시 DB에서 meeting_user_selections를 읽어와 on-the-fly로 집계

선택 업데이트 시 Redis 실패:

로그 남기고 DB만 우선 업데이트

나중에 batch로 Redis 재빌드용 스크립트 제공 가능

8. 타임존 처리 전략

meeting.timezone: "Asia/Seoul" 등 IANA 타임존 문자열

"2024-02-15" + "09:00"은 meeting.timezone 기준의 로컬 시간으로 해석

서버 내부:

slotIndex는 “해당 타임존의 하루” 기준

UTC로 변환해서 저장할 필요 X (이 서비스의 목적은 “서로 같은 캘린더 상에서 겹치는 시간” 찾기이기 때문)

나중에 고급 기능(타임존 다른 유저 간 자동 변환 UI)이 필요하면:

클라이언트에서 meeting.timezone ↔ 사용자 로컬 타임존 변환 처리

9. 구현 순서 제안

DB/엔티티/리포지토리 구현

Auth (익명 / OAuth / refresh)

Meeting

POST /meetings

GET /meetings/{meetingCode}

Selections

GET /meetings/{meetingCode}/selections

PUT /meetings/{meetingCode}/selections

1차 집계 로직 (DB JSON 파싱 → schedule & summary)

Redis 연동

selection 업데이트 시 Redis 동기화

GET /meetings/{meetingCode}/slots 구현