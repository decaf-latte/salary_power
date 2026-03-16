package com.salary.dashboard.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "salary_record", indexes = {
    @Index(name = "idx_year", columnList = "salary_year")
})
public class SalaryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM")
    private YearMonth yearMonth;

    @Column(name = "salary_year", nullable = false)
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
