package com.carrental.paymentservice.dto;

import lombok.Data;

@Data
public class BankDetailsDTO {
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String bankName;
}
