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
