package com.salary.dashboard.dto;

public class AnnualSummaryDto {

    private Integer year;
    private Long annualGross;        // 연봉 (grossTotal 합계)
    private Long estimatedAnnual;    // 예상 연봉 (진행 중인 해)
    private Long annualNet;          // 연간 실수령 합계
    private Long annualBaseSalary;   // 연간 기본급 합계
    private Long annualBonus;        // 연간 성과급 합계
    private Long annualExtraIncome;  // 연간 기타 수당 합계
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
