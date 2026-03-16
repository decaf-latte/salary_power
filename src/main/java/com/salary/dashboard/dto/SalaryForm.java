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
