package com.ecom.winners.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "addresses")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String street;
    private String suite;
    private String city;
    private String zipcode;
    @OneToOne(
            cascade = CascadeType.ALL
    )
    private GeoLocation geo;

    public Address() {
    }

    public Address(String street, String suite, String city, String zipcode, GeoLocation geo) {
        this.street = street;
        this.suite = suite;
        this.city = city;
        this.zipcode = zipcode;
        this.geo = geo;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public void setGeo(GeoLocation geo) {
        this.geo = geo;
    }

    public Long getId() {
        return id;
    }

    public String getStreet() {
        return street;
    }

    public String getSuite() {
        return suite;
    }

    public String getCity() {
        return city;
    }

    public String getZipcode() {
        return zipcode;
    }

    public GeoLocation getGeo() {
        return geo;
    }
}
