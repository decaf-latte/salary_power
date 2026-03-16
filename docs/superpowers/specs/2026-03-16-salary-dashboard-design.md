# Salary Dashboard - 개인 연봉 대시보드 설계서

## 개요

매월 급여를 기록하고, 연봉을 산출하며, 연도별 연봉 상승률을 시각적으로 확인할 수 있는 개인용 웹 대시보드.

### 목표
- 월급 데이터를 간편하게 입력/관리
- 연봉 자동 산출 (합계 기반)
- 연도별 연봉 상승률 추적
- 토스 스타일의 직관적인 UI

### 범위
- **1단계 (현재):** 개인용, 수동 입력, 핵심 대시보드
- **2단계 (향후):** 급여명세서 업로드/파싱 (OCR 또는 AI)
- **3단계 (향후):** 멀티유저 서비스 (회원가입/로그인)

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot (Java) |
| Template | Thymeleaf (SSR) |
| Interactivity | HTMX |
| Chart | Chart.js (CDN) |
| DB | MySQL |
| ORM | Spring Data JPA |
| Build | Gradle |

---

## 데이터 모델

### SalaryRecord (월급 기록)

| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 자동 생성 |
| yearMonth | YearMonth (UNIQUE) | 연월 (2026-03), `java.time.YearMonth` 사용, 중복 불가 |
| year | Integer (INDEX) | 연도 (연도별 조회 성능 최적화용, yearMonth에서 파생) |
| baseSalary | Long | 기본급 |
| bonus | Long | 성과급 (분기/반기/연 단위 보너스, 없는 달은 0) |
| extraIncome | Long | 영끌 (연장근무수당, 휴일근무수당, 야간근무수당 등 기본급 외 지급내역 합계) |
| grossTotal | Long | 세전 총액 (baseSalary + bonus + extraIncome 자동 합산) |
| totalDeduction | Long | 총 공제액 |
| netSalary | Long | 세후 실수령액 (grossTotal - totalDeduction 자동 계산, 수동 수정 가능) |
| memo | String (nullable) | 메모 |
| createdAt | LocalDateTime | 생성일시 |
| updatedAt | LocalDateTime | 수정일시 |

> **통화:** KRW (원) 기준, 소수점 없음

### AnnualSummary (연도별 요약)

별도 테이블 없이 SalaryRecord에서 쿼리로 산출.

| 항목 | 산출 방식 |
|------|----------|
| 연봉 | 해당 연도 grossTotal 합계 |
| 예상 연봉 | 합계 / 입력월수 x 12 (진행 중인 해) |
| 월평균 실수령액 | netSalary 합계 / 입력월수 |
| 전년 대비 상승률 | (올해 연봉 - 작년 연봉) / 작년 연봉 x 100 |
| 연간 실수령 합계 | netSalary 합계 |
| 연간 기본급 합계 | baseSalary 합계 |
| 연간 성과급 합계 | bonus 합계 |
| 연간 영끌 합계 | extraIncome 합계 |
| 누적 상승률 | (현재 연봉 - 최초 연도 연봉) / 최초 연도 연봉 x 100 |

---

## 화면 구성

### 1. 메인 대시보드 (`/`)
- **현재 연봉** 카드 (현재 캘린더 연도 기준, 데이터 없으면 가장 최근 연도 사용. 진행 중이면 예상 연봉 함께 표시)
- **전년 대비 상승률** 카드 (+5.2% 등)
- **누적 총 수입** 카드 (전체 기간 netSalary 합계)
- **빈 상태:** 데이터 없을 시 "첫 급여를 입력해보세요" 안내 + 입력 페이지 링크
- **연도별 연봉 추이** 꺾은선 그래프 (Chart.js)
- **월별 실수령액 추이** 막대 그래프
- **세전 vs 세후 비교** 그래프
- **수입 구성 비중** 도넛 차트 (기본급 / 성과급 / 영끌 비율)

### 2. 급여 입력/수정 (`/salary/new`, `/salary/edit/{id}`)
- 연월 선택 (date picker)
- 기본급 / 성과급 / 영끌(기타수당) 입력
- 세전 총액 자동 합산 (기본급 + 성과급 + 영끌)
- 총 공제액 입력
- 실수령액 자동 계산 (세전 - 공제, 수동 수정 가능)
- 메모 (선택)
- 저장 버튼
- 수정 시 동일 폼 재사용, 기존 데이터 프리필
- 검증 실패 시 인라인 에러 메시지 표시

### 3. 급여 목록 (`/salary/list`)
- 월별 기록 테이블 (연월, 세전, 세후)
- 수정/삭제 기능 (삭제 시 확인 모달)
- 연도별 필터

