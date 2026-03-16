# Salary Dashboard Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 매월 급여를 기록하고 연봉 상승률을 시각적으로 추적하는 개인용 웹 대시보드 구축

**Architecture:** Spring Boot SSR 모놀리식 앱. Thymeleaf로 서버사이드 렌더링, HTMX로 부드러운 인터랙션, Chart.js로 시각화. MySQL에 단일 테이블(SalaryRecord)로 데이터 저장, 연도별 요약은 쿼리로 산출.

**Tech Stack:** Java 17+, Spring Boot 3.x, Thymeleaf, HTMX (CDN), Chart.js (CDN), Spring Data JPA, MySQL, Gradle

**Spec:** `docs/superpowers/specs/2026-03-16-salary-dashboard-design.md`

---

## File Structure

```
salary-dashboard/
├── build.gradle
├── settings.gradle
├── src/
│   ├── main/
│   │   ├── java/com/salary/dashboard/
│   │   │   ├── SalaryDashboardApplication.java       # Spring Boot main
│   │   │   ├── domain/
│   │   │   │   └── SalaryRecord.java                  # JPA 엔티티
│   │   │   ├── repository/
│   │   │   │   └── SalaryRecordRepository.java        # JPA Repository + 커스텀 쿼리
│   │   │   ├── dto/
│   │   │   │   ├── SalaryForm.java                    # 입력 폼 DTO (validation)
│   │   │   │   └── AnnualSummaryDto.java              # 연도별 요약 DTO
│   │   │   ├── service/
│   │   │   │   └── SalaryService.java                 # 비즈니스 로직
│   │   │   └── controller/
│   │   │       ├── DashboardController.java           # 메인 대시보드
│   │   │       └── SalaryController.java              # 급여 CRUD
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── templates/
│   │       │   ├── layout.html                        # 공통 레이아웃 (Thymeleaf Layout)
│   │       │   ├── dashboard.html                     # 메인 대시보드
│   │       │   ├── salary/
│   │       │   │   ├── form.html                      # 급여 입력/수정 폼
│   │       │   │   └── list.html                      # 급여 목록
│   │       │   └── annual.html                        # 연도별 비교
│   │       └── static/
│   │           ├── css/
│   │           │   └── style.css                      # 토스 스타일 CSS
│   │           └── js/
│   │               └── chart-config.js                # Chart.js 초기화 + HTMX 연동
│   └── test/
│       └── groovy/com/salary/dashboard/
│           ├── domain/
│           │   └── SalaryRecordSpec.groovy
│           ├── service/
│           │   └── SalaryServiceSpec.groovy
│           ├── controller/
│           │   ├── SalaryControllerSpec.groovy
│           │   └── DashboardControllerSpec.groovy
│           └── repository/
│               └── SalaryRecordRepositorySpec.groovy
```

---

## Chunk 1: Project Setup + Domain + Repository

### Task 1: Spring Boot 프로젝트 초기화

**Files:**
- Create: `salary-dashboard/build.gradle`
- Create: `salary-dashboard/settings.gradle`
- Create: `salary-dashboard/src/main/java/com/salary/dashboard/SalaryDashboardApplication.java`
- Create: `salary-dashboard/src/main/resources/application.yml`

- [ ] **Step 1: 프로젝트 디렉토리 생성**

```bash
mkdir -p salary-dashboard/src/main/java/com/salary/dashboard
mkdir -p salary-dashboard/src/main/resources/templates/salary
mkdir -p salary-dashboard/src/main/resources/static/css
mkdir -p salary-dashboard/src/main/resources/static/js
mkdir -p salary-dashboard/src/test/groovy/com/salary/dashboard
```

- [ ] **Step 2: build.gradle 작성**

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.3'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.salary'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect:3.3.0'
    implementation 'com.fasterxml.jackson.datatype:jackson-datatype-jsr310'
    runtimeOnly 'com.mysql:mysql-connector-j'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.spockframework:spock-core:2.4-M4-groovy-4.0'
    testImplementation 'org.spockframework:spock-spring:2.4-M4-groovy-4.0'
    testRuntimeOnly 'com.h2database:h2'
}

apply plugin: 'groovy'

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 3: settings.gradle 작성**

```groovy
rootProject.name = 'salary-dashboard'
```

- [ ] **Step 4: application.yml 작성**

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/salary_dashboard?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
    username: root
    password: ${DB_PASSWORD:root}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        format_sql: true
  thymeleaf:
    cache: false

server:
  port: 8080

---
spring:
  config:
    activate:
      on-profile: test
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
  jpa:
    hibernate:
      ddl-auto: create-drop
```

- [ ] **Step 5: Application 클래스 작성**

```java
package com.salary.dashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SalaryDashboardApplication {
    public static void main(String[] args) {
        SpringApplication.run(SalaryDashboardApplication.class, args);
    }
}
```

- [ ] **Step 6: Gradle Wrapper 생성**

```bash
cd salary-dashboard && gradle wrapper --gradle-version 8.12
```

Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/` 파일 생성됨

- [ ] **Step 7: 빌드 확인**

```bash
cd salary-dashboard && ./gradlew build -x test
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git init
echo "build/\n.gradle/\n*.iml\n.idea/\nout/" > .gitignore
git add .
git commit -m "chore: init Spring Boot project with dependencies"
```

---

### Task 2: SalaryRecord 엔티티

**Files:**
- Create: `src/main/java/com/salary/dashboard/domain/SalaryRecord.java`
- Create: `src/test/groovy/com/salary/dashboard/domain/SalaryRecordSpec.groovy`

- [ ] **Step 1: 엔티티 테스트 작성**

```groovy
package com.salary.dashboard.domain

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.YearMonth

class SalaryRecordSpec extends Specification {

    @Subject
    SalaryRecord record = new SalaryRecord()

    @Unroll
    def "grossTotal은 기본급(#base) + 성과급(#bonus) + 영끌(#extra) = #expected"() {
        given:
        record.baseSalary = base
        record.bonus = bonus
        record.extraIncome = extra
        record.totalDeduction = 0L

        when:
        record.calculateTotals()

        then:
        record.grossTotal == expected

        where:
        base        | bonus       | extra     || expected
        2_169_336L  | 0L          | 747_330L  || 2_916_666L
        2_000_000L  | 1_000_000L  | 500_000L  || 3_500_000L
        2_000_000L  | 0L          | 0L        || 2_000_000L
    }

    def "netSalary는 세전총액에서 공제액 차감"() {
        given:
        record.baseSalary = 2_169_336L
        record.bonus = 0L
        record.extraIncome = 747_330L
        record.totalDeduction = -496_180L

        when:
        record.calculateTotals()

        then:
        record.netSalary == 3_412_846L
    }

    def "year는 yearMonth에서 파생된다"() {
        given:
        record.yearMonth = YearMonth.of(2026, 3)

        when:
        record.deriveYear()

        then:
        record.year == 2026
    }

    def "공제액 음수 허용 - 연말정산 환급 시 실수령액이 세전보다 크다"() {
        given:
        record.baseSalary = 2_000_000L
        record.bonus = 0L
        record.extraIncome = 0L
        record.totalDeduction = -500_000L

        when:
        record.calculateTotals()

        then:
        record.netSalary == 2_500_000L
        record.netSalary > record.grossTotal
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew test --tests "com.salary.dashboard.domain.SalaryRecordSpec"
```

Expected: FAIL — `SalaryRecord` 클래스 없음

- [ ] **Step 3: SalaryRecord 엔티티 구현**

```java
package com.salary.dashboard.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "salary_record", indexes = {
    @Index(name = "idx_year", columnList = "year")
})
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM")
    private YearMonth yearMonth;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Long baseSalary;

    @Column(nullable = false)
    private Long bonus;

    @Column(nullable = false)
    private Long extraIncome;

    @Column(nullable = false)
    private Long grossTotal;

    @Column(nullable = false)
    private Long totalDeduction;

    @Column(nullable = false)
    private Long netSalary;

    private String memo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        deriveYear();
        calculateTotals();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        deriveYear();
        calculateTotals();
    }

    public void calculateTotals() {
        this.grossTotal = this.baseSalary + this.bonus + this.extraIncome;
        this.netSalary = this.grossTotal - this.totalDeduction;
    }

    public void deriveYear() {
        if (this.yearMonth != null) {
            this.year = this.yearMonth.getYear();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public YearMonth getYearMonth() { return yearMonth; }
    public void setYearMonth(YearMonth yearMonth) { this.yearMonth = yearMonth; }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Long getBaseSalary() { return baseSalary; }
    public void setBaseSalary(Long baseSalary) { this.baseSalary = baseSalary; }

    public Long getBonus() { return bonus; }
    public void setBonus(Long bonus) { this.bonus = bonus; }

    public Long getExtraIncome() { return extraIncome; }
    public void setExtraIncome(Long extraIncome) { this.extraIncome = extraIncome; }

    public Long getGrossTotal() { return grossTotal; }
    public void setGrossTotal(Long grossTotal) { this.grossTotal = grossTotal; }

    public Long getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(Long totalDeduction) { this.totalDeduction = totalDeduction; }

    public Long getNetSalary() { return netSalary; }
    public void setNetSalary(Long netSalary) { this.netSalary = netSalary; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
```

