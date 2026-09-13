package com.skr.erp.service;

import com.skr.erp.common.constants.InvestmentItemType;
import com.skr.erp.common.constants.InvestmentType;
import com.skr.erp.common.constants.PaymentMode;
import com.skr.erp.common.constants.PaymentStatus;
import com.skr.erp.common.constants.ProductSource;
import com.skr.erp.dto.request.AddPaymentRequest;
import com.skr.erp.dto.request.CreateInvestmentItemRequest;
import com.skr.erp.dto.request.CreateInvestmentRequest;
import com.skr.erp.dto.response.InvestmentDetailsResponse;
import com.skr.erp.dto.response.InvestmentPaymentResponse;
import com.skr.erp.dto.response.InvestmentResponse;
import com.skr.erp.entity.Investment;
import com.skr.erp.entity.InvestmentItem;
import com.skr.erp.entity.InvestmentPayment;
import com.skr.erp.entity.Product;
import com.skr.erp.entity.SystemSetting;
import com.skr.erp.entity.Vendor;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.pdf.HtmlToPdfGenerator;
import com.skr.erp.repository.InvestmentItemRepository;
import com.skr.erp.repository.InvestmentPaymentRepository;
import com.skr.erp.repository.InvestmentRepository;
import com.skr.erp.repository.ProductRepository;
import com.skr.erp.repository.SystemSettingRepository;
import com.skr.erp.repository.VendorRepository;

