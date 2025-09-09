// src/main/java/com/example/demoapp/model/Purchase.java
package com.example.demoapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "purchases")
public class Purchase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "software_id", nullable = false)
    private Software software;

    @Column(name = "purchase_date", nullable = false)
    private LocalDateTime purchaseDate;

    @Column(name = "price_paid", nullable = false)
    private Double pricePaid;

    @Column(name = "payment_id")
    private String paymentId;

    // Constructors
    public Purchase() {
    }

    public Purchase(User user, Software software, LocalDateTime purchaseDate, Double pricePaid, String paymentId) {
        this.user = user;
        this.software = software;
        this.purchaseDate = purchaseDate;
        this.pricePaid = pricePaid;
        this.paymentId = paymentId;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Software getSoftware() {
        return software;
    }

    public void setSoftware(Software software) {
        this.software = software;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Double getPricePaid() {
        return pricePaid;
    }

    public void setPricePaid(Double pricePaid) {
        this.pricePaid = pricePaid;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}
