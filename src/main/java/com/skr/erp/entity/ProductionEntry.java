package com.skr.erp.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "production_entry")
@Getter
@Setter
public class ProductionEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    private String remarks;

    @OneToMany(
            mappedBy = "productionEntry",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ProductionEntryDetail> details =
            new ArrayList<>();
}