package com.skr.erp.specification;

import com.skr.erp.entity.ProductionEntry;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProductionSpecification {

    private ProductionSpecification() {
    }

    public static Specification<ProductionEntry> filter(LocalDate fromDate, LocalDate toDate, UUID employeeId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("productionDate"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("productionDate"), toDate));
            }
            if (employeeId != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}