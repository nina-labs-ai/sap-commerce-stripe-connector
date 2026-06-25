package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripePaymentTransactionUtilsTest {

    @Test
    public void findEntry_withMatchingTypeAndRequestId_returnsEntry() {
        final PaymentTransactionEntryModel entry = createEntry("pi_test_123", PaymentTransactionType.AUTHORIZATION,
                StripeServicesConstants.STATUS_PENDING);
        final PaymentTransactionModel transaction = createTransaction(entry);

        assertTrue(StripePaymentTransactionUtils.findEntry(transaction, PaymentTransactionType.AUTHORIZATION, "pi_test_123")
                .isPresent());
    }

    @Test
    public void capturedAndRejectedChecks_withMatchingStatuses_returnTrue() {
        final PaymentTransactionEntryModel capture = createEntry("pi_test_123", PaymentTransactionType.CAPTURE,
                StripeServicesConstants.STATUS_ACCEPTED);
        final PaymentTransactionEntryModel auth = createEntry("pi_test_456", PaymentTransactionType.AUTHORIZATION,
                StripeServicesConstants.STATUS_REJECTED);
        final PaymentTransactionModel transaction = new PaymentTransactionModel();
        transaction.setEntries(java.util.List.of(capture, auth));

        assertTrue(StripePaymentTransactionUtils.isCaptured(transaction, "pi_test_123"));
        assertTrue(StripePaymentTransactionUtils.isRejected(transaction, "pi_test_456"));
        assertFalse(StripePaymentTransactionUtils.isCaptured(transaction, "pi_test_999"));
    }

    @Test
    public void stripeEntryDetection_withSupportedPrefixes_returnsTrue() {
        assertTrue(StripePaymentTransactionUtils.isStripePaymentEntry(
                createEntry("pi_test_123", PaymentTransactionType.AUTHORIZATION, StripeServicesConstants.STATUS_PENDING)));
        assertTrue(StripePaymentTransactionUtils.isStripePaymentEntry(
                createEntry("cs_test_123", PaymentTransactionType.AUTHORIZATION, StripeServicesConstants.STATUS_PENDING)));
        assertFalse(StripePaymentTransactionUtils.isStripePaymentEntry(
                createEntry("other_123", PaymentTransactionType.AUTHORIZATION, StripeServicesConstants.STATUS_PENDING)));
    }

    @Test
    public void collectCheckoutSessionRequestIds_returnsUniqueCheckoutSessionIds() {
        final CartModel cart = new CartModel();
        final PaymentTransactionModel transaction = new PaymentTransactionModel();
        transaction.setEntries(java.util.List.of(
                createEntry("cs_test_123", PaymentTransactionType.AUTHORIZATION, StripeServicesConstants.STATUS_PENDING),
                createEntry("cs_test_123", PaymentTransactionType.CAPTURE, StripeServicesConstants.STATUS_ACCEPTED),
                createEntry("pi_test_123", PaymentTransactionType.AUTHORIZATION, StripeServicesConstants.STATUS_PENDING)));
        cart.setPaymentTransactions(Collections.singletonList(transaction));

        final Set<String> requestIds = StripePaymentTransactionUtils.collectCheckoutSessionRequestIds(cart);
        assertEquals(1, requestIds.size());
        assertTrue(requestIds.contains("cs_test_123"));
    }

    @Test
    public void attachHelpers_andCurrencyDigitsFallback_behaveAsExpected() {
        final CartModel cart = new CartModel();
        final PaymentTransactionModel transaction = new PaymentTransactionModel();
        final PaymentTransactionEntryModel entry = createEntry("cs_test_123", PaymentTransactionType.AUTHORIZATION,
                StripeServicesConstants.STATUS_PENDING);

        StripePaymentTransactionUtils.attachTransaction(cart, transaction);
        StripePaymentTransactionUtils.attachTransaction(cart, transaction);
        assertEquals(1, cart.getPaymentTransactions().size());

        StripePaymentTransactionUtils.attachEntry(transaction, entry);
        StripePaymentTransactionUtils.attachEntry(transaction, entry);
        assertEquals(1, transaction.getEntries().size());

        assertEquals(2, StripePaymentTransactionUtils.resolveCurrencyDigits(transaction));

        final CurrencyModel currency = new CurrencyModel();
        currency.setDigits(Integer.valueOf(3));
        transaction.setCurrency(currency);
        assertEquals(3, StripePaymentTransactionUtils.resolveCurrencyDigits(transaction));
    }

    protected PaymentTransactionModel createTransaction(final PaymentTransactionEntryModel entry) {
        final PaymentTransactionModel transaction = new PaymentTransactionModel();
        transaction.setEntries(Collections.singletonList(entry));
        return transaction;
    }

    protected PaymentTransactionEntryModel createEntry(final String requestId,
                                                       final PaymentTransactionType type,
                                                       final String status) {
        final PaymentTransactionEntryModel entry = new PaymentTransactionEntryModel();
        entry.setRequestId(requestId);
        entry.setType(type);
        entry.setTransactionStatus(status);
        return entry;
    }
}
