package com.salary.dashboard.domain

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.YearMonth

class SalaryRecordSpec extends Specification {

    @Subject
    SalaryRecord record = new SalaryRecord()

    @Unroll
    def "grossTotal은 기본급(#base) + 성과급(#bonus) + 기타수당(#extra) = #expected"() {
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
