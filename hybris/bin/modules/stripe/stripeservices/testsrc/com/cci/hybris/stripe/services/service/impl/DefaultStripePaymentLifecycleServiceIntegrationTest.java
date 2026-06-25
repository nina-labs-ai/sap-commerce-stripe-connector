package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.data.StripeRefundData;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeCheckoutSessionService;
import com.cci.hybris.stripe.services.service.StripePaymentIntentService;
import com.cci.hybris.stripe.services.service.StripePaymentLifecycleService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;

import de.hybris.bootstrap.annotations.IntegrationTest;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import java.math.BigDecimal;

import org.junit.Test;

import jakarta.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Live servicelayer integration coverage for provider-side Stripe lifecycle operations.
 */
@IntegrationTest
public class DefaultStripePaymentLifecycleServiceIntegrationTest extends AbstractStripeServicesIntegrationTest {

    @Resource
    private StripePaymentLifecycleService stripePaymentLifecycleService;
    @Resource
    private StripeCheckoutSessionService stripeCheckoutSessionService;
    @Resource
    private StripePaymentIntentService stripePaymentIntentService;
    @Resource
    private StripePaymentTransactionService stripePaymentTransactionService;
    @Resource
    private StripeClientFactory stripeClientFactory;

    @Test
    public void expireCheckoutSession_liveSession_marksOrderExpired() {
        final String siteUid = uniqueCode("site-live-expire");
        prepareLiveStripeCheckoutConfiguration(siteUid);
        final OrderModel order = createOrder(uniqueCode("order-live-expire"), siteUid);
        final StripeCheckoutSessionData sessionData = createLiveCheckoutSession(order);

        final StripeCheckoutSessionData expiredSession = stripePaymentLifecycleService.expireCheckoutSession(order, sessionData.getId());

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);

        assertEquals("expired", expiredSession.getStatus());
        assertEquals(OrderStatus.PAYMENT_NOT_CAPTURED, order.getStatus());
        assertEquals("REJECTED",
                findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId()).getTransactionStatus());
    }

    @Test
    public void cancelPaymentIntent_liveIntent_marksOrderNotCapturedAndCreatesCancelEntry() {
        final String siteUid = uniqueCode("site-live-cancel");
        prepareLiveStripeCheckoutConfiguration(siteUid);
        final OrderModel order = createOrder(uniqueCode("order-live-cancel"), siteUid);
        final StripePaymentIntentData paymentIntentData = stripePaymentIntentService.createOrUpdatePaymentIntent(order);

        final StripePaymentIntentData cancelledPaymentIntent = stripePaymentLifecycleService.cancelPaymentIntent(order,
                paymentIntentData.getId());

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);

        assertEquals("canceled", cancelledPaymentIntent.getStatus());
        assertEquals(OrderStatus.PAYMENT_NOT_CAPTURED, order.getStatus());
        assertEquals("ACCEPTED",
                findEntry(transaction, PaymentTransactionType.CANCEL, paymentIntentData.getId()).getTransactionStatus());
    }

    @Test
    public void createRefund_liveCapturedPaymentIntent_createsRefundFollowOnEntry() throws Exception {
        final String siteUid = uniqueCode("site-live-refund");
        prepareLiveStripeCheckoutConfiguration(siteUid);
        final OrderModel order = createOrder(uniqueCode("order-live-refund"), siteUid);
        final String secretKey = getRuntimeProperty(siteUid + "." + StripeServicesConstants.PROPERTY_SECRET_KEY);
        final PaymentIntent paymentIntent = createCapturedPaymentIntent(secretKey, order);
        final StripePaymentIntentData paymentIntentData = toPaymentIntentData(paymentIntent, order.getCode());
        stripePaymentTransactionService.registerPaymentIntent(order, paymentIntentData);
        stripePaymentTransactionService.markPaymentIntentSucceeded(order, paymentIntentData);

        final StripeRefundData refundData = stripePaymentLifecycleService.createRefund(order, paymentIntent.getId(),
                new BigDecimal("5.00"));

        modelService.refresh(order);
        final PaymentTransactionModel transaction = getSingleTransaction(order);
        final PaymentTransactionEntryModel refundEntry = findEntry(transaction, PaymentTransactionType.REFUND_FOLLOW_ON,
                refundData.getId());

        assertNotNull(refundEntry);
        assertEquals(Long.valueOf(500L), refundData.getAmount());
        assertEquals(new BigDecimal("5.00"), refundEntry.getAmount());
        assertEquals(refundData.getStatus(), refundEntry.getTransactionStatusDetails());
    }

    protected StripeCheckoutSessionData createLiveCheckoutSession(final OrderModel order) {
        return stripeCheckoutSessionService.createCheckoutSession(order);
    }

    protected PaymentIntent createCapturedPaymentIntent(final String secretKey, final OrderModel order) throws Exception {
        return stripeClientFactory.createPaymentIntent(secretKey, PaymentIntentCreateParams.builder()
                .setAmount(Long.valueOf(1000L))
                .setCurrency("usd")
                .setConfirm(Boolean.TRUE)
                .setErrorOnRequiresAction(Boolean.TRUE)
                .setPaymentMethod("pm_card_visa")
                .addPaymentMethodType("card")
                .putMetadata(StripeServicesConstants.METADATA_ORDER_CODE, order.getCode())
                .putMetadata(StripeServicesConstants.METADATA_SITE_UID, order.getSite().getUid())
                .putMetadata(StripeServicesConstants.METADATA_ORDER_TYPE, order.getClass().getSimpleName())
                .putMetadata(StripeServicesConstants.METADATA_PAYMENT_FLOW, StripeServicesConstants.PAYMENT_FLOW_ELEMENTS)
                .build());
    }

    protected StripePaymentIntentData toPaymentIntentData(final PaymentIntent paymentIntent, final String orderCode) {
        final StripePaymentIntentData data = new StripePaymentIntentData();
        data.setId(paymentIntent.getId());
        data.setClientSecret(paymentIntent.getClientSecret());
        data.setStatus(paymentIntent.getStatus());
        data.setAmount(paymentIntent.getAmount());
        data.setCurrency(paymentIntent.getCurrency());
        data.setClientReferenceId(orderCode);
        return data;
    }
}
