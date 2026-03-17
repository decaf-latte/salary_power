# 연봉 대시보드 (Salary Dashboard)

매월 급여를 기록하고, 연봉을 산출하며, 연도별 연봉 상승률을 시각적으로 확인할 수 있는 **개인용 웹 대시보드**입니다.

## 기획 의도

직장인으로서 매월 받는 급여명세서를 체계적으로 관리하고 싶었습니다.

- **내 연봉이 매년 얼마나 올랐는지** 한눈에 보고 싶다
- **기본급 / 성과급 / 기타 수당(기타수당)** 비중이 어떻게 변하는지 추적하고 싶다
- **세전 vs 세후** 차이를 시각적으로 비교하고 싶다
- 엑셀이 아닌 **깔끔한 대시보드**로 관리하고 싶다

토스 스타일의 직관적이고 미니멀한 UI로 설계했으며, 숫자가 한눈에 들어오는 카드 기반 레이아웃과 Chart.js 기반 시각화를 제공합니다.

## 주요 기능

### 메인 대시보드
- 현재 연봉, 전년 대비 상승률, 누적 총 수입 카드
- 연도별 연봉 추이 꺾은선 그래프
- 월별 실수령액 막대 그래프
- 세전 vs 세후 비교 그래프
- 수입 구성 비중 도넛 차트

### 급여 관리
- 월별 급여 입력/수정/삭제
- 기본급 + 성과급 + 기타 수당 → 세전 총액 자동 합산
- 세전 - 공제 → 실수령액 자동 계산 (수동 수정 가능)
- 연말정산 환급 등 공제액 음수 허용
- 연도별 필터링

### 연도별 비교
- 연도별 연봉, 실수령 합계, 기본급/성과급/기타 수당 합계 표
- 전년 대비 상승률 & 누적 상승률
- 연봉 상승률 추이 그래프
- 수입 구성 변화 누적 막대 그래프

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.4 (Java 17) |
| Template | Thymeleaf + Layout Dialect (SSR) |
| Interactivity | HTMX |
| Chart | Chart.js (CDN) |
| Database | MySQL (운영) / H2 (테스트) |
| ORM | Spring Data JPA |
| Test | Spock Framework (Groovy) |
| Build | Gradle |

## 실행 방법

### 1. MySQL 데이터베이스 생성

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS salary_dashboard DEFAULT CHARACTER SET utf8mb4;"
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

### 3. 브라우저 접속

```
http://localhost:8080
```

### 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `DB_PASSWORD` | root | MySQL 비밀번호 |

## 프로젝트 구조

```
salary-dashboard/
├── src/main/java/com/salary/dashboard/
│   ├── SalaryDashboardApplication.java
│   ├── config/WebConfig.java
│   ├── controller/
│   │   ├── DashboardController.java
│   │   └── SalaryController.java
│   ├── domain/
│   │   ├── SalaryRecord.java
│   │   └── YearMonthAttributeConverter.java
│   ├── dto/
│   │   ├── SalaryForm.java
│   │   └── AnnualSummaryDto.java
│   ├── repository/
│   │   └── SalaryRecordRepository.java
│   └── service/
│       └── SalaryService.java
├── src/main/resources/
│   ├── templates/
│   │   ├── layout.html
│   │   ├── dashboard.html
│   │   ├── salary/form.html
│   │   ├── salary/list.html
│   │   └── annual.html
│   ├── static/
│   │   ├── css/style.css
│   │   └── js/chart-config.js
│   └── application.yml
└── src/test/groovy/com/salary/dashboard/
    ├── domain/SalaryRecordSpec.groovy
    ├── repository/SalaryRecordRepositorySpec.groovy
    ├── service/SalaryServiceSpec.groovy
    └── controller/
        ├── SalaryControllerSpec.groovy
        └── DashboardControllerSpec.groovy
```

## ERD (Entity Relationship Diagram)

현재 1단계는 단일 테이블 구조이며, 향후 멀티유저 확장 시 User 테이블이 추가됩니다.

