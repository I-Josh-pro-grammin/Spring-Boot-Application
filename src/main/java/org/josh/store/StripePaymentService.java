package org.josh.store;

public class StripePaymentService implements PaymentService {

    @Override
    public void paymentProcess(double amount) {
        System.out.println("STRIPE");
        System.out.println("Amount: " + amount);
    }
}
