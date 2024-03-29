package com.banking.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.banking.constant.AppConstant;
import com.banking.dto.FundTransferRequestDto;
import com.banking.dto.ResponseDto;
import com.banking.dto.UserTransactionRequestDto;
import com.banking.dto.UserTransactionResponseDto;
import com.banking.entity.User;
import com.banking.entity.UserAccount;
import com.banking.entity.UserTransaction;
import com.banking.repository.UserAccountRepository;
import com.banking.repository.UserRepository;
import com.banking.repository.UserTransactionRepository;
import com.banking.util.CommonUtil;
import com.banking.util.ConverterUtil;

import javassist.NotFoundException;

/**
 * @description UserTransactionServiceImpl - we can implementing the user
 *              transaction methods of the fund transfer based on the user
 *              account to user account and also implemented the fund transfer
 *              to user account to mortgage account for paid loan amount.
 * 
 * @author Govindasamy.C
 * @since 05-12-2019
 *
 */
@Service
@Transactional
public class UserTransactionServiceImpl implements UserTransactionService {
	private static final Logger logger = LoggerFactory.getLogger(UserTransactionServiceImpl.class);

	@Autowired
	UserAccountRepository userAccountRepository;

	@Autowired
	UserTransactionRepository userTransactionRepository;

	@Autowired
	UserRepository userRepository;

	/**
	 * @description user can transfer the amount to one user account to another user
	 *              account and also we are handle the user can to pay the loan
	 *              amount.ie, user acccount to user mortgage account.
	 * @param its send the param values are fund transfer related params for
	 *            accountFrom, accountId and transfer amount value.
	 * @return responseDto object set the success and failure values.
	 * @throws NotFoundException for check whether useraccountId is present or not.
	 *                           if not present, we thorws the useraccount not found
	 *                           exception.
	 */
	@Override
	public ResponseDto fundTransfer(FundTransferRequestDto fundTransferRequestDto) throws NotFoundException {
		logger.info("fund transfer transaction...");
		ResponseDto fundTransferResponseDto = new ResponseDto();
		Long accountNumber = Long.valueOf(fundTransferRequestDto.getPayeeAccountNumber());
		Optional<UserAccount> userAccountDetail = userAccountRepository.findById(fundTransferRequestDto.getAccountId());
		Optional<UserAccount> userPayeeAccountDetail = userAccountRepository.findByAccountNumber(accountNumber);
		if (userAccountDetail.isPresent() && userPayeeAccountDetail.isPresent()) {
			// User Account Details
			UserAccount userAccount = userAccountDetail.get();
			UserAccount userPayeeAccount = userPayeeAccountDetail.get();

			// Check Minimum balance from user accounts.
			Double totalAmount = fundTransferRequestDto.getTransferAmount() + userAccount.getMinimumBalance();
			if (totalAmount <= userAccount.getBalanceAmount()) {
				UserTransaction userTransaction = ConverterUtil.convertDtoToTransactionEntity(fundTransferRequestDto);
				Double debitAmount = userAccount.getBalanceAmount() - fundTransferRequestDto.getTransferAmount();
				userAccount.setBalanceAmount(debitAmount);

				// Get Transaction Number
				String transactionNumber = getTransactionNumber();

				userTransaction.setTransactionId(transactionNumber);
				userTransaction.setUserAccountId(userAccount);
				userTransaction.setPayeeAccountId(userPayeeAccount);
				userTransactionRepository.save(userTransaction);

				// Credit the payee acoount balance amount
				Double creditAmount = userPayeeAccount.getBalanceAmount() + fundTransferRequestDto.getTransferAmount();
				userPayeeAccount.setBalanceAmount(creditAmount);
				logger.debug("before saving the fund transfer...");
				userAccountRepository.save(userPayeeAccount);

				fundTransferResponseDto.setStatus(AppConstant.SUCCESS);
				fundTransferResponseDto.setMessage(AppConstant.FUND_TRANSFER_SUCCESS);
			} else {
				fundTransferResponseDto.setStatus(AppConstant.FAILURE);
				fundTransferResponseDto.setMessage(AppConstant.FUND_TRANSFER_MIN_BAL);
			}
		} else {
			throw new NotFoundException(AppConstant.NO_ACCOUNTS_FOUND);
		}

		return fundTransferResponseDto;
	}

