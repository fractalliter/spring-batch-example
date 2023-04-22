package com.ecom.winners.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "transactions")
public class Transaction {
    Long user_id;
    BigDecimal amount;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Long id;

    public Transaction(Long user_id, BigDecimal amount) {
        this.user_id = user_id;
        this.amount = amount;
    }

    public Transaction() {

    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getUser_id() {
        return user_id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long getId() {
        return id;
    }
}
