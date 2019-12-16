package com.banking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.banking.constant.AppConstant;
import com.banking.dto.RegisterRequestDto;
import com.banking.dto.RegisterResponseDto;
import com.banking.entity.User;
import com.banking.entity.UserAccount;
import com.banking.exception.UserAlreadyExist;
import com.banking.repository.UserAccountRepository;
import com.banking.repository.UserRepository;

/**
 * @description User service implementation class purpose we can implemented
 *              here the functionalities of user service methods of user
 *              registration, generate the account number and also generate the
 *              password.
 * 
 * @author Janani
 * @since 2019-12-5
 *
 */
@Service
public class UserServiceImpl implements UserService {
	public static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserAccountRepository userAccountRepository;

	/**
	 * @description This is registerUser method used to register and it will
	 *              generate accountNumber for the user.
	 * @param registraionRequestDto object the values of the user registraion param
	 *                              values.
	 * @return responseDto object have the returns the success or failure message,
	 *         status based on the validations.
	 *
	 */

	@Override
	public RegisterResponseDto registerUser(RegisterRequestDto userRegisterDto) {
		logger.info("user registarion...");
		RegisterResponseDto registerResponseDto = new RegisterResponseDto();
		// check the phone number is already exists or not.
		User isValidUser = userRepository.findUserByPhone(userRegisterDto.getPhone());
		Optional<User> optioanlUser = Optional.ofNullable(isValidUser);
		if (!optioanlUser.isPresent()) {

			User user = new User();
			user.setFirstName(userRegisterDto.getFirstName());
			user.setLastName(userRegisterDto.getLastName());
			user.setUserName(userRegisterDto.getPhone());

			String password = generatePassword();
			user.setPassword(password);
			user.setAddress1(userRegisterDto.getAddress1());
			user.setAddress2(userRegisterDto.getAddress2());

			// convert String to LocalDate
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			LocalDate userDob = LocalDate.parse(userRegisterDto.getDob(), formatter);
			user.setDob(userDob);
			user.setEmailAddress(userRegisterDto.getEmailAddress());
			user.setPanNumber(userRegisterDto.getPanNumber());
			user.setPhone(userRegisterDto.getPhone());
			user.setPinCode(userRegisterDto.getPinCode());
			userRepository.save(user);

			// User Accounts updation
			UserAccount userAccount = new UserAccount();

			Long accountNumber = generateAccountNumber();
			userAccount.setAccountNumber(accountNumber);
			userAccount.setId(user.getId());
			userAccount.setMinimumBalance(AppConstant.ACCOUNT_MINIMUM_BALANCE);
			userAccount.setUserId(user.getId());
			userAccount.setAccountType(AppConstant.ACCOUNT_TYPE_SAVINGS);
			userAccount.setBalanceAmount(AppConstant.ACCOUNT_BALANCE_AMOUNT);
			userAccount.setCreatedDate(LocalDateTime.now());
			userAccountRepository.save(userAccount);

			// Set response details
			registerResponseDto.setUserId(user.getUserName());
			registerResponseDto.setPassword(user.getPassword());
			registerResponseDto.setStatus(AppConstant.SUCCESS);
			registerResponseDto.setMessage(AppConstant.REGISTER_SUCCESS_MESSAGE);

		} else {
			throw new UserAlreadyExist(AppConstant.USER_EXIST);
		}

		return registerResponseDto;
	}

	/**
	 * @description get the password by using alpha numeric characters
	 * 
	 * @return String of the generated password.
	 */
	public String generatePassword() {
		return RandomStringUtils.random(6, true, true);
	}

	/**
	 * @description get the account number by generated new account number based on
	 *              the numeric values.
	 * @return return the long value of the generated account number.
	 */
	public Long generateAccountNumber() {
		String number = RandomStringUtils.random(16, false, true);
		return Long.valueOf(number);
	}
}