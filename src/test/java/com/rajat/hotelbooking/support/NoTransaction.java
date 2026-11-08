package com.rajat.hotelbooking.support;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

// runs the callback straight away so services that use TransactionTemplate can be unit tested
public class NoTransaction extends TransactionTemplate {

    @Override
    public <T> T execute(TransactionCallback<T> action) {
        TransactionStatus status = new SimpleTransactionStatus();
        return action.doInTransaction(status);
    }
}