```
┌─────────────────────────────────────────────────┐
│                 salary_record                    │
├─────────────────────────────────────────────────┤
│ PK  id               BIGINT AUTO_INCREMENT       │
│     year_month        VARCHAR(7) UNIQUE NOT NULL  │
│ IDX salary_year       INT NOT NULL                │
│     base_salary       BIGINT NOT NULL             │
│     bonus             BIGINT NOT NULL             │
│     extra_income      BIGINT NOT NULL             │
│     gross_total       BIGINT NOT NULL (auto)      │
│     total_deduction   BIGINT NOT NULL             │
│     net_salary        BIGINT NOT NULL (auto)      │
│     memo              VARCHAR(255)                │
│     created_at        TIMESTAMP NOT NULL           │
│     updated_at        TIMESTAMP NOT NULL           │
├─────────────────────────────────────────────────┤
│ UNIQUE: year_month                               │
│ INDEX:  idx_year (salary_year)                   │
├─────────────────────────────────────────────────┤
│ Auto-calculated:                                 │
│   gross_total = base_salary + bonus + extra_income│
│   net_salary  = gross_total - total_deduction     │
│   salary_year = year_month.getYear()              │
└─────────────────────────────────────────────────┘

┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
│         annual_summary (가상 - 쿼리 산출)        │
├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
│  year, annual_gross, estimated_annual,           │
│  annual_net, annual_base_salary,                 │
│  annual_bonus, annual_extra_income,              │
│  avg_monthly_net, growth_rate,                   │
│  cumulative_growth_rate, month_count             │
└ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘
         ↑ SalaryRecord에서 GROUP BY year로 산출
```

### 향후 확장 ERD (멀티유저)

```
┌──────────────┐       ┌─────────────────────┐
│    user      │       │   salary_record      │
├──────────────┤       ├─────────────────────┤
│ PK id        │──1:N──│ FK user_id           │
│    email     │       │ PK id                │
│    password  │       │    year_month        │
│    name      │       │    ... (동일)        │
└──────────────┘       └─────────────────────┘
```

## 시퀀스 다이어그램

### 급여 입력 플로우

```
사용자          SalaryController       SalaryService        SalaryRecordRepository     DB
  │                   │                     │                        │                   │
  │  GET /salary/new  │                     │                        │                   │
  │──────────────────>│                     │                        │                   │
  │  form.html (빈 폼)│                     │                        │                   │
  │<──────────────────│                     │                        │                   │
  │                   │                     │                        │                   │
  │  POST /salary/save│                     │                        │                   │
  │  (form data)      │                     │                        │                   │
  │──────────────────>│                     │                        │                   │
  │                   │  @Valid 검증         │                        │                   │
  │                   │─── 실패 시 ────────>│                        │                   │
  │  form.html (에러) │<──────────────────  │                        │                   │
  │<──────────────────│                     │                        │                   │
  │                   │                     │                        │                   │
  │                   │  save(form)         │                        │                   │
  │                   │────────────────────>│                        │                   │
  │                   │                     │  findByYearMonth(ym)   │                   │
  │                   │                     │───────────────────────>│  SELECT            │
  │                   │                     │                        │─────────────────> │
  │                   │                     │   중복 체크 OK         │                   │
  │                   │                     │<───────────────────────│<──────────────────│
  │                   │                     │                        │                   │
  │                   │                     │  calculateTotals()     │                   │
  │                   │                     │  (gross = base+bonus   │                   │
  │                   │                     │   +extra)              │                   │
  │                   │                     │  (net = gross          │                   │
  │                   │                     │   -deduction)          │                   │
  │                   │                     │                        │                   │
  │                   │                     │  repository.save()     │                   │
  │                   │                     │───────────────────────>│  INSERT            │
  │                   │                     │                        │─────────────────> │
  │                   │                     │   saved record         │                   │
  │                   │                     │<───────────────────────│<──────────────────│
  │                   │  redirect           │                        │                   │
  │                   │<────────────────────│                        │                   │
  │  302 /salary/list │                     │                        │                   │
  │<──────────────────│                     │                        │                   │
```

