package com.salary.dashboard.controller;

import com.salary.dashboard.dto.AnnualSummaryDto;
import com.salary.dashboard.service.SalaryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Year;
import java.util.List;

@Controller
public class DashboardController {

    private final SalaryService salaryService;

    public DashboardController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        List<AnnualSummaryDto> summaries = salaryService.getAnnualSummaries();

        if (summaries.isEmpty()) {
            model.addAttribute("isEmpty", true);
            return "dashboard";
        }

        model.addAttribute("isEmpty", false);

        // 현재 연도 또는 가장 최근 연도의 요약
        int currentYear = Year.now().getValue();
        AnnualSummaryDto currentSummary = summaries.stream()
            .filter(s -> s.getYear() == currentYear)
            .findFirst()
            .orElse(summaries.get(summaries.size() - 1));

        model.addAttribute("currentAnnual", currentSummary);
        model.addAttribute("summaries", summaries);
        model.addAttribute("totalNetIncome", salaryService.getTotalNetIncome());

        // 전년 대비 상승률 (현재 연도 기준)
        model.addAttribute("growthRate", currentSummary.getGrowthRate());

        // 차트 데이터: 월별 실수령액
        model.addAttribute("monthlyRecords", salaryService.findByYear(currentSummary.getYear()));

        return "dashboard";
    }

    @GetMapping("/annual")
    public String annual(Model model) {
        model.addAttribute("summaries", salaryService.getAnnualSummaries());
        return "annual";
    }
}
