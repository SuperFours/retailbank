package com.banking.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.banking.constant.AppConstant;
import com.banking.dto.AccountBalanceDto;
import com.banking.dto.UserAccountDto;
import com.banking.dto.UserAccountResponseDto;
import com.banking.service.UserAccountService;

@RunWith(SpringJUnit4ClassRunner.class)
public class UserAccountControllerTest {

	@InjectMocks
	UserAccountController userAccountController;

	@Mock
	UserAccountService userAccountService;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);
	}


	@Test
	public void testGetUserAvailableBalance() {
		AccountBalanceDto accountBalanceDto = new AccountBalanceDto();
		accountBalanceDto.setAccountBalance(1000.00);
		accountBalanceDto.setStatus(AppConstant.SUCCESS);
		when(userAccountService.getAccountBalance(1)).thenReturn(accountBalanceDto);

		ResponseEntity<AccountBalanceDto> response = userAccountController.getUserAvailableBalance(1);
		assertEquals(accountBalanceDto.getAccountBalance(), response.getBody().getAccountBalance());
	}

	@Test
	public void testSearchSavingAccounts() {
		List<UserAccountDto> accounts = new ArrayList<>();
		UserAccountDto userAccountDto = new UserAccountDto();
		userAccountDto.setUserName("Moorthy");
		accounts.add(userAccountDto);

		when(userAccountService.getAccounts("156727783738384")).thenReturn(accounts);

		ResponseEntity<UserAccountResponseDto> resposne = userAccountController.searchSavingAccounts("156727783738384");
		assertEquals(AppConstant.SUCCESS, resposne.getBody().getMessage());
	}

}
