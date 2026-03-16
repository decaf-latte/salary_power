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
