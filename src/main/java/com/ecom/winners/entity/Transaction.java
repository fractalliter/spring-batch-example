package com.ecom.winners.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transactions")
@EntityListeners(AuditingEntityListener.class)
public class Transaction {
    @ManyToOne
    @JoinColumn(name = "userId", nullable = false)
    private User userId;
    private BigDecimal amount;
    @CreatedDate
    private Instant createdAt;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public Transaction() {
    }

    public void setUserId(User userId) {
        this.userId = userId;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public User getUserId() {
        return userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
