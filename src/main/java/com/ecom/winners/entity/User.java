package com.ecom.winners.entity;

import jakarta.persistence.*;

import java.util.Set;

@Entity
@Table(
        name = "users",
        indexes = {@Index(columnList = "userId")}
)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(unique = true)
    private Long userId;
    private String name;
    @Column(unique = true)
    private String username;
    private String phone;
    private String website;
    @OneToOne(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER
    )
    private Address address;
    @OneToOne(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER
    )
    private Company company;

    @OneToMany(
            cascade = CascadeType.ALL,
            fetch = FetchType.EAGER,
            mappedBy = "userId"
    )
    private Set<Transaction> transactions;

    public User(
            Long userId,
            String name,
            String username,
            String phone,
            String website,
            Address address,
            Company company,
            Set<Transaction> transactions
    ) {
        this.userId = userId;
        this.name = name;
        this.username = username;
        this.phone = phone;
        this.website = website;
        this.address = address;
        this.company = company;
        this.transactions = transactions;
    }

    public User() {

    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getId() {
        return id;
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

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public Set<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(Set<Transaction> transactions) {
        this.transactions = transactions;
    }
}
