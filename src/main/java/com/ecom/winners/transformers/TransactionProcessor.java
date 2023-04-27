package com.ecom.winners.transformers;

import com.ecom.winners.dto.TransactionDTO;
import com.ecom.winners.entity.Transaction;
import com.ecom.winners.entity.User;
import com.ecom.winners.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * TransactionProcessor is an implementation of ItemProcessor functional interface(SAM) for user's transactions.
 * It maps the user's transactions from CSV file into User entity.
 * It checks for the existence of user in the database.
 * If the user doesn't exist, it won't do the transformation and returns null.
 *
 * @see AllArgsConstructor
 * @see ItemProcessor
 * @see TransactionDTO
 * @see Transaction
 */
@AllArgsConstructor
public class TransactionProcessor implements ItemProcessor<TransactionDTO, Transaction> {

    private final UserRepository userRepository;

    @Override
    public Transaction process(final TransactionDTO transactionDTO) {
        Optional<User> user = userRepository.findByUserId(transactionDTO.getUser_id());
        if (user.isEmpty())
            return null;
        Transaction transaction = new Transaction();
        transaction.setUserId(user.get());
        transaction.setAmount(new BigDecimal(transactionDTO.getAmount()));
        return transaction;
    }
}
