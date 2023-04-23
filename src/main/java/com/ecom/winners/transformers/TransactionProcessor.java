package com.ecom.winners.transformers;

import com.ecom.winners.dto.TransactionDTO;
import com.ecom.winners.entity.Transaction;
import com.ecom.winners.entity.User;
import com.ecom.winners.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.Optional;

public class TransactionProcessor implements ItemProcessor<TransactionDTO, Transaction> {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);
    private final UserRepository userRepository;

    public TransactionProcessor(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Transaction process(final TransactionDTO transactionDTO) {
        logger.info(transactionDTO.toString());
        Optional<User> user = userRepository.findByUserId(transactionDTO.getUser_id());
        if (user.isEmpty())
            return null;
        Transaction transaction = new Transaction();
        transaction.setUserId(user.get());
        transaction.setAmount(new BigDecimal(transactionDTO.getAmount()));
        return transaction;
    }
}
