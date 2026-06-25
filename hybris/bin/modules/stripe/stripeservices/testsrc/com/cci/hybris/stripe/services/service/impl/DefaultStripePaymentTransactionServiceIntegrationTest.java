package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.data.StripeRefundData;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import java.util.Optional;

import org.junit.Test;

import jakarta.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Servicelayer integration tests for Stripe payment transaction persistence.
 */
@IntegrationTest
public class DefaultStripePaymentTransactionServiceIntegrationTest extends AbstractStripeServicesIntegrationTest {

    @Resource
    private StripePaymentTransactionService stripePaymentTransactionService;

    @Test
    public void registerCheckoutSession_orderWithoutTransactions_persistsAuthorizationEntry() {
        final OrderModel order = createOrder(uniqueCode("order-register"), uniqueCode("site-register"));
        final StripeCheckoutSessionData sessionData = createSessionData("cs_register");

        stripePaymentTransactionService.registerCheckoutSession(order, sessionData);

        final PaymentTransactionModel transaction = getSingleTransaction(order);
        final PaymentTransactionEntryModel authorization = findEntry(transaction, PaymentTransactionType.AUTHORIZATION,
                sessionData.getId());

        assertNotNull(authorization);
        assertEquals("PENDING", authorization.getTransactionStatus());
    }