- [ ] **Step 4: YearMonth JPA 컨버터 추가**

JPA는 `YearMonth`를 자동 매핑하지 못하므로 컨버터가 필요합니다.

Create: `src/main/java/com/salary/dashboard/domain/YearMonthAttributeConverter.java`

```java
package com.salary.dashboard.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

@Converter(autoApply = true)
public class YearMonthAttributeConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth yearMonth) {
        return yearMonth != null ? yearMonth.toString() : null;
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        return dbData != null ? YearMonth.parse(dbData) : null;
    }
}
```

- [ ] **Step 5: 테스트 통과 확인**

```bash
./gradlew test --tests "com.salary.dashboard.domain.SalaryRecordSpec"
```

Expected: 모든 테스트 PASSED (where 블록 포함 6 iterations)

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat: add SalaryRecord entity with auto-calculation logic"
```

---

### Task 3: Repository

**Files:**
- Create: `src/main/java/com/salary/dashboard/repository/SalaryRecordRepository.java`
- Create: `src/test/groovy/com/salary/dashboard/repository/SalaryRecordRepositorySpec.groovy`

- [ ] **Step 1: Repository 테스트 작성**

```groovy
package com.salary.dashboard.repository

import com.salary.dashboard.domain.SalaryRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import spock.lang.Specification

import java.time.YearMonth

@DataJpaTest
@ActiveProfiles("test")
class SalaryRecordRepositorySpec extends Specification {

    @Autowired
    SalaryRecordRepository repository

    def createRecord(YearMonth ym, long base, long bonus, long extra, long deduction) {
        def r = new SalaryRecord()
        r.yearMonth = ym
        r.baseSalary = base
        r.bonus = bonus
        r.extraIncome = extra
        r.totalDeduction = deduction
        return r
    }

    def "findByYear - 해당 연도 레코드만 반환한다"() {
        given:
        repository.save(createRecord(YearMonth.of(2025, 1), 2_000_000, 0, 500_000, 300_000))
        repository.save(createRecord(YearMonth.of(2025, 2), 2_000_000, 0, 500_000, 300_000))
        repository.save(createRecord(YearMonth.of(2026, 1), 2_100_000, 0, 500_000, 300_000))

        when:
        def result = repository.findByYearOrderByYearMonthAsc(2025)

        then:
        result.size() == 2
        result[0].yearMonth == YearMonth.of(2025, 1)
    }

    def "findAllYears - 연도 목록을 오름차순 반환한다"() {
        given:
        repository.save(createRecord(YearMonth.of(2024, 6), 2_000_000, 0, 0, 300_000))
        repository.save(createRecord(YearMonth.of(2025, 1), 2_000_000, 0, 0, 300_000))
        repository.save(createRecord(YearMonth.of(2026, 1), 2_000_000, 0, 0, 300_000))

        when:
        def years = repository.findAllYears()

        then:
        years == [2024, 2025, 2026]
    }