import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.skr.erp.dto.response.InvestmentItemResponse;
import java.util.Comparator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class InvestmentServiceImpl
        implements InvestmentService {

    private final InvestmentRepository investmentRepository;

    private final InvestmentItemRepository investmentItemRepository;

    private final VendorRepository vendorRepository;

    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final InvestmentPaymentRepository investmentPaymentRepository;
    private final SystemSettingRepository systemSettingRepository;

    private static final String PURCHASE_INVOICE_TEMPLATE_KEY = "PURCHASE_INVOICE_TEMPLATE";
    private static final java.time.format.DateTimeFormatter DTF_DATE =
            java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy");


    @Override
    public InvestmentResponse create(
            CreateInvestmentRequest request) {

        validateRequest(request);

        Investment investment = new Investment();


        if (request.getInvoiceNumber() != null && !request.getInvoiceNumber().isBlank()) {
            investment.setInvoiceNumber(request.getInvoiceNumber());

        } else {
            investment.setInvoiceNumber(generateInvoiceNumber());
        }
        investment.setInvestmentType(request.getInvestmentType());

        investment.setPurchaseDate(
                request.getPurchaseDate());

        investment.setGstAmount(
                request.getGstAmount());

        investment.setDiscountAmount(
                request.getDiscountAmount());

        investment.setOtherCharge(
                request.getOtherCharge());

        if (request.getVendorId() != null && request.getInvestmentType() != InvestmentType.OVERHEAD) {

            Vendor vendor =
                    vendorRepository
                            .findById(
                                    request.getVendorId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Vendor not found"));

            investment.setVendor(vendor);
        }

        List<InvestmentItem> items =
                buildInvestmentItems(
                        investment,
                        request.getItems());

        investment.setItems(items);

        calculateTotals(investment);

        investment = investmentRepository.save(investment);

        createInventoryBatches(investment);

        recordCreationPayment(investment, request);

        return toResponse(investment);
    }

    // Optional payment captured on the Create Investment form.
    private void recordCreationPayment(Investment investment, CreateInvestmentRequest request) {
        BigDecimal amount = null;
        if (Boolean.TRUE.equals(request.getFullPayment())) {
            amount = investment.getGrandTotal();
        } else if (request.getPaymentAmount() != null
                && request.getPaymentAmount().compareTo(BigDecimal.ZERO) > 0) {
            amount = request.getPaymentAmount();
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        LocalDate date = request.getPaymentDate() != null
                ? request.getPaymentDate()
                : investment.getPurchaseDate();
        PaymentMode mode = request.getPaymentMode() != null
                ? request.getPaymentMode()
                : PaymentMode.CASH;
        recordPayment(investment, date, mode, amount);
    }

    private void createInventoryBatches(
            Investment investment) {

        for (InvestmentItem item : investment.getItems()) {

            if (item.getItemType() != InvestmentItemType.PRODUCT) {
                continue;
            }
            inventoryService.createPurchaseBatch(item);
        }
    }

    private void validateRequest(
            CreateInvestmentRequest request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {

            throw new BusinessException(
                    "At least one item is required");
        }

        if (request.getInvestmentType()
                != InvestmentType.OVERHEAD
                && request.getVendorId() == null) {

            throw new BusinessException("Vendor is required");
        }

        if (request.getInvestmentType()
                == InvestmentType.OVERHEAD
                && request.getVendorId() != null) {

            throw new BusinessException("Vendor should not be selected for overhead");
        }

        for (CreateInvestmentItemRequest item : request.getItems()) {

            if (item.getQuantity()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new BusinessException("Quantity must be greater than zero");
            }

            if (item.getRate().compareTo(BigDecimal.ZERO) < 0) {

                throw new BusinessException("Rate cannot be negative");
            }

            if (request.getInvestmentType() == InvestmentType.OVERHEAD) {
                item.setItemType(InvestmentItemType.OVERHEAD);
            }

        }
    }

    private String generateInvoiceNumber() {

        long count =
                investmentRepository.count() + 1;

        return String.format(
                "INV-%06d",
                count
        );
    }

    private List<InvestmentItem> buildInvestmentItems(
            Investment investment,
            List<CreateInvestmentItemRequest> requests) {

        List<InvestmentItem> items =
                new ArrayList<>();

        for (CreateInvestmentItemRequest request : requests) {

            InvestmentItem item =
                    new InvestmentItem();

            item.setInvestment(investment);

            item.setItemType(
                    request.getItemType());

            item.setItemName(
                    request.getItemName());

            item.setQuantity(
                    request.getQuantity());

            item.setUnit(
                    request.getUnit());

            item.setRate(
                    request.getRate());

            item.setTotalAmount(
                    request.getQuantity()
                            .multiply(request.getRate()));

            if (request.getItemType() == InvestmentItemType.PRODUCT) {

                Product product =
                        createOrGetProduct(request);

                item.setProduct(product);
            }

            items.add(item);
        }

        return items;
    }

    private Product createOrGetProduct(
            CreateInvestmentItemRequest request) {

        Product product =
                productRepository
                        .findByNameIgnoreCase(
                                request.getItemName())
                        .orElse(null);

        if (product != null) {
            updateProductSource(product);
            return product;
        }

        product = new Product();

        product.setName(
                request.getItemName());

        product.setDescription(null);

        product.setIconName(null);

        product.setActive(true);

        product.setSource(ProductSource.PURCHASED);

        return productRepository.save(product);
    }

    private void updateProductSource(Product product) {

        if (product.getSource() == ProductSource.MANUFACTURED) {
            product.setSource(ProductSource.BOTH);
            productRepository.save(product);
        }
    }

    private void calculateTotals(
            Investment investment) {

        BigDecimal subTotal =
                investment.getItems()
                        .stream()
                        .map(InvestmentItem::getTotalAmount)
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add);

        investment.setSubTotal(
                subTotal);

        BigDecimal grandTotal =
                subTotal
                        .add(investment.getGstAmount())
                        .add(investment.getOtherCharge())
                        .subtract(investment.getDiscountAmount());

        investment.setGrandTotal(
                grandTotal);
    }

//    @Override
//    @Transactional(readOnly = true)
//    public List<InvestmentResponse> search(
//            LocalDate fromDate,
//            LocalDate toDate,
//            UUID vendorId) {
//
//        return investmentRepository
//                .findAll()
//                .stream()
//                .filter(i -> {
//
//                    if (fromDate != null &&
//                            i.getPurchaseDate().isBefore(fromDate)) {
//
//                        return false;
//                    }
//
//                    if (toDate != null &&
//                            i.getPurchaseDate().isAfter(toDate)) {
//
//                        return false;
//                    }
//
//                    if (vendorId != null) {
//
//                        if (i.getVendor() == null) {
//                            return false;
//                        }
//
//                        return i.getVendor()
//                                .getId()
//                                .equals(vendorId);
//                    }
//
//                    return true;
//                })
//                .sorted(
//                        Comparator.comparing(
//                                        Investment::getPurchaseDate)
//                                .reversed())
//                .map(this::toResponse)
//                .toList();
//    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentResponse> search(
            UUID vendorId,
            LocalDate fromDate,
            LocalDate toDate,
            InvestmentType type) {

        Specification<Investment> specification =
                (root, query, cb) -> {

                    List<Predicate> predicates = new ArrayList<>();

                    if(type!=null){
                        predicates.add(
                                cb.equal(
                                        root.get("investmentType"),
                                        type));
                    }

                    if (vendorId != null) {

                        predicates.add(
                                cb.equal(
                                        root.get("vendor").get("id"),
                                        vendorId));
                    }

                    if (fromDate != null) {

                        predicates.add(
                                cb.greaterThanOrEqualTo(
                                        root.get("purchaseDate"),
                                        fromDate));
                    }

                    if (toDate != null) {

                        predicates.add(
                                cb.lessThanOrEqualTo(
                                        root.get("purchaseDate"),
                                        toDate));
                    }

                    query.orderBy(
                            cb.desc(root.get("purchaseDate")));

                    return cb.and(
                            predicates.toArray(new Predicate[0]));
                };

        // One query for total paid per investment → avoids N+1 in the list.
        Map<UUID, BigDecimal> paidByInvestment = new HashMap<>();
        for (Object[] row : investmentPaymentRepository.paidSums()) {
            paidByInvestment.put((UUID) row[0], (BigDecimal) row[1]);
        }

        return investmentRepository
                .findAll(specification)
                .stream()
                .map(inv -> toResponse(inv, paidByInvestment.getOrDefault(inv.getId(), BigDecimal.ZERO)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvestmentDetailsResponse getById(
            UUID id) {

        Investment investment =
                investmentRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Investment not found"));

        return toDetailsResponse(investment);
    }

    @Override
    public void delete(
            UUID id) {

        Investment investment =
                investmentRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Investment not found"));

        investmentRepository.delete(investment);
    }

    private InvestmentResponse toResponse(Investment investment) {
        return toResponse(investment, investmentPaymentRepository.sumByInvestment(investment.getId()));
    }

    private InvestmentResponse toResponse(
            Investment investment, BigDecimal paid) {

        BigDecimal safePaid = paid == null ? BigDecimal.ZERO : paid;

        return InvestmentResponse
                .builder()
                .id(investment.getId())
                .invoiceNumber(
                        investment.getInvoiceNumber())
                .vendorId(
                        investment.getVendor() != null
                                ? investment.getVendor().getId()
                                : null)
                .vendorName(
                        investment.getVendor() != null
                                ? investment.getVendor().getName()
                                : null)
                .investmentType(
                        investment.getInvestmentType())
                .purchaseDate(
                        investment.getPurchaseDate())
                .itemCount(investment.getItems().size())
                .grandTotal(investment.getGrandTotal())
                .amountPaid(safePaid)
                .paymentStatus(computeStatus(safePaid, investment.getGrandTotal()))
                .build();
    }

    private PaymentStatus computeStatus(BigDecimal paid, BigDecimal grandTotal) {
        BigDecimal p = paid == null ? BigDecimal.ZERO : paid;
        BigDecimal g = grandTotal == null ? BigDecimal.ZERO : grandTotal;
        if (p.compareTo(BigDecimal.ZERO) <= 0) return PaymentStatus.PENDING;
        if (p.compareTo(g) >= 0) return PaymentStatus.PAID;
        return PaymentStatus.PARTIALLY_PAID;
    }

    private InvestmentDetailsResponse toDetailsResponse(
            Investment investment) {

        List<InvestmentPayment> payments =
                investmentPaymentRepository.findByInvestmentIdOrderByPaymentDateDescCreatedAtDesc(investment.getId());
        BigDecimal paid = payments.stream()
                .map(InvestmentPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return InvestmentDetailsResponse
                .builder()
                .id(investment.getId())
                .invoiceNumber(
                        investment.getInvoiceNumber())
                .vendorId(
                        investment.getVendor() != null
                                ? investment.getVendor().getId()
                                : null)
                .vendorName(
                        investment.getVendor() != null
                                ? investment.getVendor().getName()
                                : null)
                .investmentType(
                        investment.getInvestmentType())
                .purchaseDate(
                        investment.getPurchaseDate())
                .subTotal(
                        investment.getSubTotal())
                .gstAmount(
                        investment.getGstAmount())
                .discountAmount(
                        investment.getDiscountAmount())
                .otherCharge(
                        investment.getOtherCharge())
                .grandTotal(
                        investment.getGrandTotal())
                .amountPaid(paid)
                .amountDue(investment.getGrandTotal().subtract(paid).max(BigDecimal.ZERO))
                .paymentStatus(computeStatus(paid, investment.getGrandTotal()))
                .payments(payments.stream().map(this::toPaymentResponse).toList())
                .items(investment.getItems()
                                .stream()
                                .map(this::toItemResponse)
                                .toList())
                .build();
    }

    private InvestmentItemResponse toItemResponse(
            InvestmentItem item) {

        return InvestmentItemResponse
                .builder()
                .id(item.getId())
                .itemType(item.getItemType())
                .productId(
                        item.getProduct() != null
                                ? item.getProduct().getId()
                                : null)
                .itemName(
                        item.getItemName())
                .quantity(
                        item.getQuantity())
                .unit(item.getUnit())
                .rate(item.getRate())
                .totalAmount(
                        item.getTotalAmount())
                .build();
    }

    // ── Payments ──────────────────────────────────
    @Override
    public InvestmentDetailsResponse addPayment(UUID investmentId, AddPaymentRequest request) {

        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new BusinessException("Investment not found"));

        recordPayment(investment, request.getPaymentDate(), request.getMode(), request.getAmount());

        return toDetailsResponse(investment);
    }

    /**
     * Validate and persist one payment. Rules: date not before the invoice date and
     * not in the future; amount must not exceed the remaining due. Payments are
     * immutable once recorded (no delete).
     */
    private void recordPayment(Investment investment, LocalDate paymentDate,
                               com.skr.erp.common.constants.PaymentMode mode, BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be greater than zero.");
        }
        if (paymentDate == null) {
            throw new BusinessException("Payment date is required.");
        }
        if (paymentDate.isBefore(investment.getPurchaseDate())) {
            throw new BusinessException("Payment date cannot be before the invoice date ("
                    + investment.getPurchaseDate() + ").");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Payment date cannot be in the future.");
        }

        BigDecimal alreadyPaid = investmentPaymentRepository.sumByInvestment(investment.getId());
        BigDecimal due = investment.getGrandTotal().subtract(alreadyPaid);
        if (amount.compareTo(due) > 0) {
            throw new BusinessException(
                    "Payment (₹" + amount + ") exceeds the remaining due of ₹" + due + ".");
        }

        InvestmentPayment payment = new InvestmentPayment();
        payment.setInvestment(investment);
        payment.setPaymentDate(paymentDate);
        payment.setMode(mode);
        payment.setAmount(amount);
        investmentPaymentRepository.save(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentPaymentResponse> getPayments(UUID investmentId) {
        return investmentPaymentRepository
                .findByInvestmentIdOrderByPaymentDateDescCreatedAtDesc(investmentId)
                .stream()
                .map(this::toPaymentResponse)
                .toList();
    }

    private InvestmentPaymentResponse toPaymentResponse(InvestmentPayment p) {
        return InvestmentPaymentResponse.builder()
                .id(p.getId())
                .paymentDate(p.getPaymentDate())
                .mode(p.getMode())
                .amount(p.getAmount())
                .createdAt(p.getCreatedAt())
                .build();
    }

    // ── Invoice PDF ───────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public byte[] exportInvoicePdf(UUID id) {

        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Investment not found"));

        String template = systemSettingRepository
                .findBySettingKey(PURCHASE_INVOICE_TEMPLATE_KEY)
                .map(SystemSetting::getSettingValue)
                .orElseThrow(() -> new BusinessException(
                        "Invoice template not configured: " + PURCHASE_INVOICE_TEMPLATE_KEY));

        return HtmlToPdfGenerator.render(fillInvoiceTemplate(template, investment));
    }

    private String fillInvoiceTemplate(String template, Investment inv) {
        java.text.NumberFormat nf =
                java.text.NumberFormat.getNumberInstance(new java.util.Locale("en", "IN"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        String cur = currencyPrefix();

        List<InvestmentPayment> payments = investmentPaymentRepository
                .findByInvestmentIdOrderByPaymentDateDescCreatedAtDesc(inv.getId());
        BigDecimal paid = payments.stream()
                .map(InvestmentPayment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal grand = inv.getGrandTotal() != null ? inv.getGrandTotal() : BigDecimal.ZERO;
        BigDecimal due = grand.subtract(paid).max(BigDecimal.ZERO);
        PaymentStatus status = computeStatus(paid, grand);

        StringBuilder items = new StringBuilder();
        int idx = 1;
        if (inv.getItems() != null) {
            for (InvestmentItem it : inv.getItems()) {
                items.append("<tr>")
                        .append("<td>").append(idx++).append("</td>")
                        .append("<td><b>").append(esc(it.getItemName())).append("</b>")
                        .append("<div class=\"itype\">")
                        .append(it.getItemType() != null ? titleCase(it.getItemType().name()) : "")
                        .append("</div></td>")
                        .append("<td class=\"right\">").append(fmtQty(it.getQuantity())).append("</td>")
                        .append("<td>").append(esc(it.getUnit() != null ? String.valueOf(it.getUnit()) : "")).append("</td>")
                        .append("<td class=\"right\">").append(cur).append(money(nf, it.getRate())).append("</td>")
                        .append("<td class=\"right\">").append(cur).append(money(nf, it.getTotalAmount())).append("</td>")
                        .append("</tr>");
            }
        }
        if (items.length() == 0) {
            items.append("<tr><td colspan=\"6\" style=\"text-align:center;color:#999999;\">No items</td></tr>");
        }

        StringBuilder prows = new StringBuilder();
        for (InvestmentPayment p : payments) {
            prows.append("<tr>")
                    .append("<td>").append(p.getPaymentDate() != null ? p.getPaymentDate().format(DTF_DATE) : "-").append("</td>")
                    .append("<td>").append(p.getMode() != null ? titleCase(p.getMode().name()) : "-").append("</td>")
                    .append("<td class=\"right\">").append(cur).append(money(nf, p.getAmount())).append("</td>")
                    .append("</tr>");
        }
        if (prows.length() == 0) {
            prows.append("<tr><td colspan=\"3\" style=\"text-align:center;color:#999999;\">No payments recorded</td></tr>");
        }

        Vendor v = inv.getVendor();
        String generatedOn = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a"));

        return template
                .replace("{{companyName}}", esc(setting("COMPANY_NAME", "SKR Garment")))
                .replace("{{companyAddress}}", esc(setting("COMPANY_ADDRESS", "")))
                .replace("{{companyContact}}", esc(setting("COMPANY_CONTACT", "")))
                .replace("{{companyGstin}}", esc(setting("COMPANY_GSTIN", "-")))
                .replace("{{vendorName}}", esc(v != null ? v.getName() : "-"))
                .replace("{{vendorAddress}}", esc(v != null && v.getAddress() != null ? v.getAddress() : ""))
                .replace("{{vendorContact}}", esc(vendorContact(v)))
                .replace("{{vendorGstin}}", esc(v != null && v.getGstNumber() != null ? v.getGstNumber() : "-"))
                .replace("{{invoiceNumber}}", esc(inv.getInvoiceNumber()))
                .replace("{{invoiceDate}}", inv.getPurchaseDate() != null ? inv.getPurchaseDate().format(DTF_DATE) : "-")
                .replace("{{invoiceType}}", inv.getInvestmentType() != null ? titleCase(inv.getInvestmentType().name()) : "-")
                .replace("{{itemRows}}", items.toString())
                .replace("{{subtotal}}", cur + money(nf, inv.getSubTotal()))
                .replace("{{gst}}", cur + money(nf, inv.getGstAmount()))
                .replace("{{discount}}", cur + money(nf, inv.getDiscountAmount()))
                .replace("{{otherCharge}}", cur + money(nf, inv.getOtherCharge()))
                .replace("{{grandTotal}}", cur + money(nf, grand))
                .replace("{{amountPaid}}", cur + money(nf, paid))
                .replace("{{amountDue}}", cur + money(nf, due))
                .replace("{{paymentStatus}}", status != null ? titleCase(status.name()) : "-")
                .replace("{{paymentRows}}", prows.toString())
                .replace("{{generatedOn}}", generatedOn);
    }

    /** Currency prefix for the PDF. Default PDF fonts lack the rupee glyph, so map it to "Rs.". */
    private String currencyPrefix() {
        String sym = setting("CURRENCY_SYMBOL", "₹");
        return "₹".equals(sym) ? "Rs. " : sym;
    }

    private String setting(String key, String fallback) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSetting::getSettingValue)
                .filter(s -> s != null && !s.isBlank())
                .orElse(fallback);
    }

    private String vendorContact(Vendor v) {
        if (v == null) return "-";
        String mobile = v.getMobile() != null ? v.getMobile() : "";
        String email = (v.getEmail() != null && !v.getEmail().isBlank()) ? v.getEmail() : "";
        if (!mobile.isEmpty() && !email.isEmpty()) return mobile + " / " + email;
        String one = (mobile + email).trim();
        return one.isEmpty() ? "-" : one;
    }

    private static String money(java.text.NumberFormat nf, BigDecimal b) {
        return b != null ? nf.format(b) : nf.format(BigDecimal.ZERO);
    }

    private static String fmtQty(BigDecimal q) {
        return q != null ? q.stripTrailingZeros().toPlainString() : "0";
    }

    private static String titleCase(String s) {
        if (s == null || s.isBlank()) return "-";
        String[] parts = s.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}