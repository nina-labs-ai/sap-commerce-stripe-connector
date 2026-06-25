
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Payment transaction helper utilities used by Stripe transaction services.
 */
public final class StripePaymentTransactionUtils {

    private static final int DEFAULT_CURRENCY_DIGITS = 2;

    private StripePaymentTransactionUtils() {
    }

    /**
     * Finds a transaction entry by type and request identifier.
     *
     * @param transaction payment transaction
     * @param type entry type
     * @param requestId Stripe request identifier
     * @return matching transaction entry
     */
    public static Optional<PaymentTransactionEntryModel> findEntry(final PaymentTransactionModel transaction,
                                                                   final PaymentTransactionType type,
                                                                   final String requestId) {
        if (transaction == null || type == null || requestId == null) {
            return Optional.empty();
        }
        if (CollectionUtils.isEmpty(transaction.getEntries())) {
            return Optional.empty();
        }

        return transaction.getEntries().stream()
                .filter(entry -> type == entry.getType() && requestId.equals(entry.getRequestId()))
                .findFirst();
    }

    /**
     * Returns whether the transaction contains an entry of the given type and status.
     *
     * @param transaction payment transaction
     * @param type entry type
     * @param status status value
     * @return {@code true} when a matching entry exists
     */
    public static boolean hasEntryWithStatus(final PaymentTransactionModel transaction,
                                             final PaymentTransactionType type,
                                             final String status) {
        if (transaction == null || type == null || status == null) {
            return false;
        }
        if (CollectionUtils.isEmpty(transaction.getEntries())) {
            return false;
        }

        return transaction.getEntries().stream()
                .anyMatch(entry -> type == entry.getType() && status.equals(entry.getTransactionStatus()));
    }

    /**
     * Returns whether the transaction contains an entry for request id and status.
     *
     * @param transaction payment transaction
     * @param type entry type
     * @param requestId Stripe request identifier
     * @param status status value
     * @return {@code true} when a matching entry exists
     */
    public static boolean hasEntryWithStatus(final PaymentTransactionModel transaction,
                                             final PaymentTransactionType type,
                                             final String requestId,
                                             final String status) {
        if (hasInvalidStatusLookupInput(transaction, type, requestId, status)) {
            return false;
        }
        if (CollectionUtils.isEmpty(transaction.getEntries())) {
            return false;
        }

        return transaction.getEntries().stream()
                .anyMatch(entry -> type == entry.getType()
                        && requestId.equals(entry.getRequestId())
                        && status.equals(entry.getTransactionStatus()));
    }

    private static boolean hasInvalidStatusLookupInput(final PaymentTransactionModel transaction,
                                                       final PaymentTransactionType type,
                                                       final String requestId,
                                                       final String status) {
        if (transaction == null || type == null) {
            return true;
        }
        return requestId == null || status == null;
    }

    /**
     * Returns whether the request has already been captured successfully.
     *
     * @param transaction payment transaction
     * @param requestId Stripe request identifier
     * @return {@code true} when captured
     */
    public static boolean isCaptured(final PaymentTransactionModel transaction, final String requestId) {
        return hasEntryWithStatus(transaction, PaymentTransactionType.CAPTURE, requestId, StripeServicesConstants.STATUS_ACCEPTED);
    }

    /**
     * Returns whether the request has already been rejected.
     *
     * @param transaction payment transaction
     * @param requestId Stripe request identifier
     * @return {@code true} when rejected
     */
    public static boolean isRejected(final PaymentTransactionModel transaction, final String requestId) {
        return hasEntryWithStatus(transaction, PaymentTransactionType.AUTHORIZATION, requestId, StripeServicesConstants.STATUS_REJECTED);
    }

    /**
     * Returns whether the entry belongs to a Stripe checkout/payment-intent flow.
     *
     * @param entry transaction entry
     * @return {@code true} when the entry points to Stripe payment references
     */
    public static boolean isStripePaymentEntry(final PaymentTransactionEntryModel entry) {
        final String requestId = entry == null ? null : entry.getRequestId();
        return StripePaymentReferenceUtils.isPaymentIntentReference(requestId)
                || StripePaymentReferenceUtils.isCheckoutSessionReference(requestId);
    }

    /**
     * Attaches a payment transaction to an order when missing.
     *
     * @param order owning order/cart
     * @param transaction payment transaction
     */
    public static void attachTransaction(final AbstractOrderModel order, final PaymentTransactionModel transaction) {
        final List<PaymentTransactionModel> transactions = CollectionUtils.isEmpty(order.getPaymentTransactions())
                ? new ArrayList<>()
                : new ArrayList<>(order.getPaymentTransactions());
        if (!transactions.contains(transaction)) {
            transactions.add(transaction);
            order.setPaymentTransactions(transactions);
        }
    }

    /**
     * Attaches an entry to a payment transaction when missing.
     *
     * @param transaction payment transaction
     * @param entry transaction entry
     */
    public static void attachEntry(final PaymentTransactionModel transaction, final PaymentTransactionEntryModel entry) {
        final List<PaymentTransactionEntryModel> entries = CollectionUtils.isEmpty(transaction.getEntries())
                ? new ArrayList<>()
                : new ArrayList<>(transaction.getEntries());
        if (!entries.contains(entry)) {
            entries.add(entry);
            transaction.setEntries(entries);
        }
    }

    /**
     * Collects Checkout Session request identifiers registered on an order/cart.
     *
     * @param order order/cart to inspect
     * @return unique Checkout Session request identifiers
     */
    public static Set<String> collectCheckoutSessionRequestIds(final AbstractOrderModel order) {
        final Set<String> requestIds = new LinkedHashSet<>();
        if (order == null || CollectionUtils.isEmpty(order.getPaymentTransactions())) {
            return requestIds;
        }

        for (final PaymentTransactionModel transaction : order.getPaymentTransactions()) {
            if (transaction == null || CollectionUtils.isEmpty(transaction.getEntries())) {
                continue;
            }
            for (final PaymentTransactionEntryModel entry : transaction.getEntries()) {
                final String requestId = entry == null ? null : entry.getRequestId();
                if (StripePaymentReferenceUtils.isCheckoutSessionReference(requestId)) {
                    requestIds.add(requestId);
                }
            }
        }
        return requestIds;
    }

    /**
     * Resolves currency digits for a payment transaction with SAP's default fallback.
     *
     * @param transaction payment transaction
     * @return currency digits
     */
    public static int resolveCurrencyDigits(final PaymentTransactionModel transaction) {
        if (transaction == null || transaction.getCurrency() == null || transaction.getCurrency().getDigits() == null) {
            return DEFAULT_CURRENCY_DIGITS;
        }
        return transaction.getCurrency().getDigits();
    }
}
