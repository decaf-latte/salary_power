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
