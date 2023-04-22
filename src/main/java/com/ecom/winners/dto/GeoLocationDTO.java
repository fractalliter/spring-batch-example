package com.ecom.winners.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class GeoLocationDTO {
    private String lng;
    private String lat;
}