    def "existsByYearMonth - 중복 연월 체크"() {
        given:
        repository.save(createRecord(YearMonth.of(2026, 3), 2_000_000, 0, 0, 300_000))

        expect:
        repository.existsByYearMonth(YearMonth.of(2026, 3)) == true
        repository.existsByYearMonth(YearMonth.of(2026, 4)) == false
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew test --tests "com.salary.dashboard.repository.SalaryRecordRepositorySpec"
```

Expected: FAIL — `SalaryRecordRepository` 없음

- [ ] **Step 3: Repository 구현**

```java
package com.salary.dashboard.repository;

import com.salary.dashboard.domain.SalaryRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {

    List<SalaryRecord> findByYearOrderByYearMonthAsc(Integer year);

    List<SalaryRecord> findAllByOrderByYearMonthDesc();

    Optional<SalaryRecord> findByYearMonth(YearMonth yearMonth);

    boolean existsByYearMonth(YearMonth yearMonth);

    @Query("SELECT DISTINCT s.year FROM SalaryRecord s ORDER BY s.year ASC")
    List<Integer> findAllYears();

    @Query("SELECT SUM(s.grossTotal) FROM SalaryRecord s WHERE s.year = :year")
    Long sumGrossTotalByYear(Integer year);

    @Query("SELECT SUM(s.netSalary) FROM SalaryRecord s WHERE s.year = :year")
    Long sumNetSalaryByYear(Integer year);

    @Query("SELECT SUM(s.baseSalary) FROM SalaryRecord s WHERE s.year = :year")
    Long sumBaseSalaryByYear(Integer year);

    @Query("SELECT SUM(s.bonus) FROM SalaryRecord s WHERE s.year = :year")
    Long sumBonusByYear(Integer year);

    @Query("SELECT SUM(s.extraIncome) FROM SalaryRecord s WHERE s.year = :year")
    Long sumExtraIncomeByYear(Integer year);

    @Query("SELECT COUNT(s) FROM SalaryRecord s WHERE s.year = :year")
    Long countByYear(Integer year);

    @Query("SELECT SUM(s.netSalary) FROM SalaryRecord s")
    Long sumAllNetSalary();
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew test --tests "com.salary.dashboard.repository.SalaryRecordRepositorySpec"
```

Expected: 3 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add SalaryRecordRepository with annual query methods"
```

---

## Chunk 2: Service + DTOs

### Task 4: DTO 클래스

**Files:**
- Create: `src/main/java/com/salary/dashboard/dto/SalaryForm.java`
- Create: `src/main/java/com/salary/dashboard/dto/AnnualSummaryDto.java`

- [ ] **Step 1: SalaryForm DTO 작성**

```java
package com.salary.dashboard.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SalaryForm {

    private Long id;

    @NotNull(message = "연월을 선택해주세요")
    private String yearMonth; // "2026-03" 형식, 컨트롤러에서 YearMonth로 변환

    @NotNull(message = "기본급을 입력해주세요")
    @Min(value = 0, message = "기본급은 0 이상이어야 합니다")
    private Long baseSalary;

    @NotNull(message = "성과급을 입력해주세요")
    @Min(value = 0, message = "성과급은 0 이상이어야 합니다")
    private Long bonus;

    @NotNull(message = "영끌을 입력해주세요")
    @Min(value = 0, message = "영끌은 0 이상이어야 합니다")
    private Long extraIncome;

    @NotNull(message = "총 공제액을 입력해주세요")
    private Long totalDeduction; // 음수 허용

    private Long netSalary; // 자동 계산, 수동 수정 가능

    private String memo;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getYearMonth() { return yearMonth; }
    public void setYearMonth(String yearMonth) { this.yearMonth = yearMonth; }

    public Long getBaseSalary() { return baseSalary; }
    public void setBaseSalary(Long baseSalary) { this.baseSalary = baseSalary; }

    public Long getBonus() { return bonus; }
    public void setBonus(Long bonus) { this.bonus = bonus; }

    public Long getExtraIncome() { return extraIncome; }
    public void setExtraIncome(Long extraIncome) { this.extraIncome = extraIncome; }

    public Long getTotalDeduction() { return totalDeduction; }
    public void setTotalDeduction(Long totalDeduction) { this.totalDeduction = totalDeduction; }

    public Long getNetSalary() { return netSalary; }
    public void setNetSalary(Long netSalary) { this.netSalary = netSalary; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
}
```

- [ ] **Step 2: AnnualSummaryDto 작성**

```java
package com.salary.dashboard.dto;

public class AnnualSummaryDto {

    private Integer year;
    private Long annualGross;        // 연봉 (grossTotal 합계)
    private Long estimatedAnnual;    // 예상 연봉 (진행 중인 해)
    private Long annualNet;          // 연간 실수령 합계
    private Long annualBaseSalary;   // 연간 기본급 합계
    private Long annualBonus;        // 연간 성과급 합계
    private Long annualExtraIncome;  // 연간 영끌 합계
    private Double avgMonthlyNet;    // 월평균 실수령액
    private Double growthRate;       // 전년 대비 상승률 (%)
    private Double cumulativeGrowthRate; // 누적 상승률 (%)
    private Long monthCount;         // 입력 월수
    private boolean dataInsufficient; // 3개월 미만 여부

    // Getters and Setters
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }

    public Long getAnnualGross() { return annualGross; }
    public void setAnnualGross(Long annualGross) { this.annualGross = annualGross; }

    public Long getEstimatedAnnual() { return estimatedAnnual; }
    public void setEstimatedAnnual(Long estimatedAnnual) { this.estimatedAnnual = estimatedAnnual; }

    public Long getAnnualNet() { return annualNet; }
    public void setAnnualNet(Long annualNet) { this.annualNet = annualNet; }

    public Long getAnnualBaseSalary() { return annualBaseSalary; }
    public void setAnnualBaseSalary(Long annualBaseSalary) { this.annualBaseSalary = annualBaseSalary; }

    public Long getAnnualBonus() { return annualBonus; }
    public void setAnnualBonus(Long annualBonus) { this.annualBonus = annualBonus; }

    public Long getAnnualExtraIncome() { return annualExtraIncome; }
    public void setAnnualExtraIncome(Long annualExtraIncome) { this.annualExtraIncome = annualExtraIncome; }

    public Double getAvgMonthlyNet() { return avgMonthlyNet; }
    public void setAvgMonthlyNet(Double avgMonthlyNet) { this.avgMonthlyNet = avgMonthlyNet; }

    public Double getGrowthRate() { return growthRate; }
    public void setGrowthRate(Double growthRate) { this.growthRate = growthRate; }

    public Double getCumulativeGrowthRate() { return cumulativeGrowthRate; }
    public void setCumulativeGrowthRate(Double cumulativeGrowthRate) { this.cumulativeGrowthRate = cumulativeGrowthRate; }

    public Long getMonthCount() { return monthCount; }
    public void setMonthCount(Long monthCount) { this.monthCount = monthCount; }

    public boolean isDataInsufficient() { return dataInsufficient; }
    public void setDataInsufficient(boolean dataInsufficient) { this.dataInsufficient = dataInsufficient; }
}
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add SalaryForm and AnnualSummaryDto"
```

---

### Task 5: SalaryService

**Files:**
- Create: `src/main/java/com/salary/dashboard/service/SalaryService.java`
- Create: `src/test/groovy/com/salary/dashboard/service/SalaryServiceSpec.groovy`

- [ ] **Step 1: Service 테스트 작성**

```groovy
package com.salary.dashboard.service

import com.salary.dashboard.dto.SalaryForm
import com.salary.dashboard.repository.SalaryRecordRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SalaryServiceSpec extends Specification {

    @Autowired
    SalaryService salaryService

    @Autowired
    SalaryRecordRepository repository

    def createForm(String ym, long base, long bonus, long extra, long deduction) {
        def form = new SalaryForm()
        form.yearMonth = ym
        form.baseSalary = base
        form.bonus = bonus
        form.extraIncome = extra
        form.totalDeduction = deduction
        return form
    }

    def "save - 새 급여 기록을 저장하고 자동 계산한다"() {
        given:
        def form = createForm("2026-03", 2_169_336, 0, 747_330, -496_180)

        when:
        def saved = salaryService.save(form)

        then:
        saved.id != null
        saved.grossTotal == 2_916_666L
        saved.netSalary == 3_412_846L
        saved.year == 2026
    }

    def "save - 중복 연월 입력 시 예외 발생"() {
        given:
        salaryService.save(createForm("2026-03", 2_000_000, 0, 0, 300_000))

        when:
        salaryService.save(createForm("2026-03", 2_100_000, 0, 0, 300_000))

        then:
        def e = thrown(IllegalArgumentException)
        e.message.contains("이미 등록된 연월")
    }

    def "getAnnualSummaries - 연도별 요약을 산출한다"() {
        given:
        salaryService.save(createForm("2025-01", 2_000_000, 0, 500_000, 300_000))
        salaryService.save(createForm("2025-06", 2_000_000, 1_000_000, 500_000, 300_000))
        salaryService.save(createForm("2026-01", 2_100_000, 0, 500_000, 300_000))

        when:
        def summaries = salaryService.getAnnualSummaries()

        then:
        summaries.size() == 2

        and: "2025년 요약"
        def y2025 = summaries[0]
        y2025.year == 2025
        y2025.annualGross == 6_000_000L
        y2025.growthRate == null

        and: "2026년은 상승률이 있다"
        def y2026 = summaries[1]
        y2026.year == 2026
        y2026.growthRate != null
    }

    def "getAnnualSummaries - 누적 상승률을 계산한다"() {
        given: "3년치 데이터 (매년 10% 상승)"
        salaryService.save(createForm("2024-01", 2_500_000, 0, 0, 300_000))
        salaryService.save(createForm("2025-01", 2_750_000, 0, 0, 300_000))
        salaryService.save(createForm("2026-01", 3_025_000, 0, 0, 300_000))

        when:
        def summaries = salaryService.getAnnualSummaries()

        then: "첫 해는 상승률 없음"
        summaries[0].growthRate == null
        summaries[0].cumulativeGrowthRate == null

        and: "2025: 전년 대비 +10%"
        Math.abs(summaries[1].growthRate - 10.0) < 0.1
        Math.abs(summaries[1].cumulativeGrowthRate - 10.0) < 0.1

        and: "2026: 전년 대비 +10%, 누적 +21%"
        Math.abs(summaries[2].growthRate - 10.0) < 0.1
        Math.abs(summaries[2].cumulativeGrowthRate - 21.0) < 0.1
    }

    def "getAnnualSummaries - 3개월 미만이면 데이터 부족 표시"() {
        given:
        salaryService.save(createForm("2026-01", 2_000_000, 0, 0, 300_000))
        salaryService.save(createForm("2026-02", 2_000_000, 0, 0, 300_000))

        when:
        def summaries = salaryService.getAnnualSummaries()

        then:
        summaries[0].dataInsufficient == true
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew test --tests "com.salary.dashboard.service.SalaryServiceSpec"
```

Expected: FAIL — `SalaryService` 없음

- [ ] **Step 3: SalaryService 구현**

```java
package com.salary.dashboard.service;

import com.salary.dashboard.domain.SalaryRecord;
import com.salary.dashboard.dto.AnnualSummaryDto;
import com.salary.dashboard.dto.SalaryForm;
import com.salary.dashboard.repository.SalaryRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class SalaryService {

    private final SalaryRecordRepository repository;

    public SalaryService(SalaryRecordRepository repository) {
        this.repository = repository;
    }

    public SalaryRecord save(SalaryForm form) {
        YearMonth ym = YearMonth.parse(form.getYearMonth());

        repository.findByYearMonth(ym).ifPresent(existing -> {
            if (!existing.getId().equals(form.getId())) {
                throw new IllegalArgumentException("이미 등록된 연월입니다: " + form.getYearMonth());
            }
        });

        SalaryRecord record;
        if (form.getId() != null) {
            record = repository.findById(form.getId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기록: " + form.getId()));
        } else {
            record = new SalaryRecord();
        }

        record.setYearMonth(ym);
        record.setBaseSalary(form.getBaseSalary());
        record.setBonus(form.getBonus());
        record.setExtraIncome(form.getExtraIncome());
        record.setTotalDeduction(form.getTotalDeduction());

        record.calculateTotals();

        // netSalary: 사용자가 수동 입력했으면 그 값으로 오버라이드
        if (form.getNetSalary() != null) {
            record.setNetSalary(form.getNetSalary());
        }

        return repository.save(record);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public SalaryRecord findById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 기록: " + id));
    }

    public List<SalaryRecord> findAll() {
        return repository.findAllByOrderByYearMonthDesc();
    }

    public List<SalaryRecord> findByYear(Integer year) {
        return repository.findByYearOrderByYearMonthAsc(year);
    }

    @Transactional(readOnly = true)
    public List<AnnualSummaryDto> getAnnualSummaries() {
        List<Integer> years = repository.findAllYears();
        List<AnnualSummaryDto> summaries = new ArrayList<>();
        Long firstYearGross = null;

        for (int i = 0; i < years.size(); i++) {
            Integer year = years.get(i);
            AnnualSummaryDto dto = new AnnualSummaryDto();
            dto.setYear(year);

            Long gross = repository.sumGrossTotalByYear(year);
            Long net = repository.sumNetSalaryByYear(year);
            Long base = repository.sumBaseSalaryByYear(year);
            Long bonus = repository.sumBonusByYear(year);
            Long extra = repository.sumExtraIncomeByYear(year);
            Long count = repository.countByYear(year);

            dto.setAnnualGross(gross != null ? gross : 0L);
            dto.setAnnualNet(net != null ? net : 0L);
            dto.setAnnualBaseSalary(base != null ? base : 0L);
            dto.setAnnualBonus(bonus != null ? bonus : 0L);
            dto.setAnnualExtraIncome(extra != null ? extra : 0L);
            dto.setMonthCount(count != null ? count : 0L);
            dto.setDataInsufficient(dto.getMonthCount() < 3);

            if (dto.getMonthCount() > 0) {
                dto.setAvgMonthlyNet((double) dto.getAnnualNet() / dto.getMonthCount());
                dto.setEstimatedAnnual(dto.getAnnualGross() * 12 / dto.getMonthCount());
            }

            // 첫 연도 기록
            if (i == 0) {
                firstYearGross = dto.getAnnualGross();
            }

            // 전년 대비 상승률
            if (i > 0) {
                Long prevGross = summaries.get(i - 1).getAnnualGross();
                if (prevGross != null && prevGross > 0) {
                    dto.setGrowthRate((double) (dto.getAnnualGross() - prevGross) / prevGross * 100);
                }
            }

            // 누적 상승률
            if (i > 0 && firstYearGross != null && firstYearGross > 0) {
                dto.setCumulativeGrowthRate(
                    (double) (dto.getAnnualGross() - firstYearGross) / firstYearGross * 100
                );
            }

            summaries.add(dto);
        }

        return summaries;
    }

    @Transactional(readOnly = true)
    public Long getTotalNetIncome() {
        Long total = repository.sumAllNetSalary();
        return total != null ? total : 0L;
    }

    @Transactional(readOnly = true)
    public List<Integer> getAllYears() {
        return repository.findAllYears();
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew test --tests "com.salary.dashboard.service.SalaryServiceSpec"
```

Expected: 5 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add SalaryService with annual summary calculation"
```

---

## Chunk 3: Controllers

### Task 6: SalaryController (CRUD)

**Files:**
- Create: `src/main/java/com/salary/dashboard/controller/SalaryController.java`
- Create: `src/test/groovy/com/salary/dashboard/controller/SalaryControllerSpec.groovy`

- [ ] **Step 1: Controller 테스트 작성**

```groovy
package com.salary.dashboard.controller

import com.salary.dashboard.domain.SalaryRecord
import com.salary.dashboard.repository.SalaryRecordRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.time.YearMonth

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SalaryControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    SalaryRecordRepository repository

    def createRecord(YearMonth ym, long base) {
        def record = new SalaryRecord()
        record.yearMonth = ym
        record.baseSalary = base
        record.bonus = 0L
        record.extraIncome = 0L
        record.totalDeduction = 300_000L
        return repository.save(record)
    }

    def "GET /salary/new - 급여 입력 폼 페이지를 반환한다"() {
        when:
        def result = mockMvc.perform(get("/salary/new"))

        then:
        result.andExpect(status().isOk())
              .andExpect(view().name("salary/form"))
              .andExpect(model().attributeExists("salaryForm"))
    }

    def "POST /salary/save - 저장 후 목록으로 리다이렉트한다"() {
        when:
        def result = mockMvc.perform(post("/salary/save")
                .param("yearMonth", "2026-03")
                .param("baseSalary", "2169336")
                .param("bonus", "0")
                .param("extraIncome", "747330")
                .param("totalDeduction", "-496180"))

        then:
        result.andExpect(status().is3xxRedirection())
              .andExpect(redirectedUrl("/salary/list"))
    }

    def "POST /salary/save - 검증 실패 시 폼을 다시 표시한다"() {
        when:
        def result = mockMvc.perform(post("/salary/save")
                .param("yearMonth", "")
                .param("baseSalary", "-1"))

        then:
        result.andExpect(status().isOk())
              .andExpect(view().name("salary/form"))
    }

    def "GET /salary/list - 급여 목록 페이지를 반환한다"() {
        when:
        def result = mockMvc.perform(get("/salary/list"))

        then:
        result.andExpect(status().isOk())
              .andExpect(view().name("salary/list"))
              .andExpect(model().attributeExists("records"))
    }

    def "POST /salary/delete - 삭제 후 목록으로 리다이렉트한다"() {
        given:
        def record = createRecord(YearMonth.of(2026, 1), 2_000_000L)

        when:
        def result = mockMvc.perform(post("/salary/delete/" + record.id))

        then:
        result.andExpect(status().is3xxRedirection())
              .andExpect(redirectedUrl("/salary/list"))

        and:
        repository.findById(record.id).isEmpty()
    }

    def "GET /salary/edit/{id} - 수정 폼에 기존 데이터가 채워진다"() {
        given:
        def record = createRecord(YearMonth.of(2026, 3), 2_000_000L)

        when:
        def result = mockMvc.perform(get("/salary/edit/" + record.id))

        then:
        result.andExpect(status().isOk())
              .andExpect(view().name("salary/form"))
              .andExpect(model().attributeExists("salaryForm"))
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew test --tests "com.salary.dashboard.controller.SalaryControllerSpec"
```

Expected: FAIL

- [ ] **Step 3: SalaryController 구현**

```java
package com.salary.dashboard.controller;

import com.salary.dashboard.domain.SalaryRecord;
import com.salary.dashboard.dto.SalaryForm;
import com.salary.dashboard.service.SalaryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/salary")
public class SalaryController {

    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("salaryForm", new SalaryForm());
        return "salary/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        SalaryRecord record = salaryService.findById(id);
        SalaryForm form = new SalaryForm();
        form.setId(record.getId());
        form.setYearMonth(record.getYearMonth().toString());
        form.setBaseSalary(record.getBaseSalary());
        form.setBonus(record.getBonus());
        form.setExtraIncome(record.getExtraIncome());
        form.setTotalDeduction(record.getTotalDeduction());
        form.setNetSalary(record.getNetSalary());
        form.setMemo(record.getMemo());
        model.addAttribute("salaryForm", form);
        return "salary/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute SalaryForm salaryForm,
                       BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "salary/form";
        }
        try {
            salaryService.save(salaryForm);
            return "redirect:/salary/list";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("yearMonth", "duplicate", e.getMessage());
            return "salary/form";
        }
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) Integer year, Model model) {
        if (year != null) {
            model.addAttribute("records", salaryService.findByYear(year));
        } else {
            model.addAttribute("records", salaryService.findAll());
        }
        model.addAttribute("years", salaryService.getAllYears());
        model.addAttribute("selectedYear", year);
        return "salary/list";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        salaryService.delete(id);
        return "redirect:/salary/list";
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew test --tests "com.salary.dashboard.controller.SalaryControllerSpec"
```

Expected: 6 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add SalaryController with CRUD endpoints"
```

---

### Task 7: DashboardController

**Files:**
- Create: `src/main/java/com/salary/dashboard/controller/DashboardController.java`
- Create: `src/test/groovy/com/salary/dashboard/controller/DashboardControllerSpec.groovy`

- [ ] **Step 1: Dashboard 테스트 작성**

```groovy
package com.salary.dashboard.controller

import com.salary.dashboard.dto.SalaryForm
import com.salary.dashboard.service.SalaryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @Autowired
    SalaryService salaryService

    def "GET / - 데이터 없으면 빈 상태를 표시한다"() {
        when:
        def result = mockMvc.perform(get("/"))

        then:
        result.andExpect(status().isOk())
              .andExpect(view().name("dashboard"))
              .andExpect(model().attribute("isEmpty", true))
    }

    def "GET / - 데이터 있으면 대시보드를 표시한다"() {
        given:
        def form = new SalaryForm()
        form.yearMonth = "2026-01"
        form.baseSalary = 2_000_000L
        form.bonus = 0L
        form.extraIncome = 500_000L
        form.totalDeduction = 300_000L
        salaryService.save(form)

        when:
        def result = mockMvc.perform(get("/"))

        then:
        result.andExpect(status().isOk())
              .andExpect(view().name("dashboard"))
              .andExpect(model().attribute("isEmpty", false))
              .andExpect(model().attributeExists("currentAnnual"))
              .andExpect(model().attributeExists("totalNetIncome"))
    }

    def "GET /annual - 연도별 비교 페이지를 반환한다"() {
        when:
        def result = mockMvc.perform(get("/annual"))

        then:
        result.andExpect(status().isOk())
              .andExpect(view().name("annual"))
              .andExpect(model().attributeExists("summaries"))
    }
}
```

- [ ] **Step 2: 테스트 실패 확인**

```bash
./gradlew test --tests "com.salary.dashboard.controller.DashboardControllerSpec"
```

Expected: FAIL

- [ ] **Step 3: DashboardController 구현**

```java
package com.salary.dashboard.controller;

import com.salary.dashboard.dto.AnnualSummaryDto;
import com.salary.dashboard.service.SalaryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Year;
import java.util.List;

@Controller
public class DashboardController {

    private final SalaryService salaryService;

    public DashboardController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<AnnualSummaryDto> summaries = salaryService.getAnnualSummaries();

        if (summaries.isEmpty()) {
            model.addAttribute("isEmpty", true);
            return "dashboard";
        }

        model.addAttribute("isEmpty", false);

        // 현재 연도 또는 가장 최근 연도의 요약
        int currentYear = Year.now().getValue();
        AnnualSummaryDto currentSummary = summaries.stream()
            .filter(s -> s.getYear() == currentYear)
            .findFirst()
            .orElse(summaries.get(summaries.size() - 1));

        model.addAttribute("currentAnnual", currentSummary);
        model.addAttribute("summaries", summaries);
        model.addAttribute("totalNetIncome", salaryService.getTotalNetIncome());

        // 전년 대비 상승률 (현재 연도 기준)
        model.addAttribute("growthRate", currentSummary.getGrowthRate());

        // 차트 데이터: 월별 실수령액
        model.addAttribute("monthlyRecords", salaryService.findByYear(currentSummary.getYear()));

        return "dashboard";
    }

    @GetMapping("/annual")
    public String annual(Model model) {
        model.addAttribute("summaries", salaryService.getAnnualSummaries());
        return "annual";
    }
}
```

- [ ] **Step 4: 테스트 통과 확인**

```bash
./gradlew test --tests "com.salary.dashboard.controller.DashboardControllerSpec"
```

Expected: 3 tests PASSED

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add DashboardController with annual comparison"
```

---

## Chunk 4: Templates + CSS + Chart.js

### Task 8: 공통 레이아웃 + CSS

**Files:**
- Create: `src/main/resources/templates/layout.html`
- Create: `src/main/resources/static/css/style.css`

- [ ] **Step 1: layout.html 작성**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title layout:title-pattern="$CONTENT_TITLE - 연봉 대시보드">연봉 대시보드</title>
    <link rel="stylesheet" th:href="@{/css/style.css}">
    <script src="https://unpkg.com/htmx.org@2.0.4"></script>
    <script src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
</head>
<body hx-boost="true">
    <nav class="sidebar">
        <div class="sidebar-logo">
            <h2>💰 연봉 대시보드</h2>
        </div>
        <ul class="sidebar-menu">
            <li><a th:href="@{/}" th:classappend="${#httpServletRequest.requestURI == '/' ? 'active' : ''}">대시보드</a></li>
            <li><a th:href="@{/salary/list}" th:classappend="${#httpServletRequest.requestURI.startsWith('/salary') ? 'active' : ''}">급여 관리</a></li>
            <li><a th:href="@{/annual}" th:classappend="${#httpServletRequest.requestURI == '/annual' ? 'active' : ''}">연도별 비교</a></li>
        </ul>
    </nav>
    <main class="content">
        <div layout:fragment="content"></div>
    </main>
    <script th:src="@{/js/chart-config.js}"></script>
</body>
</html>
```

- [ ] **Step 2: style.css 작성 (토스 스타일, 네이비 + 민트)**

```css
/* === Reset & Base === */
* { margin: 0; padding: 0; box-sizing: border-box; }

:root {
    --navy: #1B2838;
    --navy-light: #243447;
    --mint: #4ECDC4;
    --mint-light: rgba(78, 205, 196, 0.1);
    --white: #FFFFFF;
    --gray-50: #F8F9FA;
    --gray-100: #F1F3F5;
    --gray-200: #E9ECEF;
    --gray-400: #ADB5BD;
    --gray-600: #868E96;
    --gray-800: #343A40;
    --coral: #FF6B6B;
    --coral-light: rgba(255, 107, 107, 0.1);
    --radius: 16px;
    --shadow: 0 2px 8px rgba(0,0,0,0.04);
    --shadow-lg: 0 4px 24px rgba(0,0,0,0.08);
}

body {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    background: var(--gray-50);
    color: var(--gray-800);
    display: flex;
    min-height: 100vh;
}

/* === Sidebar === */
.sidebar {
    width: 240px;
    background: var(--navy);
    color: var(--white);
    padding: 32px 0;
    position: fixed;
    height: 100vh;
    display: flex;
    flex-direction: column;
}

.sidebar-logo {
    padding: 0 24px 32px;
    border-bottom: 1px solid var(--navy-light);
}

.sidebar-logo h2 {
    font-size: 18px;
    font-weight: 700;
}

.sidebar-menu {
    list-style: none;
    padding: 16px 0;
}

.sidebar-menu li a {
    display: block;
    padding: 12px 24px;
    color: var(--gray-400);
    text-decoration: none;
    font-size: 15px;
    font-weight: 500;
    transition: all 0.2s;
}

.sidebar-menu li a:hover {
    color: var(--white);
    background: var(--navy-light);
}

.sidebar-menu li a.active {
    color: var(--mint);
    background: var(--navy-light);
    border-left: 3px solid var(--mint);
}

/* === Content === */
.content {
    margin-left: 240px;
    padding: 40px;
    flex: 1;
    max-width: 1200px;
}

/* === Cards === */
.card-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
    gap: 20px;
    margin-bottom: 32px;
}

.card {
    background: var(--white);
    border-radius: var(--radius);
    padding: 28px;
    box-shadow: var(--shadow);
    transition: box-shadow 0.2s;
}

.card:hover {
    box-shadow: var(--shadow-lg);
}

.card-label {
    font-size: 13px;
    font-weight: 600;
    color: var(--gray-600);
    margin-bottom: 8px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

.card-value {
    font-size: 32px;
    font-weight: 800;
    color: var(--gray-800);
    line-height: 1.2;
}

.card-sub {
    font-size: 14px;
    color: var(--gray-600);
    margin-top: 4px;
}

.card-value.up { color: var(--mint); }
.card-value.down { color: var(--coral); }

.badge-up {
    display: inline-block;
    background: var(--mint-light);
    color: var(--mint);
    padding: 4px 10px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 700;
}

.badge-down {
    display: inline-block;
    background: var(--coral-light);
    color: var(--coral);
    padding: 4px 10px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 700;
}

.badge-neutral {
    display: inline-block;
    background: var(--gray-100);
    color: var(--gray-600);
    padding: 4px 10px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 700;
}

/* === Charts === */
.chart-container {
    background: var(--white);
    border-radius: var(--radius);
    padding: 28px;
    box-shadow: var(--shadow);
    margin-bottom: 24px;
}

.chart-title {
    font-size: 16px;
    font-weight: 700;
    margin-bottom: 20px;
    color: var(--gray-800);
}

.chart-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 24px;
    margin-bottom: 24px;
}

/* === Tables === */
.table-container {
    background: var(--white);
    border-radius: var(--radius);
    padding: 28px;
    box-shadow: var(--shadow);
    overflow-x: auto;
}

table {
    width: 100%;
    border-collapse: collapse;
}

thead th {
    text-align: left;
    padding: 12px 16px;
    font-size: 13px;
    font-weight: 600;
    color: var(--gray-600);
    border-bottom: 2px solid var(--gray-200);
    text-transform: uppercase;
    letter-spacing: 0.5px;
}

tbody td {
    padding: 16px;
    font-size: 15px;
    border-bottom: 1px solid var(--gray-100);
}

tbody tr:hover {
    background: var(--gray-50);
}

/* === Forms === */
.form-container {
    background: var(--white);
    border-radius: var(--radius);
    padding: 36px;
    box-shadow: var(--shadow);
    max-width: 600px;
}

.form-group {
    margin-bottom: 24px;
}

.form-group label {
    display: block;
    font-size: 14px;
    font-weight: 600;
    color: var(--gray-800);
    margin-bottom: 8px;
}

.form-group input,
.form-group textarea,
.form-group select {
    width: 100%;
    padding: 12px 16px;
    border: 1px solid var(--gray-200);
    border-radius: 12px;
    font-size: 16px;
    transition: border-color 0.2s;
    outline: none;
}

.form-group input:focus,
.form-group textarea:focus {
    border-color: var(--mint);
    box-shadow: 0 0 0 3px var(--mint-light);
}

.form-group .error {
    color: var(--coral);
    font-size: 13px;
    margin-top: 4px;
}

.form-group .computed {
    background: var(--gray-50);
    color: var(--gray-600);
}

/* === Buttons === */
.btn {
    display: inline-block;
    padding: 12px 24px;
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.2s;
    text-decoration: none;
}

.btn-primary {
    background: var(--mint);
    color: var(--white);
}

.btn-primary:hover {
    background: #3dbdb5;
    box-shadow: 0 4px 12px rgba(78, 205, 196, 0.3);
}

.btn-secondary {
    background: var(--gray-100);
    color: var(--gray-800);
}

.btn-secondary:hover {
    background: var(--gray-200);
}

.btn-danger {
    background: var(--coral-light);
    color: var(--coral);
}

.btn-danger:hover {
    background: var(--coral);
    color: var(--white);
}

.btn-sm {
    padding: 8px 16px;
    font-size: 13px;
}

/* === Filter === */
.filter-bar {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;
    align-items: center;
}

.filter-bar select {
    padding: 8px 16px;
    border: 1px solid var(--gray-200);
    border-radius: 12px;
    font-size: 14px;
    outline: none;
}

/* === Empty State === */
.empty-state {
    text-align: center;
    padding: 80px 40px;
    background: var(--white);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
}

.empty-state h3 {
    font-size: 20px;
    color: var(--gray-800);
    margin-bottom: 8px;
}

.empty-state p {
    color: var(--gray-600);
    margin-bottom: 24px;
}

/* === Page Header === */
.page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 32px;
}

.page-header h1 {
    font-size: 28px;
    font-weight: 800;
    color: var(--gray-800);
}

/* === Modal === */
.modal-overlay {
    display: none;
    position: fixed;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5);
    z-index: 100;
    justify-content: center;
    align-items: center;
}

.modal-overlay.active {
    display: flex;
}

.modal {
    background: var(--white);
    border-radius: var(--radius);
    padding: 32px;
    max-width: 400px;
    text-align: center;
}

.modal h3 {
    margin-bottom: 8px;
}

.modal p {
    color: var(--gray-600);
    margin-bottom: 24px;
}

.modal-actions {
    display: flex;
    gap: 12px;
    justify-content: center;
}

/* === Utilities === */
.text-right { text-align: right; }
.text-center { text-align: center; }
.mt-4 { margin-top: 16px; }
.mb-4 { margin-bottom: 16px; }
.money { font-variant-numeric: tabular-nums; }
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add layout template and Toss-style CSS"
```

---

### Task 9: 대시보드 템플릿

**Files:**
- Create: `src/main/resources/templates/dashboard.html`

- [ ] **Step 1: dashboard.html 작성**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>대시보드</title>
</head>
<body>
<div layout:fragment="content">

    <!-- Empty State -->
    <div th:if="${isEmpty}" class="empty-state">
        <h3>아직 급여 데이터가 없어요</h3>
        <p>첫 급여를 입력하고 연봉 추이를 확인해보세요</p>
        <a th:href="@{/salary/new}" class="btn btn-primary">첫 급여 입력하기</a>
    </div>

    <!-- Dashboard Content -->
    <div th:unless="${isEmpty}">
        <div class="page-header">
            <h1>대시보드</h1>
        </div>

        <!-- Summary Cards -->
        <div class="card-grid">
            <div class="card">
                <div class="card-label">현재 연봉</div>
                <div class="card-value money" th:text="${#numbers.formatInteger(currentAnnual.annualGross, 0, 'COMMA')} + '원'">0원</div>
                <div class="card-sub" th:if="${currentAnnual.dataInsufficient}">
                    ⚠️ 데이터 부족 (예상: <span class="money" th:text="${#numbers.formatInteger(currentAnnual.estimatedAnnual, 0, 'COMMA')} + '원'"></span>)
                </div>
                <div class="card-sub" th:unless="${currentAnnual.dataInsufficient}" th:if="${currentAnnual.monthCount < 12}">
                    예상 연봉: <span class="money" th:text="${#numbers.formatInteger(currentAnnual.estimatedAnnual, 0, 'COMMA')} + '원'"></span>
                </div>
            </div>

            <div class="card">
                <div class="card-label">전년 대비</div>
                <div th:if="${growthRate != null}">
                    <span th:class="${growthRate >= 0 ? 'badge-up' : 'badge-down'}"
                          th:text="${growthRate >= 0 ? '+' : ''} + ${#numbers.formatDecimal(growthRate, 1, 1)} + '%'"></span>
                </div>
                <div th:unless="${growthRate != null}">
                    <span class="badge-neutral">—</span>
                </div>
            </div>

            <div class="card">
                <div class="card-label">누적 총 수입</div>
                <div class="card-value money" th:text="${#numbers.formatInteger(totalNetIncome, 0, 'COMMA')} + '원'">0원</div>
                <div class="card-sub">전체 기간 실수령 합계</div>
            </div>
        </div>

        <!-- Charts Row 1: 연봉 추이 + 월별 실수령 -->
        <div class="chart-row">
            <div class="chart-container">
                <div class="chart-title">연도별 연봉 추이</div>
                <canvas id="annualTrendChart"></canvas>
            </div>
            <div class="chart-container">
                <div class="chart-title">월별 실수령액</div>
                <canvas id="monthlyNetChart"></canvas>
            </div>
        </div>

        <!-- Charts Row 2: 세전 vs 세후 + 수입 구성 -->
        <div class="chart-row">
            <div class="chart-container">
                <div class="chart-title">세전 vs 세후 비교</div>
                <canvas id="grossVsNetChart"></canvas>
            </div>
            <div class="chart-container">
                <div class="chart-title">수입 구성 비중</div>
                <canvas id="incomeBreakdownChart"></canvas>
            </div>
        </div>
    </div>

    <!-- Chart Data -->
    <script th:unless="${isEmpty}" th:inline="javascript">
        window.dashboardData = {
            summaries: /*[[${summaries}]]*/ [],
            monthlyRecords: /*[[${monthlyRecords}]]*/ []
        };
    </script>
</div>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add dashboard template with cards and chart canvases"
```

---

### Task 10: 급여 입력/수정 폼 + 목록 템플릿

**Files:**
- Create: `src/main/resources/templates/salary/form.html`
- Create: `src/main/resources/templates/salary/list.html`

- [ ] **Step 1: salary/form.html 작성**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>급여 입력</title>
</head>
<body>
<div layout:fragment="content">
    <div class="page-header">
        <h1 th:text="${salaryForm.id != null ? '급여 수정' : '급여 입력'}">급여 입력</h1>
    </div>

    <div class="form-container">
        <form th:action="@{/salary/save}" th:object="${salaryForm}" method="post">
            <input type="hidden" th:field="*{id}">

            <div class="form-group">
                <label for="yearMonth">연월</label>
                <input type="month" id="yearMonth" th:field="*{yearMonth}">
                <div class="error" th:if="${#fields.hasErrors('yearMonth')}" th:errors="*{yearMonth}"></div>
            </div>

            <div class="form-group">
                <label for="baseSalary">기본급</label>
                <input type="number" id="baseSalary" th:field="*{baseSalary}" placeholder="0" oninput="calculateTotals()">
                <div class="error" th:if="${#fields.hasErrors('baseSalary')}" th:errors="*{baseSalary}"></div>
            </div>

            <div class="form-group">
                <label for="bonus">성과급</label>
                <input type="number" id="bonus" th:field="*{bonus}" placeholder="0" oninput="calculateTotals()">
                <div class="error" th:if="${#fields.hasErrors('bonus')}" th:errors="*{bonus}"></div>
            </div>

            <div class="form-group">
                <label for="extraIncome">영끌 (기타수당)</label>
                <input type="number" id="extraIncome" th:field="*{extraIncome}" placeholder="0" oninput="calculateTotals()">
                <div class="error" th:if="${#fields.hasErrors('extraIncome')}" th:errors="*{extraIncome}"></div>
            </div>

            <div class="form-group">
                <label for="grossDisplay">세전 총액 (자동 계산)</label>
                <input type="text" id="grossDisplay" class="computed" readonly>
            </div>

            <div class="form-group">
                <label for="totalDeduction">총 공제액</label>
                <input type="number" id="totalDeduction" th:field="*{totalDeduction}" placeholder="0" oninput="calculateTotals()">
                <div class="error" th:if="${#fields.hasErrors('totalDeduction')}" th:errors="*{totalDeduction}"></div>
            </div>

            <div class="form-group">
                <label for="netSalary">실수령액 (자동 계산, 수정 가능)</label>
                <input type="number" id="netSalary" th:field="*{netSalary}" placeholder="자동 계산">
            </div>

            <div class="form-group">
                <label for="memo">메모 (선택)</label>
                <textarea id="memo" th:field="*{memo}" rows="3" placeholder="예: 연말정산 환급 포함"></textarea>
            </div>

            <div style="display: flex; gap: 12px;">
                <button type="submit" class="btn btn-primary">저장</button>
                <a th:href="@{/salary/list}" class="btn btn-secondary">취소</a>
            </div>
        </form>
    </div>

    <script>
        function calculateTotals() {
            const base = parseInt(document.getElementById('baseSalary').value) || 0;
            const bonus = parseInt(document.getElementById('bonus').value) || 0;
            const extra = parseInt(document.getElementById('extraIncome').value) || 0;
            const deduction = parseInt(document.getElementById('totalDeduction').value) || 0;

            const gross = base + bonus + extra;
            const net = gross - deduction;

            document.getElementById('grossDisplay').value = gross.toLocaleString('ko-KR') + '원';
            document.getElementById('netSalary').value = net;
        }
        // 페이지 로드 시 초기 계산
        document.addEventListener('DOMContentLoaded', calculateTotals);
    </script>
</div>
</body>
</html>
```

- [ ] **Step 2: salary/list.html 작성**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>급여 목록</title>
</head>
<body>
<div layout:fragment="content">
    <div class="page-header">
        <h1>급여 관리</h1>
        <a th:href="@{/salary/new}" class="btn btn-primary">+ 급여 입력</a>
    </div>

    <!-- Year Filter -->
    <div class="filter-bar" th:if="${!years.isEmpty()}">
        <label>연도 필터:</label>
        <select onchange="location.href=this.value ? '/salary/list?year='+this.value : '/salary/list'">
            <option value="">전체</option>
            <option th:each="y : ${years}" th:value="${y}" th:text="${y}" th:selected="${y == selectedYear}"></option>
        </select>
    </div>

    <!-- Empty -->
    <div th:if="${records.isEmpty()}" class="empty-state">
        <h3>등록된 급여가 없습니다</h3>
        <p>급여를 입력해보세요</p>
        <a th:href="@{/salary/new}" class="btn btn-primary">급여 입력</a>
    </div>

    <!-- Table -->
    <div th:unless="${records.isEmpty()}" class="table-container">
        <table>
            <thead>
                <tr>
                    <th>연월</th>
                    <th class="text-right">기본급</th>
                    <th class="text-right">성과급</th>
                    <th class="text-right">영끌</th>
                    <th class="text-right">세전 총액</th>
                    <th class="text-right">공제액</th>
                    <th class="text-right">실수령액</th>
                    <th class="text-center">관리</th>
                </tr>
            </thead>
            <tbody>
                <tr th:each="r : ${records}">
                    <td th:text="${r.yearMonth}"></td>
                    <td class="text-right money" th:text="${#numbers.formatInteger(r.baseSalary, 0, 'COMMA')}"></td>
                    <td class="text-right money" th:text="${#numbers.formatInteger(r.bonus, 0, 'COMMA')}"></td>
                    <td class="text-right money" th:text="${#numbers.formatInteger(r.extraIncome, 0, 'COMMA')}"></td>
                    <td class="text-right money" th:text="${#numbers.formatInteger(r.grossTotal, 0, 'COMMA')}"></td>
                    <td class="text-right money" th:text="${#numbers.formatInteger(r.totalDeduction, 0, 'COMMA')}"></td>
                    <td class="text-right money" style="font-weight:700" th:text="${#numbers.formatInteger(r.netSalary, 0, 'COMMA')}"></td>
                    <td class="text-center">
                        <a th:href="@{/salary/edit/{id}(id=${r.id})}" class="btn btn-secondary btn-sm">수정</a>
                        <button class="btn btn-danger btn-sm" onclick="confirmDelete(this)" th:data-id="${r.id}">삭제</button>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>

    <!-- Delete Confirm Modal -->
    <div id="deleteModal" class="modal-overlay">
        <div class="modal">
            <h3>급여 삭제</h3>
            <p>이 기록을 삭제하시겠습니까?</p>
            <div class="modal-actions">
                <button class="btn btn-secondary" onclick="closeModal()">취소</button>
                <form id="deleteForm" method="post" style="display:inline">
                    <button type="submit" class="btn btn-danger">삭제</button>
                </form>
            </div>
        </div>
    </div>

    <script>
        function confirmDelete(btn) {
            const id = btn.dataset.id;
            document.getElementById('deleteForm').action = '/salary/delete/' + id;
            document.getElementById('deleteModal').classList.add('active');
        }
        function closeModal() {
            document.getElementById('deleteModal').classList.remove('active');
        }
    </script>
</div>
</body>
</html>
```

- [ ] **Step 3: Commit**

```bash
git add .
git commit -m "feat: add salary form and list templates"
```

---

### Task 11: 연도별 비교 템플릿

**Files:**
- Create: `src/main/resources/templates/annual.html`

- [ ] **Step 1: annual.html 작성**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layout}">
<head>
    <title>연도별 비교</title>
</head>
<body>
<div layout:fragment="content">
    <div class="page-header">
        <h1>연도별 비교</h1>
    </div>

    <div th:if="${summaries.isEmpty()}" class="empty-state">
        <h3>비교할 데이터가 없습니다</h3>
        <p>급여를 입력하면 연도별 비교를 확인할 수 있어요</p>
        <a th:href="@{/salary/new}" class="btn btn-primary">급여 입력</a>
    </div>

    <div th:unless="${summaries.isEmpty()}">
        <!-- Annual Table -->
        <div class="table-container mb-4">
            <table>
                <thead>
                    <tr>
                        <th>연도</th>
                        <th class="text-right">연봉 (세전)</th>
                        <th class="text-right">실수령 합계</th>
                        <th class="text-right">기본급</th>
                        <th class="text-right">성과급</th>
                        <th class="text-right">영끌</th>
                        <th class="text-center">전년 대비</th>
                        <th class="text-center">누적 상승률</th>
                        <th class="text-center">데이터</th>
                    </tr>
                </thead>
                <tbody>
                    <tr th:each="s : ${summaries}">
                        <td th:text="${s.year}"></td>
                        <td class="text-right money" th:text="${#numbers.formatInteger(s.annualGross, 0, 'COMMA')}"></td>
                        <td class="text-right money" th:text="${#numbers.formatInteger(s.annualNet, 0, 'COMMA')}"></td>
                        <td class="text-right money" th:text="${#numbers.formatInteger(s.annualBaseSalary, 0, 'COMMA')}"></td>
                        <td class="text-right money" th:text="${#numbers.formatInteger(s.annualBonus, 0, 'COMMA')}"></td>
                        <td class="text-right money" th:text="${#numbers.formatInteger(s.annualExtraIncome, 0, 'COMMA')}"></td>
                        <td class="text-center">
                            <span th:if="${s.growthRate != null}" th:class="${s.growthRate >= 0 ? 'badge-up' : 'badge-down'}"
                                  th:text="${(s.growthRate >= 0 ? '+' : '') + #numbers.formatDecimal(s.growthRate, 1, 1) + '%'}"></span>
                            <span th:unless="${s.growthRate != null}" class="badge-neutral">—</span>
                        </td>
                        <td class="text-center">
                            <span th:if="${s.cumulativeGrowthRate != null}" th:class="${s.cumulativeGrowthRate >= 0 ? 'badge-up' : 'badge-down'}"
                                  th:text="${(s.cumulativeGrowthRate >= 0 ? '+' : '') + #numbers.formatDecimal(s.cumulativeGrowthRate, 1, 1) + '%'}"></span>
                            <span th:unless="${s.cumulativeGrowthRate != null}" class="badge-neutral">—</span>
                        </td>
                        <td class="text-center">
                            <span th:text="${s.monthCount} + '개월'"></span>
                            <span th:if="${s.dataInsufficient}" style="color:var(--coral);font-size:12px"> ⚠️</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- Charts -->
        <div class="chart-row">
            <div class="chart-container">
                <div class="chart-title">연봉 상승률 추이</div>
                <canvas id="growthRateChart"></canvas>
            </div>
            <div class="chart-container">
                <div class="chart-title">수입 구성 변화</div>
                <canvas id="incomeCompositionChart"></canvas>
            </div>
        </div>
    </div>

    <script th:unless="${summaries.isEmpty()}" th:inline="javascript">
        window.annualData = {
            summaries: /*[[${summaries}]]*/ []
        };
    </script>
</div>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add annual comparison template with growth rate table"
```

---

### Task 12: Chart.js 설정

**Files:**
- Create: `src/main/resources/static/js/chart-config.js`

- [ ] **Step 1: chart-config.js 작성**

```javascript
// Chart.js 공통 설정
const COLORS = {
    mint: '#4ECDC4',
    mintLight: 'rgba(78, 205, 196, 0.2)',
    coral: '#FF6B6B',
    coralLight: 'rgba(255, 107, 107, 0.2)',
    navy: '#1B2838',
    gray: '#ADB5BD',
    white: '#FFFFFF'
};

const commonOptions = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
        legend: {
            labels: { font: { family: '-apple-system, sans-serif', size: 13 } }
        },
        tooltip: {
            callbacks: {
                label: function(ctx) {
                    return ctx.dataset.label + ': ' + ctx.parsed.y.toLocaleString('ko-KR') + '원';
                }
            }
        }
    },
    scales: {
        y: {
            ticks: {
                callback: function(val) {
                    if (val >= 10000) return (val / 10000).toLocaleString('ko-KR') + '만';
                    return val.toLocaleString('ko-KR');
                }
            }
        }
    }
};

function formatMoney(val) {
    return val.toLocaleString('ko-KR') + '원';
}

// ====== Dashboard Charts ======
function initDashboardCharts() {
    if (!window.dashboardData) return;
    const { summaries, monthlyRecords } = window.dashboardData;

    // 연도별 연봉 추이 (꺾은선)
    const annualCtx = document.getElementById('annualTrendChart');
    if (annualCtx) {
        new Chart(annualCtx, {
            type: 'line',
            data: {
                labels: summaries.map(s => s.year + '년'),
                datasets: [{
                    label: '연봉 (세전)',
                    data: summaries.map(s => s.annualGross),
                    borderColor: COLORS.mint,
                    backgroundColor: COLORS.mintLight,
                    fill: true,
                    tension: 0.3,
                    pointRadius: 6,
                    pointHoverRadius: 8
                }]
            },
            options: { ...commonOptions }
        });
    }

    // 월별 실수령액 (막대)
    const monthlyCtx = document.getElementById('monthlyNetChart');
    if (monthlyCtx) {
        new Chart(monthlyCtx, {
            type: 'bar',
            data: {
                labels: monthlyRecords.map(r => {
                    const ym = r.yearMonth;
                    return (typeof ym === 'string') ? ym : ym.year + '-' + String(ym.monthValue).padStart(2, '0');
                }),
                datasets: [{
                    label: '실수령액',
                    data: monthlyRecords.map(r => r.netSalary),
                    backgroundColor: COLORS.mint,
                    borderRadius: 8
                }]
            },
            options: { ...commonOptions }
        });
    }

    // 세전 vs 세후 (막대)
    const gvnCtx = document.getElementById('grossVsNetChart');
    if (gvnCtx) {
        new Chart(gvnCtx, {
            type: 'bar',
            data: {
                labels: monthlyRecords.map(r => {
                    const ym = r.yearMonth;
                    return (typeof ym === 'string') ? ym : ym.year + '-' + String(ym.monthValue).padStart(2, '0');
                }),
                datasets: [
                    {
                        label: '세전',
                        data: monthlyRecords.map(r => r.grossTotal),
                        backgroundColor: COLORS.navy,
                        borderRadius: 8
                    },
                    {
                        label: '세후',
                        data: monthlyRecords.map(r => r.netSalary),
                        backgroundColor: COLORS.mint,
                        borderRadius: 8
                    }
                ]
            },
            options: { ...commonOptions }
        });
    }

    // 수입 구성 비중 (도넛)
    const breakdownCtx = document.getElementById('incomeBreakdownChart');
    if (breakdownCtx) {
        const totalBase = monthlyRecords.reduce((sum, r) => sum + r.baseSalary, 0);
        const totalBonus = monthlyRecords.reduce((sum, r) => sum + r.bonus, 0);
        const totalExtra = monthlyRecords.reduce((sum, r) => sum + r.extraIncome, 0);

        new Chart(breakdownCtx, {
            type: 'doughnut',
            data: {
                labels: ['기본급', '성과급', '영끌'],
                datasets: [{
                    data: [totalBase, totalBonus, totalExtra],
                    backgroundColor: [COLORS.navy, COLORS.mint, COLORS.coral],
                    borderWidth: 0,
                    hoverOffset: 8
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: {
                        callbacks: {
                            label: function(ctx) {
                                const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
                                const pct = ((ctx.parsed / total) * 100).toFixed(1);
                                return ctx.label + ': ' + ctx.parsed.toLocaleString('ko-KR') + '원 (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    }
}

// ====== Annual Page Charts ======
function initAnnualCharts() {
    if (!window.annualData) return;
    const { summaries } = window.annualData;

    // 상승률 추이 (꺾은선)
    const growthCtx = document.getElementById('growthRateChart');
    if (growthCtx) {
        new Chart(growthCtx, {
            type: 'line',
            data: {
                labels: summaries.map(s => s.year + '년'),
                datasets: [
                    {
                        label: '전년 대비',
                        data: summaries.map(s => s.growthRate),
                        borderColor: COLORS.mint,
                        backgroundColor: COLORS.mintLight,
                        tension: 0.3,
                        pointRadius: 6
                    },
                    {
                        label: '누적 상승률',
                        data: summaries.map(s => s.cumulativeGrowthRate),
                        borderColor: COLORS.coral,
                        backgroundColor: COLORS.coralLight,
                        tension: 0.3,
                        pointRadius: 6
                    }
                ]
            },
            options: {
                ...commonOptions,
                scales: {
                    y: {
                        ticks: { callback: val => val + '%' }
                    }
                }
            }
        });
    }

    // 수입 구성 변화 (누적 막대)
    const compCtx = document.getElementById('incomeCompositionChart');
    if (compCtx) {
        new Chart(compCtx, {
            type: 'bar',
            data: {
                labels: summaries.map(s => s.year + '년'),
                datasets: [
                    {
                        label: '기본급',
                        data: summaries.map(s => s.annualBaseSalary),
                        backgroundColor: COLORS.navy
                    },
                    {
                        label: '성과급',
                        data: summaries.map(s => s.annualBonus),
                        backgroundColor: COLORS.mint
                    },
                    {
                        label: '영끌',
                        data: summaries.map(s => s.annualExtraIncome),
                        backgroundColor: COLORS.coral
                    }
                ]
            },
            options: {
                ...commonOptions,
                scales: {
                    x: { stacked: true },
                    y: {
                        stacked: true,
                        ticks: {
                            callback: val => (val / 10000).toLocaleString('ko-KR') + '만'
                        }
                    }
                }
            }
        });
    }
}

// ====== Init ======
document.addEventListener('DOMContentLoaded', function() {
    initDashboardCharts();
    initAnnualCharts();
});

// HTMX 연동: 페이지 전환 후 차트 재초기화
document.addEventListener('htmx:afterSwap', function() {
    initDashboardCharts();
    initAnnualCharts();
});
```

- [ ] **Step 2: Commit**

```bash
git add .
git commit -m "feat: add Chart.js configuration with dashboard and annual charts"
```

---

## Chunk 5: Integration + Final Verification

### Task 13: 전체 통합 테스트

- [ ] **Step 1: 전체 테스트 실행**

```bash
./gradlew test
```

Expected: ALL tests PASSED

- [ ] **Step 2: 애플리케이션 기동 확인**

MySQL DB 생성 (로컬에 MySQL이 있다면):

```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS salary_dashboard DEFAULT CHARACTER SET utf8mb4;"
```

```bash
./gradlew bootRun
```

Expected: 8080 포트에서 정상 기동

- [ ] **Step 3: 수동 테스트**

1. `http://localhost:8080/` → 빈 상태 안내 확인
2. "첫 급여 입력하기" 클릭 → 폼 확인
3. 급여 입력 (기본급: 2169336, 성과급: 0, 영끌: 747330, 공제: -496180) → 저장
4. 목록에서 확인 → 세전 2,916,666 / 실수령 3,412,846
5. 대시보드에서 카드, 차트 확인
6. 추가 데이터 몇 건 입력 후 연도별 비교 확인

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "chore: verify full integration"
```

---

## Summary

| Task | 내용 | 예상 파일 수 |
|------|------|-------------|
| 1 | 프로젝트 초기화 | 4 |
| 2 | SalaryRecord 엔티티 | 3 |
| 3 | Repository | 2 |
| 4 | DTO (SalaryForm, AnnualSummaryDto) | 2 |
| 5 | SalaryService | 2 |
| 6 | SalaryController | 2 |
| 7 | DashboardController | 2 |
| 8 | Layout + CSS | 2 |
| 9 | Dashboard 템플릿 | 1 |
| 10 | 급여 폼 + 목록 템플릿 | 2 |
| 11 | 연도별 비교 템플릿 | 1 |
| 12 | Chart.js 설정 | 1 |
| 13 | 통합 테스트 | 0 |
| **총** | | **24 files** |
