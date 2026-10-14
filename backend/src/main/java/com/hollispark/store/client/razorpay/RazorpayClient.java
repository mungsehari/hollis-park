package com.hollispark.store.client.razorpay;

/** The two Razorpay calls the store needs. Swappable so tests never reach the network. */
public interface RazorpayClient {
    record RazorpayOrder(String id, long amountPaise, String currency) {}

    /** Creates a Razorpay order for an amount in paise; {@code receipt} is our order number. */
    RazorpayOrder createOrder(long amountPaise, String currency, String receipt);
}
