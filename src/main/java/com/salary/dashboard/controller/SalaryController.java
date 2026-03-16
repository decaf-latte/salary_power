package com.salary.dashboard.controller;

import com.salary.dashboard.domain.SalaryRecord;
import com.salary.dashboard.dto.SalaryForm;
import com.salary.dashboard.service.SalaryService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/salary")
public class SalaryController {

    private final SalaryService salaryService;

    public SalaryController(SalaryService salaryService) {
        this.salaryService = salaryService;
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("salaryForm", new SalaryForm());
        return "salary/form";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        SalaryRecord record = salaryService.findById(id);
        SalaryForm form = new SalaryForm();
        form.setId(record.getId());
        form.setYearMonth(record.getYearMonth().toString());
        form.setBaseSalary(record.getBaseSalary());
        form.setBonus(record.getBonus());
        form.setExtraIncome(record.getExtraIncome());
        form.setTotalDeduction(record.getTotalDeduction());
        form.setNetSalary(record.getNetSalary());
        form.setMemo(record.getMemo());
        model.addAttribute("salaryForm", form);
        return "salary/form";
    }

    @PostMapping("/save")
    public String save(@Valid @ModelAttribute SalaryForm salaryForm,
                       BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            return "salary/form";
        }
        try {
            salaryService.save(salaryForm);
            return "redirect:/salary/list";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("yearMonth", "duplicate", e.getMessage());
            return "salary/form";
        }
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) Integer year, Model model) {
        if (year != null) {
            model.addAttribute("records", salaryService.findByYear(year));
        } else {
            model.addAttribute("records", salaryService.findAll());
        }
        model.addAttribute("years", salaryService.getAllYears());
        model.addAttribute("selectedYear", year);
        return "salary/list";
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        salaryService.delete(id);
        return "redirect:/salary/list";
    }
}
