package org.josh.store;

import org.springframework.stereotype.Service;

@Service
public interface PaymentService {
    void paymentProcess(double amount);
}