### 4. 연도별 비교 (`/annual`)
- 연도별 연봉, 전년 대비 상승률, 누적 상승률 표
  - 예: 2023 → 4,200만 / — / —
  - 예: 2024 → 4,500만 / +7.1% / +7.1%
  - 예: 2025 → 4,800만 / +6.7% / +14.3%
  - 예: 2026 → 5,200만 / +8.3% / +23.8%
- 연봉 상승률 추이 그래프
- 수입 구성 변화 (연도별 기본급/성과급/영끌 비중 누적 막대 그래프)

HTMX로 페이지 전환 시 부드러운 전환 처리.

### HTMX + Chart.js 연동
- HTMX로 페이지 전환 후 `htmx:afterSwap` 이벤트에서 Chart.js 인스턴스를 초기화/재생성
- 차트 초기화 로직을 `chart-config.js`에 함수로 분리하여 재호출 가능하게 구성

---

## 디자인 시스템

### 토스 스타일 원칙
- 깔끔한 카드 UI
- 넉넉한 여백
- 큰 숫자 강조
- 미니멀한 디자인

### 컬러 시스템

| 용도 | 색상 | 코드 |
|------|------|------|
| 배경/헤더/사이드바 | 네이비 | #1B2838 |
| 포인트/버튼/강조 | 민트 | #4ECDC4 |
| 카드 배경/본문 | 화이트 | #FFFFFF |
| 상승 표시 | 민트 | #4ECDC4 |
| 하락 표시 | 코랄 | #FF6B6B |

---

## 프로젝트 구조

```
salary-dashboard/
├── src/main/java/.../
│   ├── controller/
│   │   ├── DashboardController    # 메인 대시보드
│   │   └── SalaryController       # 급여 CRUD
│   ├── domain/
│   │   └── SalaryRecord           # 엔티티
│   ├── repository/
│   │   └── SalaryRecordRepository # JPA Repository
│   ├── service/
│   │   └── SalaryService          # 비즈니스 로직, 연봉 산출/상승률 계산
│   └── dto/
│       ├── SalaryForm             # 입력 폼
│       └── AnnualSummaryDto       # 연도별 요약
├── src/main/resources/
│   ├── templates/
│   │   ├── layout.html            # 공통 레이아웃
│   │   ├── dashboard.html         # 메인 대시보드
│   │   ├── salary/form.html       # 입력 폼
│   │   ├── salary/list.html       # 목록
│   │   └── annual.html            # 연도별 비교
│   ├── static/
│   │   ├── css/style.css          # 토스 스타일 CSS
│   │   └── js/chart-config.js     # Chart.js 설정
│   └── application.yml            # DB 설정 등
└── build.gradle
```

---

## 비즈니스 로직

### 연봉 산출
- 해당 연도 grossTotal 합계 = 연봉
- 진행 중인 해: 합계 / 입력월수 x 12 = 예상 연봉

### 상승률 계산
- 전년 대비: (올해 연봉 - 작년 연봉) / 작년 연봉 x 100
- 누적 상승률: (현재 연봉 - 최초 기록 연도 연봉) / 최초 기록 연도 연봉 x 100
- 작년 데이터 없으면 "N/A" 표시
- 최초 연도는 누적 상승률 "—" 표시

### 예상 연봉 주의사항
- 입력 월수가 3개월 미만일 경우 예상 연봉 옆에 "데이터 부족" 표시
- 성과급 등 비정기 수당이 포함된 달이 있으면 예상치가 과대/과소 산출될 수 있음을 안내

### 입력 기준
- **지급내역만 세분화** (기본급, 성과급, 영끌)
- **공제내역은 합계만 입력** (세부 공제 항목은 다루지 않음)
- 연말정산 환급 등으로 공제합계가 음수가 될 수 있음 → **totalDeduction 음수 허용**
- 차인지급액이 지급합계보다 클 수 있음 (환급 시)

### 데이터 검증
- yearMonth 중복 입력 방지 (DB UNIQUE 제약 + 서비스 레벨 검증)
- baseSalary, bonus, extraIncome은 0 이상
- totalDeduction은 음수 허용 (연말정산 환급 등)
- yearMonth 필수
- 검증 실패 시 폼에 인라인 에러 메시지 표시

---

## 향후 확장 고려사항

- **멀티유저:** User 엔티티 추가, Spring Security 인증, SalaryRecord에 userId FK 추가
- **급여명세서 파싱:** 파일 업로드 API + OCR/AI 파싱 서비스 추가
- **자영업자 지원:** 매출/수익 기반 별도 모델 추가 가능
