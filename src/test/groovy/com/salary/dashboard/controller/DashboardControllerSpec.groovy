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