	/**
	 * @description This method is used for to get recent 5 transactions input
	 *              parameter account and also we are handle the user can to pay the
	 *              loan amount.ie, user acccount to user mortgage account.
	 * 
	 * @return usertransactionresponsedto object for return list of last five
	 *         transactions summary.
	 */
	@Override
	public UserTransactionResponseDto findRecentFiveTransactions(Integer userAccountId) {
		logger.info("recent five transaction summary...");
		List<UserTransaction> userTransactionResponse = userTransactionRepository
				.findTop5ByUserAccountIdIdOrPayeeAccountIdIdOrderByIdDesc(userAccountId, userAccountId);

		return convertUserTransactionEntityToUserTransactionResponseDto(userTransactionResponse);
	}

	/**
	 * @description get the all morgage account transaction summary based on the
	 *              login user.
	 * @return list of the mortgage account transactions with success or failure
	 *         status.
	 */
	@Override
	public UserTransactionResponseDto findMortgageTransactions(Integer userAccountId) {
		logger.info("get all mortgage account transaction summary...");
		List<UserTransaction> transactions = userTransactionRepository
				.findAllByUserAccountIdIdOrPayeeAccountIdIdOrderByIdDesc(userAccountId, userAccountId);

		return convertUserTransactionEntityToMortgage(transactions);
	}

	/**
	 * @description here we are converting the transaction records to responseDto
	 *              based on the required param values.
	 * @param transactions list of the transactions
	 * @return list of the transactions we can converted into single responseDto
	 *         based retuired params only.
	 */
	private UserTransactionResponseDto convertUserTransactionEntityToMortgage(List<UserTransaction> transactions) {
		UserTransactionResponseDto responseDto = new UserTransactionResponseDto();
		List<UserTransactionRequestDto> list = new ArrayList<>();
		transactions.forEach(transaction -> {

			UserAccount userAccount = transaction.getPayeeAccountId();
			if (userAccount.getAccountType().equals(AppConstant.ACCOUNT_TYPE_MORTGAGE)) {

				UserTransactionRequestDto userTransactionRequestDto = new UserTransactionRequestDto();
				Optional<User> user = userRepository.findById(transaction.getPayeeAccountId().getUserId());
				if (user.isPresent()) {
					userTransactionRequestDto.setPayeeName(user.get().getFirstName() + " " + user.get().getLastName());
				}
				userTransactionRequestDto.setRemarks(transaction.getRemarks());
				userTransactionRequestDto
						.setPayeeAccountNumber(String.valueOf(transaction.getPayeeAccountId().getAccountNumber()));
				userTransactionRequestDto.setTransactionType(transaction.getTransactionType());
				userTransactionRequestDto.setTransactionDate(transaction.getTransactionDate());
				userTransactionRequestDto.setBalanceAmount(transaction.getPayeeAccountId().getBalanceAmount());
				userTransactionRequestDto.setTransactionAmount(transaction.getTransactionAmount());
				list.add(userTransactionRequestDto);
			}
		});

		if (list.isEmpty()) {
			responseDto.setMessage(AppConstant.NO_RECORD_FOUND);
		} else {
			responseDto.setMessage(AppConstant.OPERATION_SUCCESS);
		}
		responseDto.setTransactionDetails(list);
		responseDto.setStatusCode(HttpStatus.OK.value());
		return responseDto;
	}

