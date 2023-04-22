package com.ecom.winners.dto;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransactionDTO {
    private Long user_id;
    private String amount;
}