    @Test
    public void markCheckoutSessionCompleted_registeredAuthorization_updatesOrderAndCaptureState() {
        final OrderModel order = createOrder(uniqueCode("order-complete"), uniqueCode("site-complete"));
        final StripeCheckoutSessionData sessionData = createSessionData("cs_complete");
        stripePaymentTransactionService.registerCheckoutSession(order, sessionData);

        stripePaymentTransactionService.markCheckoutSessionCompleted(order, sessionData);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals("ACCEPTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId()).getTransactionStatus());
        assertEquals("ACCEPTED",
                findEntry(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()).getTransactionStatus());
    }

    @Test
    public void markCheckoutSessionExpired_registeredAuthorization_updatesOrderAndLookupState() {
        final OrderModel order = createOrder(uniqueCode("order-expire"), uniqueCode("site-expire"));
        final StripeCheckoutSessionData sessionData = createSessionData("cs_expired");
        sessionData.setStatus("expired");
        stripePaymentTransactionService.registerCheckoutSession(order, sessionData);

        stripePaymentTransactionService.markCheckoutSessionExpired(order, sessionData);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        final Optional<AbstractOrderModel> orderByCode = stripePaymentTransactionService.findOrderByCode(order.getCode());
        final Optional<AbstractOrderModel> orderByPaymentReference = stripePaymentTransactionService
                .findOrderByPaymentReference(order.getCode(), sessionData.getId());

        assertEquals(OrderStatus.PAYMENT_NOT_CAPTURED, order.getStatus());
        assertEquals("REJECTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId()).getTransactionStatus());
        assertTrue(orderByCode.isPresent());
        assertTrue(orderByPaymentReference.isPresent());
    }

    @Test
    public void markCheckoutSessionCompleted_duplicateCall_keepsSingleCaptureEntry() {
        final OrderModel order = createOrder(uniqueCode("order-complete-duplicate"), uniqueCode("site-complete-duplicate"));
        final StripeCheckoutSessionData sessionData = createSessionData("cs_complete_duplicate");
        stripePaymentTransactionService.registerCheckoutSession(order, sessionData);

        stripePaymentTransactionService.markCheckoutSessionCompleted(order, sessionData);
        stripePaymentTransactionService.markCheckoutSessionCompleted(order, sessionData);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);

        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()));
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId()));
    }

    @Test
    public void markCheckoutSessionExpired_afterCompletion_doesNotDowngradeCapturedOrder() {
        final OrderModel order = createOrder(uniqueCode("order-expired-after-complete"), uniqueCode("site-expired-after-complete"));
        final StripeCheckoutSessionData sessionData = createSessionData("cs_expired_after_complete");
        stripePaymentTransactionService.registerCheckoutSession(order, sessionData);
        stripePaymentTransactionService.markCheckoutSessionCompleted(order, sessionData);
        sessionData.setStatus("expired");

        stripePaymentTransactionService.markCheckoutSessionExpired(order, sessionData);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);

        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals("ACCEPTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId()).getTransactionStatus());
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.CAPTURE, sessionData.getId()));
    }

    @Test
    public void markPaymentIntentFailed_afterSuccess_doesNotDowngradeCapturedOrder() {
        final OrderModel order = createOrder(uniqueCode("order-pi-failed-after-success"), uniqueCode("site-pi-failed-after-success"));
        final StripePaymentIntentData paymentIntentData = createPaymentIntentData("pi_success_then_failed");
        stripePaymentTransactionService.registerPaymentIntent(order, paymentIntentData);
        stripePaymentTransactionService.markPaymentIntentSucceeded(order, paymentIntentData);
        paymentIntentData.setStatus("canceled");

        stripePaymentTransactionService.markPaymentIntentFailed(order, paymentIntentData);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);

        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals("ACCEPTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, paymentIntentData.getId()).getTransactionStatus());
        assertEquals(1L, countEntries(transaction, PaymentTransactionType.CAPTURE, paymentIntentData.getId()));
    }

    @Test
    public void markPaymentIntentCancelled_registeredAuthorization_createsCancelEntry() {
        final OrderModel order = createOrder(uniqueCode("order-pi-cancel"), uniqueCode("site-pi-cancel"));
        final StripePaymentIntentData paymentIntentData = createPaymentIntentData("pi_cancelled");
        paymentIntentData.setStatus("canceled");
        stripePaymentTransactionService.registerPaymentIntent(order, paymentIntentData);

        stripePaymentTransactionService.markPaymentIntentCancelled(order, paymentIntentData);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);

        assertEquals(OrderStatus.PAYMENT_NOT_CAPTURED, order.getStatus());
        assertEquals("REJECTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, paymentIntentData.getId()).getTransactionStatus());
        assertEquals("ACCEPTED",
                findEntry(transaction, PaymentTransactionType.CANCEL, paymentIntentData.getId()).getTransactionStatus());
    }

    @Test
    public void registerRefund_successfulRefund_createsRefundFollowOnEntry() {
        final OrderModel order = createOrder(uniqueCode("order-refund"), uniqueCode("site-refund"));
        final StripePaymentIntentData paymentIntentData = createPaymentIntentData("pi_refund_source");
        stripePaymentTransactionService.registerPaymentIntent(order, paymentIntentData);
        stripePaymentTransactionService.markPaymentIntentSucceeded(order, paymentIntentData);
        final StripeRefundData refundData = createRefundData("re_test_refund", paymentIntentData.getId(), "succeeded", 500L);

        stripePaymentTransactionService.registerRefund(order, paymentIntentData.getId(), refundData);

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        final PaymentTransactionEntryModel refundEntry = findEntry(transaction, PaymentTransactionType.REFUND_FOLLOW_ON,
                refundData.getId());

        assertNotNull(refundEntry);
        assertEquals("ACCEPTED", refundEntry.getTransactionStatus());
        assertEquals("succeeded", refundEntry.getTransactionStatusDetails());
        assertEquals(paymentIntentData.getId(), refundEntry.getRequestToken());
    }

    protected StripeCheckoutSessionData createSessionData(final String sessionId) {
        final StripeCheckoutSessionData sessionData = new StripeCheckoutSessionData();
        sessionData.setId(sessionId);
        sessionData.setStatus("open");
        sessionData.setPaymentStatus("unpaid");
        sessionData.setClientReferenceId(sessionId);
        sessionData.setUrl("https://example.com/checkout/" + sessionId);
        return sessionData;
    }

    protected StripePaymentIntentData createPaymentIntentData(final String paymentIntentId) {
        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId(paymentIntentId);
        paymentIntentData.setStatus("requires_payment_method");
        paymentIntentData.setCurrency("usd");
        paymentIntentData.setAmount(Long.valueOf(1000L));
        paymentIntentData.setClientReferenceId(paymentIntentId);
        return paymentIntentData;
    }

    protected StripeRefundData createRefundData(final String refundId,
                                                final String paymentIntentId,
                                                final String status,
                                                final long amount) {
        final StripeRefundData refundData = new StripeRefundData();
        refundData.setId(refundId);
        refundData.setPaymentIntentId(paymentIntentId);
        refundData.setStatus(status);
        refundData.setAmount(Long.valueOf(amount));
        refundData.setCurrency("usd");
        return refundData;
    }

    protected long countEntries(final PaymentTransactionModel transaction,
                                final PaymentTransactionType type,
                                final String requestId) {
        return transaction.getEntries().stream()
                .filter(entry -> type.equals(entry.getType()) && requestId.equals(entry.getRequestId()))
                .count();
    }
}
