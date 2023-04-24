package com.ecom.winners.transformers;

import com.ecom.winners.dto.TransactionDTO;
import com.ecom.winners.entity.Transaction;
import com.ecom.winners.entity.User;
import com.ecom.winners.repositories.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;
import java.util.Optional;

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
