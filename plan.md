# 캘린더 Export 기능 구현 계획

## 개요
사용자가 투표 결과 중 원하는 날짜/시간을 선택하여 본인 캘린더로 export하는 기능

## 기술 결정사항
- **구현 방식**: BE API 수정 + FE 연동
- **표시 범위**: 모든 bestSlots 표시
- **타입 지원**: TIME + ALL_DAY 둘 다 지원
- **TDD**: 적용 (Red-Green-Refactor)

---

## Backend (time2gather-be)

### Phase 1: Export API Query Parameter 지원

#### 1.1 API 스펙
```
GET /api/v1/meetings/{meetingCode}/export
GET /api/v1/meetings/{meetingCode}/export?date=2024-02-15&slotIndex=9
GET /api/v1/meetings/{meetingCode}/export?date=2024-02-15&slotIndex=-1  # ALL_DAY
```

#### 1.2 테스트 케이스

- [x] 파라미터 없이 호출 시 기존 동작 유지 (bestSlot 첫번째 선택)
- [x] date + slotIndex 파라미터로 특정 시간대 ICS 생성
- [x] ALL_DAY 케이스 (slotIndex = -1) 정상 처리
- [x] 잘못된 date 형식 → 400 Bad Request
- [x] Meeting에 존재하지 않는 날짜 → 400 Bad Request
- [x] date만 있고 slotIndex가 없는 경우 → 400 Bad Request

---

## Frontend (time2gather-fe)

### Phase 2: API Service 추가

- [x] meetings.ts에 getExportUrl 함수 추가
- [x] meetings.ts에 timeToSlotIndex 함수 추가

### Phase 3: CalendarExportDialog 컴포넌트 생성

- [x] Radix UI Dialog 기반 컴포넌트 생성
- [x] bestSlots를 라디오 버튼으로 표시
- [x] TIME 타입: 날짜 + 시간 표시
- [x] ALL_DAY 타입: 날짜 + "종일" 표시
- [x] 선택 후 ICS 다운로드 트리거

### Phase 4: ResultButtons 수정

- [x] "내 캘린더로 옮기기" 버튼 추가
- [x] Dialog 상태 관리 및 연동

### Phase 5: i18n 번역 추가

- [x] ko.json 번역 추가
- [x] en.json 번역 추가

### Phase 6: 빌드 검증

- [x] FE TypeScript 빌드 성공 (npm run build)

---

## 완료 조건

- [x] BE: 모든 테스트 케이스 통과
- [x] FE: 결과 페이지에서 캘린더 export 버튼 동작
- [ ] iOS Safari에서 ICS 파일 다운로드 및 캘린더 앱 연동 확인
- [ ] Android Chrome에서 ICS 파일 다운로드 및 캘린더 앱 연동 확인
