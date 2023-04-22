package com.ecom.winners.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "locations")
public class GeoLocation {
    private String longitude;
    private String Latitude;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    public GeoLocation(String longitude, String latitude) {
        this.longitude = longitude;
        Latitude = latitude;
    }

    public GeoLocation() {

    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(String latitude) {
        Latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public String getLatitude() {
        return Latitude;
    }

    public Long getId() {
        return id;
    }
}
