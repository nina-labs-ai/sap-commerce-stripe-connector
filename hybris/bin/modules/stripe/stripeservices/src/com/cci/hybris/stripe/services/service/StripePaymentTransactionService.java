
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service;

import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.data.StripeRefundData;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;

import java.util.Optional;

/**
 * Owns local SAP Commerce payment transaction updates for Stripe Checkout.
 */
public interface StripePaymentTransactionService {

    /**
     * Registers a Checkout Session authorization entry for the supplied order.
     *
     * @param order order that owns the Checkout Session
     * @param sessionData Checkout Session data
     */
    void registerCheckoutSession(AbstractOrderModel order, StripeCheckoutSessionData sessionData);

    /**
     * Marks a Checkout Session as completed and captured for the supplied order.
     *
     * @param order order that owns the Checkout Session
     * @param sessionData Checkout Session data
     */
    void markCheckoutSessionCompleted(AbstractOrderModel order, StripeCheckoutSessionData sessionData);

    /**
     * Marks a Checkout Session as expired for the supplied order.
     *
     * @param order order that owns the Checkout Session
     * @param sessionData Checkout Session data
     */
    void markCheckoutSessionExpired(AbstractOrderModel order, StripeCheckoutSessionData sessionData);

    /**
     * Registers a PaymentIntent authorization entry for the supplied order.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentData PaymentIntent data
     */
    void registerPaymentIntent(AbstractOrderModel order, StripePaymentIntentData paymentIntentData);

    /**
     * Marks a PaymentIntent as succeeded and captured for the supplied order.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentData PaymentIntent data
     */
    void markPaymentIntentSucceeded(AbstractOrderModel order, StripePaymentIntentData paymentIntentData);

    /**
     * Marks a PaymentIntent as failed for the supplied order.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentData PaymentIntent data
     */
    void markPaymentIntentFailed(AbstractOrderModel order, StripePaymentIntentData paymentIntentData);

    /**
     * Marks a PaymentIntent as cancelled for the supplied order.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentData PaymentIntent data
     */
    void markPaymentIntentCancelled(AbstractOrderModel order, StripePaymentIntentData paymentIntentData);

    /**
     * Registers a Stripe refund entry for the supplied order.
     *
     * @param order refunded order
     * @param paymentReference refunded Stripe reference
     * @param refundData Stripe refund data
     */
    void registerRefund(AbstractOrderModel order, String paymentReference, StripeRefundData refundData);

    /**
     * Returns the latest open PaymentIntent identifier for the supplied order.
     *
     * @param order order to inspect
     * @return latest open PaymentIntent identifier
     */
    Optional<String> findLatestOpenPaymentIntentId(AbstractOrderModel order);

    /**
     * Returns whether the supplied order already contains the Stripe request identifier.
     *
     * @param order order to inspect
     * @param requestId Stripe request identifier
     * @return {@code true} when the entry exists
     */
    boolean hasPaymentTransactionEntry(AbstractOrderModel order, String requestId);

    /**
     * Finds an order or cart by code.
     *
     * @param code order or cart code
     * @return matching order model
     */
    Optional<AbstractOrderModel> findOrderByCode(String code);

    /**
     * Finds an order or cart by order code and Stripe payment reference.
     *
     * @param code order or cart code
     * @param requestId Stripe request identifier
     * @return matching order model
     */
    Optional<AbstractOrderModel> findOrderByPaymentReference(String code, String requestId);

    /**
     * Finds an order or cart by Stripe payment reference only.
     *
     * @param requestId Stripe request identifier
     * @return matching order model
     */
    Optional<AbstractOrderModel> findOrderByPaymentReference(String requestId);

    /**
     * Copies Stripe Checkout Session and PaymentIntent entries from a source cart or order to the target order.
     *
     * @param source source cart or order
     * @param target target order
     */
    void synchronizeStripePaymentsToOrder(AbstractOrderModel source, OrderModel target);
}
