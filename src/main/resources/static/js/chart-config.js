// Chart.js 공통 설정
const COLORS = {
    mint: '#4ECDC4',
    mintLight: 'rgba(78, 205, 196, 0.2)',
    coral: '#FF6B6B',
    coralLight: 'rgba(255, 107, 107, 0.2)',
    navy: '#1B2838',
    gray: '#ADB5BD',
    white: '#FFFFFF'
};

const commonOptions = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
        legend: {
            labels: { font: { family: '-apple-system, sans-serif', size: 13 } }
        },
        tooltip: {
            callbacks: {
                label: function(ctx) {
                    return ctx.dataset.label + ': ' + ctx.parsed.y.toLocaleString('ko-KR') + '원';
                }
            }
        }
    },
    scales: {
        y: {
            ticks: {
                callback: function(val) {
                    if (val >= 10000) return (val / 10000).toLocaleString('ko-KR') + '만';
                    return val.toLocaleString('ko-KR');
                }
            }
        }
    }
};

function formatMoney(val) {
    return val.toLocaleString('ko-KR') + '원';
}

// ====== Dashboard Charts ======
function initDashboardCharts() {
    if (!window.dashboardData) return;
    const { summaries, monthlyRecords } = window.dashboardData;

    // 연도별 연봉 추이 (꺾은선)
    const annualCtx = document.getElementById('annualTrendChart');
    if (annualCtx) {
        new Chart(annualCtx, {
            type: 'line',
            data: {
                labels: summaries.map(s => s.year + '년'),
                datasets: [{
                    label: '연봉 (세전)',
                    data: summaries.map(s => s.annualGross),
                    borderColor: COLORS.mint,
                    backgroundColor: COLORS.mintLight,
                    fill: true,
                    tension: 0.3,
                    pointRadius: 6,
                    pointHoverRadius: 8
                }]
            },
            options: { ...commonOptions }
        });
    }

    // 월별 실수령액 (막대)
    const monthlyCtx = document.getElementById('monthlyNetChart');
    if (monthlyCtx) {
        new Chart(monthlyCtx, {
            type: 'bar',
            data: {
                labels: monthlyRecords.map(r => {
                    const ym = r.yearMonth;
                    return (typeof ym === 'string') ? ym : ym.year + '-' + String(ym.monthValue).padStart(2, '0');
                }),
                datasets: [{
                    label: '실수령액',
                    data: monthlyRecords.map(r => r.netSalary),
                    backgroundColor: COLORS.mint,
                    borderRadius: 8
                }]
            },
            options: { ...commonOptions }
        });
    }

    // 세전 vs 세후 (막대)
    const gvnCtx = document.getElementById('grossVsNetChart');
    if (gvnCtx) {
        new Chart(gvnCtx, {
            type: 'bar',
            data: {
                labels: monthlyRecords.map(r => {
                    const ym = r.yearMonth;
                    return (typeof ym === 'string') ? ym : ym.year + '-' + String(ym.monthValue).padStart(2, '0');
                }),
                datasets: [
                    {
                        label: '세전',
                        data: monthlyRecords.map(r => r.grossTotal),
                        backgroundColor: COLORS.navy,
                        borderRadius: 8
                    },
                    {
                        label: '세후',
                        data: monthlyRecords.map(r => r.netSalary),
                        backgroundColor: COLORS.mint,
                        borderRadius: 8
                    }
                ]
            },
            options: { ...commonOptions }
        });
    }

    // 수입 구성 비중 (도넛)
    const breakdownCtx = document.getElementById('incomeBreakdownChart');
    if (breakdownCtx) {
        const totalBase = monthlyRecords.reduce((sum, r) => sum + r.baseSalary, 0);
        const totalBonus = monthlyRecords.reduce((sum, r) => sum + r.bonus, 0);
        const totalExtra = monthlyRecords.reduce((sum, r) => sum + r.extraIncome, 0);

        new Chart(breakdownCtx, {
            type: 'doughnut',
            data: {
                labels: ['기본급', '성과급', '영끌'],
                datasets: [{
                    data: [totalBase, totalBonus, totalExtra],
                    backgroundColor: [COLORS.navy, COLORS.mint, COLORS.coral],
                    borderWidth: 0,
                    hoverOffset: 8
                }]
            },
            options: {
                responsive: true,
                plugins: {
                    legend: { position: 'bottom' },
                    tooltip: {
                        callbacks: {
                            label: function(ctx) {
                                const total = ctx.dataset.data.reduce((a, b) => a + b, 0);
                                const pct = ((ctx.parsed / total) * 100).toFixed(1);
                                return ctx.label + ': ' + ctx.parsed.toLocaleString('ko-KR') + '원 (' + pct + '%)';
                            }
                        }
                    }
                }
            }
        });
    }
}

// ====== Annual Page Charts ======
function initAnnualCharts() {
    if (!window.annualData) return;
    const { summaries } = window.annualData;

    // 상승률 추이 (꺾은선)
    const growthCtx = document.getElementById('growthRateChart');
    if (growthCtx) {
        new Chart(growthCtx, {
            type: 'line',
            data: {
                labels: summaries.map(s => s.year + '년'),
                datasets: [
                    {
                        label: '전년 대비',
                        data: summaries.map(s => s.growthRate),
                        borderColor: COLORS.mint,
                        backgroundColor: COLORS.mintLight,
                        tension: 0.3,
                        pointRadius: 6
                    },
                    {
                        label: '누적 상승률',
                        data: summaries.map(s => s.cumulativeGrowthRate),
                        borderColor: COLORS.coral,
                        backgroundColor: COLORS.coralLight,
                        tension: 0.3,
                        pointRadius: 6
                    }
                ]
            },
            options: {
                ...commonOptions,
                scales: {
                    y: {
                        ticks: { callback: val => val + '%' }
                    }
                }
            }
        });
    }

    // 수입 구성 변화 (누적 막대)
    const compCtx = document.getElementById('incomeCompositionChart');
    if (compCtx) {
        new Chart(compCtx, {
            type: 'bar',
            data: {
                labels: summaries.map(s => s.year + '년'),
                datasets: [
                    {
                        label: '기본급',
                        data: summaries.map(s => s.annualBaseSalary),
                        backgroundColor: COLORS.navy
                    },
                    {
                        label: '성과급',
                        data: summaries.map(s => s.annualBonus),
                        backgroundColor: COLORS.mint
                    },
                    {
                        label: '영끌',
                        data: summaries.map(s => s.annualExtraIncome),
                        backgroundColor: COLORS.coral
                    }
                ]
            },
            options: {
                ...commonOptions,
                scales: {
                    x: { stacked: true },
                    y: {
                        stacked: true,
                        ticks: {
                            callback: val => (val / 10000).toLocaleString('ko-KR') + '만'
                        }
                    }
                }
            }
        });
    }
}

// ====== Init ======
document.addEventListener('DOMContentLoaded', function() {
    initDashboardCharts();
    initAnnualCharts();
});

// HTMX 연동: 페이지 전환 후 차트 재초기화
document.addEventListener('htmx:afterSwap', function() {
    initDashboardCharts();
    initAnnualCharts();
});
