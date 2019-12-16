package com.banking.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 
 * @author Govindasamy.C
 *
 */
@Getter
@Setter
public class ViewPayeeDto {

	private Integer accountId;
	private String accountType;
	private Long accountNumber;
	private String payeeName;


}