### 대시보드 조회 플로우

```
사용자        DashboardController      SalaryService       SalaryRecordRepository     DB
  │                  │                      │                       │                   │
  │  GET /           │                      │                       │                   │
  │─────────────────>│                      │                       │                   │
  │                  │  getAnnualSummaries() │                       │                   │
  │                  │─────────────────────>│                       │                   │
  │                  │                      │  findAllYears()       │                   │
  │                  │                      │──────────────────────>│  SELECT DISTINCT   │
  │                  │                      │                       │─────────────────> │
  │                  │                      │  [2025, 2026]         │                   │
  │                  │                      │<──────────────────────│<──────────────────│
  │                  │                      │                       │                   │
  │                  │                      │  ── for each year ──  │                   │
  │                  │                      │  sumGrossTotal(year)  │  SUM(gross_total)  │
  │                  │                      │  sumNetSalary(year)   │  SUM(net_salary)   │
  │                  │                      │  sumBaseSalary(year)  │  SUM(base_salary)  │
  │                  │                      │  sumBonus(year)       │  SUM(bonus)        │
  │                  │                      │  sumExtraIncome(year) │  SUM(extra_income) │
  │                  │                      │  countByYear(year)    │  COUNT(*)          │
  │                  │                      │──────────────────────>│─────────────────> │
  │                  │                      │<──────────────────────│<──────────────────│
  │                  │                      │                       │                   │
  │                  │                      │  상승률 계산           │                   │
  │                  │                      │  (전년대비, 누적)      │                   │
  │                  │                      │  ── end for ──        │                   │
  │                  │                      │                       │                   │
  │                  │  List<AnnualSummary>  │                       │                   │
  │                  │<─────────────────────│                       │                   │
  │                  │                      │                       │                   │
  │                  │  getTotalNetIncome()  │                       │                   │
  │                  │─────────────────────>│  sumAllNetSalary()    │                   │
  │                  │                      │──────────────────────>│  SUM(net_salary)   │
  │                  │                      │<──────────────────────│─────────────────> │
  │                  │<─────────────────────│                       │<──────────────────│
  │                  │                      │                       │                   │
  │  dashboard.html  │                      │                       │                   │
  │  (cards + charts)│                      │                       │                   │
  │<─────────────────│                      │                       │                   │
```

### 급여 삭제 플로우

```
사용자          SalaryController       SalaryService       SalaryRecordRepository    DB
  │                   │                     │                       │                  │
  │  [삭제] 클릭      │                     │                       │                  │
  │  → 확인 모달 표시 │                     │                       │                  │
  │                   │                     │                       │                  │
  │  [삭제 확인]      │                     │                       │                  │
  │  POST /salary/    │                     │                       │                  │
  │    delete/{id}    │                     │                       │                  │
  │──────────────────>│                     │                       │                  │
  │                   │  delete(id)         │                       │                  │
  │                   │────────────────────>│                       │                  │
  │                   │                     │  deleteById(id)       │                  │
  │                   │                     │──────────────────────>│  DELETE           │
  │                   │                     │                       │────────────────> │
  │                   │                     │         OK            │                  │
  │                   │                     │<──────────────────────│<─────────────────│
  │                   │  redirect           │                       │                  │
  │                   │<────────────────────│                       │                  │
  │  302 /salary/list │                     │                       │                  │
  │<──────────────────│                     │                       │                  │
```

## 디자인 컨셉

토스(Toss) 스타일의 깔끔한 UI를 목표로 합니다.

- **컬러:** 네이비(#1B2838) 사이드바 + 민트(#4ECDC4) 포인트
- **카드 UI:** 넉넉한 여백, 큰 숫자 강조
- **상승/하락:** 민트(+) / 코랄(-) 뱃지로 직관적 표시

## 향후 계획

- **2단계:** 급여명세서 업로드/파싱 (OCR 또는 AI)
- **3단계:** 멀티유저 서비스 (회원가입/로그인)
