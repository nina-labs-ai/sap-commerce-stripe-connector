package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.data.StripeRefundData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCancelParams;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionExpireParams;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStripePaymentLifecycleServiceTest {

    @InjectMocks
    private DefaultStripePaymentLifecycleService service;

    @Mock
    private StripeClientFactory stripeClientFactory;
    @Mock
    private StripeConfigurationService stripeConfigurationService;
    @Mock
    private StripePaymentTransactionService stripePaymentTransactionService;

    @Test
    public void expireCheckoutSession_registeredSession_expiresAndMarksOrder() throws Exception {
        // Arrange
        final OrderModel order = createOrder("order-expire");
        final Session session = new Session();
        session.setId("cs_test_expire");
        session.setStatus("expired");
        session.setPaymentStatus("unpaid");
        session.setClientReferenceId(order.getCode());
        session.setMetadata(Collections.singletonMap("siteUid", "electronics"));

        when(stripePaymentTransactionService.hasPaymentTransactionEntry(order, "cs_test_expire")).thenReturn(true);
        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_expire")).thenReturn(session);
        when(stripeClientFactory.expireCheckoutSession(eq("sk_test"), eq("cs_test_expire"), any(SessionExpireParams.class)))
                .thenReturn(session);

        // Act
        final StripeCheckoutSessionData result = service.expireCheckoutSession(order, "cs_test_expire");

        // Assert
        assertEquals("cs_test_expire", result.getId());
        verify(stripePaymentTransactionService).markCheckoutSessionExpired(order, result);
    }

    @Test
    public void cancelPaymentIntent_registeredPaymentIntent_cancelsAndMarksOrder() throws Exception {
        // Arrange
        final OrderModel order = createOrder("order-cancel");
        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId("pi_test_cancel");
        paymentIntent.setStatus("canceled");
        paymentIntent.setClientSecret("pi_test_cancel_secret");
        paymentIntent.setCurrency("usd");
        paymentIntent.setAmount(Long.valueOf(1000L));
        paymentIntent.setMetadata(Collections.singletonMap("siteUid", "electronics"));

        when(stripePaymentTransactionService.hasPaymentTransactionEntry(order, "pi_test_cancel")).thenReturn(true);
        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getPaymentIntent("sk_test", "pi_test_cancel")).thenReturn(paymentIntent);
        when(stripeClientFactory.cancelPaymentIntent(eq("sk_test"), eq("pi_test_cancel"), any(PaymentIntentCancelParams.class)))
                .thenReturn(paymentIntent);

        // Act
        final StripePaymentIntentData result = service.cancelPaymentIntent(order, "pi_test_cancel");

        // Assert
        assertEquals("pi_test_cancel", result.getId());
        final ArgumentCaptor<PaymentIntentCancelParams> paramsCaptor = ArgumentCaptor.forClass(PaymentIntentCancelParams.class);
        verify(stripeClientFactory).cancelPaymentIntent(eq("sk_test"), eq("pi_test_cancel"), paramsCaptor.capture());
        verify(stripePaymentTransactionService).markPaymentIntentCancelled(order, result);
        assertEquals(PaymentIntentCancelParams.CancellationReason.REQUESTED_BY_CUSTOMER,
                paramsCaptor.getValue().getCancellationReason());
    }

    @Test(expected = StripeIntegrationException.class)
    public void expireCheckoutSession_wrongSiteMetadata_throwsException() throws Exception {
        // Arrange
        final OrderModel order = createOrder("order-expire");
        final Session session = new Session();
        session.setId("cs_test_expire");
        session.setClientReferenceId(order.getCode());
        session.setMetadata(Collections.singletonMap("siteUid", "apparel"));

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_expire")).thenReturn(session);
        when(stripePaymentTransactionService.hasPaymentTransactionEntry(order, "cs_test_expire")).thenReturn(true);

        // Act
        service.expireCheckoutSession(order, "cs_test_expire");
    }

    @Test(expected = StripeIntegrationException.class)
    public void cancelPaymentIntent_wrongSiteMetadata_throwsException() throws Exception {
        // Arrange
        final OrderModel order = createOrder("order-cancel");
        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId("pi_test_cancel");
        paymentIntent.setMetadata(Collections.singletonMap("siteUid", "apparel"));

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getPaymentIntent("sk_test", "pi_test_cancel")).thenReturn(paymentIntent);
        when(stripePaymentTransactionService.hasPaymentTransactionEntry(order, "pi_test_cancel")).thenReturn(true);

        // Act
        service.cancelPaymentIntent(order, "pi_test_cancel");
    }

    @Test(expected = StripeIntegrationException.class)
    public void expireCheckoutSession_nullOrder_throwsException() {
        // Act
        service.expireCheckoutSession(null, "cs_test_expire");
    }

    @Test(expected = StripeIntegrationException.class)
    public void cancelPaymentIntent_nullOrder_throwsException() {
        // Act
        service.cancelPaymentIntent(null, "pi_test_cancel");
    }

    @Test
    public void createRefund_checkoutSessionReference_resolvesPaymentIntentAndRegistersRefund() throws Exception {
        // Arrange
        final OrderModel order = createOrder("order-refund");
        final Session session = new Session();
        session.setId("cs_test_refund");
        session.setPaymentIntent("pi_test_refund");

        final Refund refund = new Refund();
        refund.setId("re_test_123");
        refund.setPaymentIntent("pi_test_refund");
        refund.setStatus("succeeded");
        refund.setAmount(Long.valueOf(500L));
        refund.setCurrency("usd");

        when(stripePaymentTransactionService.hasPaymentTransactionEntry(order, "cs_test_refund")).thenReturn(true);
        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_refund")).thenReturn(session);
        when(stripeClientFactory.createRefund(eq("sk_test"), any(RefundCreateParams.class))).thenReturn(refund);

        // Act
        final StripeRefundData result = service.createRefund(order, "cs_test_refund", new BigDecimal("5.00"));

        // Assert
        assertEquals("re_test_123", result.getId());
        final ArgumentCaptor<RefundCreateParams> paramsCaptor = ArgumentCaptor.forClass(RefundCreateParams.class);
        verify(stripeClientFactory).createRefund(eq("sk_test"), paramsCaptor.capture());
        verify(stripePaymentTransactionService).registerRefund(order, "cs_test_refund", result);
        assertEquals("pi_test_refund", paramsCaptor.getValue().getPaymentIntent());
        assertEquals(Long.valueOf(500L), paramsCaptor.getValue().getAmount());
        assertTrue(String.valueOf(paramsCaptor.getValue().getMetadata()).contains("paymentFlow=checkout"));
    }

    @Test
    public void createRefund_checkoutBackedPaymentIntentReference_registeredSessionAllowsRefund() throws Exception {
        // Arrange
        final OrderModel order = createOrder("order-checkout-backed-refund");
        order.setPaymentTransactions(Collections.singletonList(createTransaction("cs_test_checkout")));

        final Session session = new Session();
        session.setId("cs_test_checkout");
        session.setPaymentIntent("pi_test_checkout");

        final Refund refund = new Refund();
        refund.setId("re_test_checkout");
        refund.setPaymentIntent("pi_test_checkout");
        refund.setStatus("succeeded");
        refund.setAmount(Long.valueOf(500L));
        refund.setCurrency("usd");

        when(stripePaymentTransactionService.hasPaymentTransactionEntry(order, "pi_test_checkout")).thenReturn(false);
        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_checkout")).thenReturn(session);
        when(stripeClientFactory.createRefund(eq("sk_test"), any(RefundCreateParams.class))).thenReturn(refund);

        // Act
        final StripeRefundData result = service.createRefund(order, "pi_test_checkout", new BigDecimal("5.00"));

        // Assert
        assertEquals("re_test_checkout", result.getId());
        verify(stripePaymentTransactionService).registerRefund(order, "pi_test_checkout", result);
    }

    @Test(expected = StripeIntegrationException.class)
    public void createRefund_unownedReference_throwsException() {
        // Arrange
        final OrderModel order = createOrder("order-unowned");

        when(stripePaymentTransactionService.hasPaymentTransactionEntry(order, "pi_unowned")).thenReturn(false);

        // Act
        service.createRefund(order, "pi_unowned", BigDecimal.ONE);
    }

    protected OrderModel createOrder(final String code) {
        final OrderModel order = new OrderModel();
        order.setCode(code);

        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");
        order.setSite(site);

        final CurrencyModel currency = new CurrencyModel();
        currency.setIsocode("USD");
        currency.setDigits(Integer.valueOf(2));
        order.setCurrency(currency);
        return order;
    }

    protected PaymentTransactionModel createTransaction(final String requestId) {
        final PaymentTransactionEntryModel entry = new PaymentTransactionEntryModel();
        entry.setRequestId(requestId);

        final PaymentTransactionModel transaction = new PaymentTransactionModel();
        transaction.setEntries(Collections.singletonList(entry));
        return transaction;
    }
}
