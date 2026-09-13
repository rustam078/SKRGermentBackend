package com.skr.erp.service;

import com.skr.erp.common.constants.InvestmentItemType;
import com.skr.erp.common.constants.InvestmentType;
import com.skr.erp.common.constants.ProductSource;
import com.skr.erp.dto.request.CreateInvestmentItemRequest;
import com.skr.erp.dto.request.CreateInvestmentRequest;
import com.skr.erp.dto.response.InvestmentDetailsResponse;
import com.skr.erp.dto.response.InvestmentResponse;
import com.skr.erp.entity.Investment;
import com.skr.erp.entity.InvestmentItem;
import com.skr.erp.entity.Product;
import com.skr.erp.entity.Vendor;
import com.skr.erp.exception.BusinessException;
import com.skr.erp.repository.InvestmentItemRepository;
import com.skr.erp.repository.InvestmentRepository;
import com.skr.erp.repository.ProductRepository;
import com.skr.erp.repository.VendorRepository;
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

        return toResponse(investment);
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

        return investmentRepository
                .findAll(specification)
                .stream()
                .map(this::toResponse)
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

    private InvestmentResponse toResponse(
            Investment investment) {

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
                .build();
    }

    private InvestmentDetailsResponse toDetailsResponse(
            Investment investment) {

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
}