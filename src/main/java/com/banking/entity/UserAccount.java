package com.banking.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;


/**
 * UserAccount Entity
 * @author Govindasamy.C
 * @Since
 */
@Entity
@Setter
@Getter
@Table(name = "user_account")
@SequenceGenerator(name = "sequence", initialValue = 10)
public class UserAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence")
	private Integer id;
	@Column(name = "user_id")
	
	private Integer userId;
	private String accountType;
	private Long accountNumber;
	private Double minimumBalance;
	private Double balanceAmount;
	private LocalDateTime createdDate;

	
}
