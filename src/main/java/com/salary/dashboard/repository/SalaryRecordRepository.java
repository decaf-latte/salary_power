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
