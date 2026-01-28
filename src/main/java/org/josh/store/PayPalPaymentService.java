package org.josh.store;

import org.springframework.stereotype.Service;

@Service
public class PayPalPaymentService implements PaymentService {
    @Override
    public void paymentProcess(double amount) {
        System.out.print("PAYPAL");
        System.out.println("amount: " + amount);
    }
}
