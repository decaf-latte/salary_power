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