	/**
	 * @description this method is to get entire month transactions to respective
	 *              userAccountId
	 * 
	 * @param userAccountId Integer, month Integer, year Integer - providing
	 *                      required account number, month and year to search
	 *                      monthly transactions
	 * @return UserTransactionResponseDto object contain with status and message
	 *         along with response object
	 */
	@Override
	public UserTransactionResponseDto findUserTransactionsByMonth(Integer userAccountId, Integer month, Integer year) {

		logger.info("Getting monthly transactions fot the given user account");

		String inputMonth = String.format("%02d", month);

		LocalDate startDate = LocalDate.parse(year + "-" + inputMonth + "-" + "01");

		Integer lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth().getDayOfMonth();

		LocalDate endDate = LocalDate.parse(year + "-" + inputMonth + "-" + lastDayOfMonth);

		List<UserTransaction> userTransactionResponse = userTransactionRepository
				.getAllByUserAccountIdIdAndTransactionDateBetween(userAccountId, startDate, endDate);

		return convertUserTransactionEntityToUserTransactionResponseDto(userTransactionResponse);
	}

	/**
	 * @description this is private method it is used for Entity to DtoResponse
	 *              converts - written in single separate method for code re-use
	 * @param list of transaction entitries
	 * @return UserTransactionResponseDto values are response required values only
	 *         sended.
	 */
	/**
	 * @description this is private method it is used for Entity to DtoResponse
	 *              converts - written in single separate method for code re-use
	 * @param userTransactionResponse object
	 * @return UserTransactionResponseDto return the object with status code and
	 *         message
	 */
	private UserTransactionResponseDto convertUserTransactionEntityToUserTransactionResponseDto(
			List<UserTransaction> userTransactionResponse) {

		UserTransactionResponseDto userTransactionResponseDto = new UserTransactionResponseDto();
		List<UserTransactionRequestDto> response = new ArrayList<>();

		if (null != userTransactionResponse && userTransactionResponse.size() > AppConstant.ZERO) {

			response = userTransactionResponse.stream().map(request -> {

				UserTransactionRequestDto userTransactionRequestDto = new UserTransactionRequestDto();
				Optional<User> user = userRepository.findById(request.getPayeeAccountId().getUserId());
				if (user.isPresent()) {
					userTransactionRequestDto.setPayeeName(user.get().getFirstName() + " " + user.get().getLastName());
				}
				userTransactionRequestDto.setRemarks(request.getRemarks());
				userTransactionRequestDto
						.setPayeeAccountNumber(String.valueOf(request.getPayeeAccountId().getAccountNumber()));
				userTransactionRequestDto.setTransactionType(request.getTransactionType());
				userTransactionRequestDto.setTransactionDate(request.getTransactionDate());
				userTransactionRequestDto.setTransactionAmount(request.getTransactionAmount());

				return userTransactionRequestDto;
			}).collect(Collectors.toList());

			userTransactionResponseDto.setMessage(AppConstant.OPERATION_SUCCESS);
			userTransactionResponseDto.setStatusCode(HttpStatus.OK.value());
			userTransactionResponseDto.setTransactionDetails(response);

			return userTransactionResponseDto;

		} else {
			userTransactionResponseDto.setMessage(AppConstant.NO_RECORD_FOUND);
			userTransactionResponseDto.setStatusCode(HttpStatus.OK.value());
			userTransactionResponseDto.setTransactionDetails(response);
		}
		return userTransactionResponseDto;
	}

	/**
	 * @description get the transaction number based on the unique alphanumeric
	 *              values.
	 * 
	 * @return return the string value of the generated transaction number.
	 */
	private String getTransactionNumber() {
		logger.info("get the transaction number...");
		Integer transactionId = CommonUtil.getTransactionNumber();
		String transactionNumber = AppConstant.GET_TRANSACTION_NO_PREFIX + transactionId;

		UserTransaction userTransaction = userTransactionRepository.findByTransactionId(transactionNumber);
		Optional<UserTransaction> isUserTransaction = Optional.ofNullable(userTransaction);
		if (isUserTransaction.isPresent()) {
			getTransactionNumber();
		}
		return transactionNumber;

	}
}
