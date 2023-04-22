package com.ecom.winners.transformers;

import com.ecom.winners.dto.TransactionDTO;
import com.ecom.winners.entity.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;

public class TransactionProcessor implements ItemProcessor<TransactionDTO, Transaction> {
    private static final Logger logger = LoggerFactory.getLogger(TransactionProcessor.class);

    @Override
    public Transaction process(final TransactionDTO item) {
        logger.info(item.toString());
        BigDecimal amountInCent = new BigDecimal(item.getAmount());
        return new Transaction(item.getUser_id(), amountInCent);
    }
}
