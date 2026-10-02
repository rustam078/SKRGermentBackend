package com.skr.erp.service;

import com.skr.erp.dto.response.EmployeeDetailsResponse;
import com.skr.erp.dto.response.EmployeeProductSummaryResponse;
import com.skr.erp.dto.response.EmployeeProductionHistoryResponse;
import com.skr.erp.report.ReportExcelWriter;
import com.skr.erp.report.ReportPeriod;
import com.skr.erp.report.ReportSection;
import com.skr.erp.report.ReportSupport;
import com.skr.erp.report.ReportWorkbookData;
import com.skr.erp.repository.ProductionEntryDetailRepository;
import com.skr.erp.repository.SalesOrderItemRepository;
import com.skr.erp.repository.SalesOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Builds date-range Production, Sales and Employee reports as PDF (HTML templates) and Excel. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private static final String PRODUCTION_TEMPLATE = "PRODUCTION_REPORT_TEMPLATE";
    private static final String SALES_TEMPLATE = "SALES_REPORT_TEMPLATE";
    private static final String EMPLOYEE_TEMPLATE = "EMPLOYEE_REPORT_TEMPLATE";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd-MMM");
    private static final PageRequest TOP = PageRequest.of(0, 1000);

    private final ReportSupport support;
    private final ProductionEntryDetailRepository productionDetailRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final EmployeeService employeeService;

    public String fileToken(LocalDate from, LocalDate to) {
        return ReportPeriod.of(from, to).fileToken();
    }

    // ── Production ──────────────────────────────────────
    public byte[] productionPdf(LocalDate from, LocalDate to) {
        return support.renderPdf(PRODUCTION_TEMPLATE, productionValues(ReportPeriod.of(from, to)));
    }

    public byte[] productionExcel(LocalDate from, LocalDate to) {
        ReportPeriod p = ReportPeriod.of(from, to);
        Object[] t = productionDetailRepository.aggregateProductionBetween(p.from(), p.to()).get(0);
        List<String[]> summary = List.of(
                new String[]{"Entries", support.num(asLong(t[0]))},
                new String[]{"Total pieces", support.num(asLong(t[1]))},
                new String[]{"Total payment", support.money(asBig(t[2]))});
        List<ReportSection> sections = List.of(
                new ReportSection("By product", List.of("Product", "Pieces", "Payment"),
                        rows3(productionDetailRepository.productionByProductBetween(p.from(), p.to()))),
                new ReportSection("By employee", List.of("Employee", "Pieces", "Payment"),
                        rows3(productionDetailRepository.topEmployeesBetween(p.from(), p.to(), TOP))));
        return ReportExcelWriter.build(workbook("Production report", p, summary, sections));
    }

    private Map<String, String> productionValues(ReportPeriod p) {
        Object[] t = productionDetailRepository.aggregateProductionBetween(p.from(), p.to()).get(0);
        Map<String, String> v = base(p);
        v.put("totalEntries", support.num(asLong(t[0])));
        v.put("totalQuantity", support.num(asLong(t[1])));
        v.put("totalAmount", support.money(asBig(t[2])));
        v.put("productRows", htmlRows3(productionDetailRepository.productionByProductBetween(p.from(), p.to())));
        v.put("employeeRows", htmlRows3(productionDetailRepository.topEmployeesBetween(p.from(), p.to(), TOP)));
        return v;
    }

    // ── Sales ───────────────────────────────────────────
    public byte[] salesPdf(LocalDate from, LocalDate to) {
        return support.renderPdf(SALES_TEMPLATE, salesValues(ReportPeriod.of(from, to)));
    }

    public byte[] salesExcel(LocalDate from, LocalDate to) {
        ReportPeriod p = ReportPeriod.of(from, to);
        Object[] t = salesOrderRepository.reportSummaryBetween(p.start(), p.endExclusive()).get(0);
        BigDecimal profit = salesOrderItemRepository.grossProfitBetween(p.start(), p.endExclusive());
        List<String[]> summary = List.of(
                new String[]{"Sales", support.num(asLong(t[0]))},
                new String[]{"Revenue", support.money(asBig(t[4]))},
                new String[]{"Discount", support.money(asBig(t[2]))},
                new String[]{"GST", support.money(asBig(t[3]))},
                new String[]{"Profit", support.money(profit)});
        List<ReportSection> sections = List.of(
                new ReportSection("By product", List.of("Product", "Qty sold", "Revenue", "Profit"),
                        rows4(salesOrderItemRepository.salesByProductBetween(p.start(), p.endExclusive(), TOP))));
        return ReportExcelWriter.build(workbook("Sales report", p, summary, sections));
    }

    private Map<String, String> salesValues(ReportPeriod p) {
        Object[] t = salesOrderRepository.reportSummaryBetween(p.start(), p.endExclusive()).get(0);
        Map<String, String> v = base(p);
        v.put("totalSales", support.num(asLong(t[0])));
        v.put("totalRevenue", support.money(asBig(t[4])));
        v.put("totalDiscount", support.money(asBig(t[2])));
        v.put("totalTax", support.money(asBig(t[3])));
        v.put("totalProfit", support.money(salesOrderItemRepository.grossProfitBetween(p.start(), p.endExclusive())));
        v.put("productRows", htmlRows4(salesOrderItemRepository.salesByProductBetween(p.start(), p.endExclusive(), TOP)));
        return v;
    }

    // ── Employee ────────────────────────────────────────
    public byte[] employeePdf(UUID employeeId, LocalDate from, LocalDate to) {
        ReportPeriod p = ReportPeriod.of(from, to);
        return support.renderPdf(EMPLOYEE_TEMPLATE, employeeValues(employeeId, p));
    }

    public byte[] employeeExcel(UUID employeeId, LocalDate from, LocalDate to) {
        ReportPeriod p = ReportPeriod.of(from, to);
        EmployeeDetailsResponse d = employeeService.getDetails(employeeId, p.from(), p.to());
        List<String[]> summary = List.of(
                new String[]{"Employee", d.getEmployee().getFullName()},
                new String[]{"Total pieces", support.num(d.getSummary().getTotalProductionQty())},
                new String[]{"Total payment", support.money(d.getSummary().getTotalEarnings())},
                new String[]{"Products", support.num(d.getSummary().getProductsWorkedOn())});
        List<ReportSection> sections = List.of(
                new ReportSection("By product", List.of("Product", "Pieces", "Payment"), employeeProductRows(d.getProductSummary())),
                new ReportSection("Daily production", List.of("Date", "Product", "Pieces", "Rate", "Payment"), employeeHistoryRows(d.getProductionHistory())));
        return ReportExcelWriter.build(workbook("Employee report", p, summary, sections));
    }

    private Map<String, String> employeeValues(UUID employeeId, ReportPeriod p) {
        EmployeeDetailsResponse d = employeeService.getDetails(employeeId, p.from(), p.to());
        Map<String, String> v = base(p);
        v.put("employeeName", support.esc(d.getEmployee().getFullName()));
        v.put("employeeCode", support.esc(d.getEmployee().getEmployeeCode() == null ? "-" : d.getEmployee().getEmployeeCode()));
        v.put("totalQuantity", support.num(d.getSummary().getTotalProductionQty()));
        v.put("totalEarnings", support.money(d.getSummary().getTotalEarnings()));
        v.put("productsWorkedOn", support.num(d.getSummary().getProductsWorkedOn()));
        v.put("productRows", htmlEmployeeProducts(d.getProductSummary()));
        v.put("historyRows", htmlEmployeeHistory(d.getProductionHistory()));
        return v;
    }

    // ── Shared builders ─────────────────────────────────
    private Map<String, String> base(ReportPeriod p) {
        Map<String, String> v = new HashMap<>();
        v.put("fromDate", p.fromLabel());
        v.put("toDate", p.toLabel());
        return v;
    }

    private ReportWorkbookData workbook(String title, ReportPeriod p, List<String[]> summary, List<ReportSection> sections) {
        return new ReportWorkbookData(title, p.label(), support.companyName(), support.companyLine(), summary, sections);
    }

    private String htmlRows3(List<Object[]> rows) {
        if (rows.isEmpty()) return emptyRow(3);
        StringBuilder sb = new StringBuilder();
        for (Object[] r : rows) {
            sb.append("<tr><td>").append(support.esc(str(r[0]))).append("</td>")
                    .append("<td class=\"num\">").append(support.num(asLong(r[1]))).append("</td>")
                    .append("<td class=\"num\">").append(support.money(asBig(r[2]))).append("</td></tr>");
        }
        return sb.toString();
    }

    private String htmlRows4(List<Object[]> rows) {
        if (rows.isEmpty()) return emptyRow(4);
        StringBuilder sb = new StringBuilder();
        for (Object[] r : rows) {
            sb.append("<tr><td>").append(support.esc(str(r[0]))).append("</td>")
                    .append("<td class=\"num\">").append(support.num(asLong(r[1]))).append("</td>")
                    .append("<td class=\"num\">").append(support.money(asBig(r[2]))).append("</td>")
                    .append("<td class=\"num\">").append(support.money(asBig(r[3]))).append("</td></tr>");
        }
        return sb.toString();
    }

    private String htmlEmployeeProducts(List<EmployeeProductSummaryResponse> list) {
        if (list == null || list.isEmpty()) return emptyRow(3);
        StringBuilder sb = new StringBuilder();
        for (EmployeeProductSummaryResponse x : list) {
            sb.append("<tr><td>").append(support.esc(x.getProductName())).append("</td>")
                    .append("<td class=\"num\">").append(support.num(x.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(support.money(x.getEarnings())).append("</td></tr>");
        }
        return sb.toString();
    }

    private String htmlEmployeeHistory(List<EmployeeProductionHistoryResponse> list) {
        if (list == null || list.isEmpty()) return emptyRow(5);
        StringBuilder sb = new StringBuilder();
        for (EmployeeProductionHistoryResponse x : list) {
            sb.append("<tr><td>").append(x.getProductionDate() == null ? "-" : x.getProductionDate().format(DAY)).append("</td>")
                    .append("<td>").append(support.esc(x.getProductName())).append("</td>")
                    .append("<td class=\"num\">").append(support.num(x.getQuantity())).append("</td>")
                    .append("<td class=\"num\">").append(support.money(x.getRate())).append("</td>")
                    .append("<td class=\"num\">").append(support.money(x.getEarnings())).append("</td></tr>");
        }
        return sb.toString();
    }

    private List<List<String>> rows3(List<Object[]> rows) {
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(List.of(str(r[0]), support.num(asLong(r[1])), support.money(asBig(r[2]))));
        }
        return out;
    }

    private List<List<String>> rows4(List<Object[]> rows) {
        List<List<String>> out = new ArrayList<>();
        for (Object[] r : rows) {
            out.add(List.of(str(r[0]), support.num(asLong(r[1])), support.money(asBig(r[2])), support.money(asBig(r[3]))));
        }
        return out;
    }

    private List<List<String>> employeeProductRows(List<EmployeeProductSummaryResponse> list) {
        List<List<String>> out = new ArrayList<>();
        if (list != null) {
            for (EmployeeProductSummaryResponse x : list) {
                out.add(List.of(nz(x.getProductName()), support.num(x.getQuantity()), support.money(x.getEarnings())));
            }
        }
        return out;
    }

    private List<List<String>> employeeHistoryRows(List<EmployeeProductionHistoryResponse> list) {
        List<List<String>> out = new ArrayList<>();
        if (list != null) {
            for (EmployeeProductionHistoryResponse x : list) {
                out.add(List.of(x.getProductionDate() == null ? "-" : x.getProductionDate().format(DAY),
                        nz(x.getProductName()), support.num(x.getQuantity()), support.money(x.getRate()), support.money(x.getEarnings())));
            }
        }
        return out;
    }

    private String emptyRow(int cols) {
        return "<tr><td colspan=\"" + cols + "\">No data for this period</td></tr>";
    }

    private long asLong(Object o) {
        return o == null ? 0L : ((Number) o).longValue();
    }

    private BigDecimal asBig(Object o) {
        if (o == null) return BigDecimal.ZERO;
        return o instanceof BigDecimal b ? b : new BigDecimal(o.toString());
    }

    private String str(Object o) {
        return o == null ? "-" : o.toString();
    }

    private String nz(String s) {
        return s == null ? "-" : s;
    }
}
