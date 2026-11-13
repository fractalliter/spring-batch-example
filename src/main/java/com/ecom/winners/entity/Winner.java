package com.ecom.winners.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Người thắng cuộc được ghi lại khi gửi lên Kafka topic {@code lucky_winner}.
 * Mỗi lần chọn đều tạo một dòng mới, kể cả khi trùng user.
 */
@Entity
@Table(name = "winners", indexes = {@Index(columnList = "wonAt")})
public class Winner {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private String username;
    private String phone;
    private String website;
    private String companyName;
    private String city;
    private LocalDateTime wonAt;

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public LocalDateTime getWonAt() {
        return wonAt;
    }

    public void setWonAt(LocalDateTime wonAt) {
        this.wonAt = wonAt;
    }
}
