package com.campus.resourcesharing.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
public class Resource {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @NotBlank(message = "Category is required")
    @Column(nullable = false)
    private String category;
    
    @NotBlank(message = "Condition status is required")
    @Column(nullable = false)
    private String conditionStatus;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
    
    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;
    
    @NotBlank(message = "Transaction type is required (SELL, EXCHANGE, RENT, or DONATE)")
    @Column(name = "transaction_type", nullable = false)
    private String transactionType; // SELL, EXCHANGE, RENT, DONATE
    
    @Column(name = "price")
    private Double price; // For SELL and RENT
    
    @Column(name = "rental_duration")
    private String rentalDuration; // For RENT (e.g., "1 week", "1 month", "3 months")
    
    @Column(name = "exchange_description", columnDefinition = "TEXT")
    private String exchangeDescription; // For EXCHANGE - what they want in exchange
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public Resource() {}
    
    public Resource(String title, String description, String category, String conditionStatus, User owner) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.conditionStatus = conditionStatus;
        this.owner = owner;
        this.isAvailable = true;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getConditionStatus() {
        return conditionStatus;
    }
    
    public void setConditionStatus(String conditionStatus) {
        this.conditionStatus = conditionStatus;
    }
    
    public User getOwner() {
        return owner;
    }
    
    public void setOwner(User owner) {
        this.owner = owner;
    }
    
    public Boolean getIsAvailable() {
        return isAvailable;
    }
    
    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getTransactionType() {
        return transactionType;
    }
    
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }
    
    public Double getPrice() {
        return price;
    }
    
    public void setPrice(Double price) {
        this.price = price;
    }
    
    public String getRentalDuration() {
        return rentalDuration;
    }
    
    public void setRentalDuration(String rentalDuration) {
        this.rentalDuration = rentalDuration;
    }
    
    public String getExchangeDescription() {
        return exchangeDescription;
    }
    
    public void setExchangeDescription(String exchangeDescription) {
        this.exchangeDescription = exchangeDescription;
    }
}
