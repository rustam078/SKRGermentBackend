package com.skr.erp.service;

import com.skr.erp.dto.response.DashboardResponse;
import com.skr.erp.entity.SalesOrder;
import com.skr.erp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final ProductionEntryDetailRepository productionEntryDetailRepository;
    private final InvestmentRepository investmentRepository;
    private final InventoryBatchRepository inventoryBatchRepository;
    private final EmployeeRepository employeeRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(LocalDate fromDate, LocalDate toDate) {

        // Default to the trailing 30 days when no range is supplied.
        LocalDate to = (toDate != null) ? toDate : LocalDate.now();
        LocalDate from = (fromDate != null) ? fromDate : to.minusDays(29);
        if (from.isAfter(to)) {
            LocalDate tmp = from;
            from = to;
            to = tmp;
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        // ── Sales ────────────────────────────────────────
        Object[] salesAgg = first(salesOrderRepository.aggregateSalesBetween(start, end));
        long salesCount = lng(salesAgg[0]);
        BigDecimal salesRevenue = bd(salesAgg[1]);
        BigDecimal salesDiscount = bd(salesAgg[2]);
        BigDecimal avgOrderValue = salesCount > 0
                ? salesRevenue.divide(BigDecimal.valueOf(salesCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── Production ───────────────────────────────────
        Object[] prodAgg = first(productionEntryDetailRepository.aggregateProductionBetween(from, to));
        long productionEntries = lng(prodAgg[0]);
        long productionQty = lng(prodAgg[1]);
        BigDecimal productionAmount = bd(prodAgg[2]);

        // ── Investment / expenses ────────────────────────
        Object[] invAgg = first(investmentRepository.aggregateInvestmentBetween(from, to));
        long investmentCount = lng(invAgg[0]);
        BigDecimal investmentTotal = bd(invAgg[1]);

        // ── Inventory snapshot ───────────────────────────
        Object[] stockAgg = first(inventoryBatchRepository.aggregateActiveInventory());
        BigDecimal inventoryUnits = bd(stockAgg[0]);
        BigDecimal inventoryStockValue = bd(stockAgg[1]);
        long lowStockCount = inventoryService.getLowStockAlerts().size();

        // ── Workforce snapshot ───────────────────────────
        long activeEmployees = employeeRepository.countByActiveTrue();
        long totalEmployees = employeeRepository.count();

        BigDecimal netProfit = salesRevenue
                .subtract(productionAmount)
                .subtract(investmentTotal);

        // Gross profit on sales = revenue - cost of goods sold over the range.
        BigDecimal grossProfit = bd(salesOrderItemRepository.grossProfitBetween(start, end));

        // ── Revenue trend ────────────────────────────────
        List<DashboardResponse.SeriesPoint> revenueSeries =
                salesOrderRepository.revenueSeriesBetween(start, end).stream()
                        .map(r -> DashboardResponse.SeriesPoint.builder()
                                .date(dateStr(r[0]))
                                .revenue(bd(r[1]))
                                .orders(lng(r[2]))
                                .build())
                        .toList();

        // ── Payment mode breakdown ───────────────────────
        List<DashboardResponse.Breakdown> paymentBreakdown =
                salesOrderRepository.paymentBreakdownBetween(start, end).stream()
                        .map(r -> DashboardResponse.Breakdown.builder()
                                .label(r[0] != null ? r[0].toString() : "UNKNOWN")
                                .count(lng(r[1]))
                                .amount(bd(r[2]))
                                .build())
                        .toList();

        // ── Top products ─────────────────────────────────
        List<DashboardResponse.TopProduct> topProducts =
                salesOrderItemRepository.topProductsBetween(start, end, PageRequest.of(0, 6)).stream()
                        .map(r -> DashboardResponse.TopProduct.builder()
                                .name(r[0] != null ? r[0].toString() : "Unknown")
                                .quantity(lng(r[1]))
                                .revenue(bd(r[2]))
                                .build())
                        .toList();

        // ── Top employees by production (piece-rate output) ──
        List<DashboardResponse.TopEmployee> topEmployees =
                productionEntryDetailRepository.topEmployeesBetween(from, to, PageRequest.of(0, 6)).stream()
                        .map(r -> DashboardResponse.TopEmployee.builder()
                                .name(r[0] != null ? r[0].toString() : "Unknown")
                                .quantity(lng(r[1]))
                                .earnings(bd(r[2]))
                                .build())
                        .toList();

        // ── Recent sales ─────────────────────────────────
        List<DashboardResponse.RecentSale> recentSales =
                salesOrderRepository.findRecent(PageRequest.of(0, 8)).stream()
                        .map(this::toRecentSale)
                        .toList();

        return DashboardResponse.builder()
                .fromDate(from.toString())
                .toDate(to.toString())
                .salesCount(salesCount)
                .salesRevenue(salesRevenue)
                .salesDiscount(salesDiscount)
                .avgOrderValue(avgOrderValue)
                .productionEntries(productionEntries)
                .productionQty(productionQty)
                .productionAmount(productionAmount)
                .investmentCount(investmentCount)
                .investmentTotal(investmentTotal)
                .inventoryStockValue(inventoryStockValue)
                .inventoryUnits(inventoryUnits)
                .lowStockCount(lowStockCount)
                .activeEmployees(activeEmployees)
                .totalEmployees(totalEmployees)
                .netProfit(netProfit)
                .grossProfit(grossProfit)
                .revenueSeries(revenueSeries)
                .paymentBreakdown(paymentBreakdown)
                .topProducts(topProducts)
                .topEmployees(topEmployees)
                .recentSales(recentSales)
                .build();
    }

    private DashboardResponse.RecentSale toRecentSale(SalesOrder s) {
        return DashboardResponse.RecentSale.builder()
                .invoiceNo(s.getInvoiceNo())
                .customerName(s.getCustomerName())
                .amount(bd(s.getGrandTotal()))
                .paymentStatus(s.getPaymentStatus() != null ? s.getPaymentStatus().name() : null)
                .date(s.getCreatedAt() != null ? s.getCreatedAt().toLocalDate().toString() : null)
                .build();
    }

    // ── Null-safe parsing helpers for JPQL Object[] rows ─────
    private static Object[] first(List<Object[]> rows) {
        return (rows == null || rows.isEmpty())
                ? new Object[]{0L, BigDecimal.ZERO, BigDecimal.ZERO}
                : rows.get(0);
    }

    private static BigDecimal bd(Object o) {
        if (o == null) return BigDecimal.ZERO;
        if (o instanceof BigDecimal b) return b;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }

    private static long lng(Object o) {
        return (o instanceof Number n) ? n.longValue() : 0L;
    }

    private static String dateStr(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate d) return d.toString();
        if (o instanceof java.sql.Date d) return d.toLocalDate().toString();
        if (o instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate().toString();
        return o.toString();
    }
}
