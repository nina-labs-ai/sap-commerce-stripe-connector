package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.OrderModel;

import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStripeWebhookServiceTest {

    @Mock
    private StripeClientFactory stripeClientFactory;
    @Mock
    private StripeConfigurationService stripeConfigurationService;
    @Mock
    private StripePaymentTransactionService stripePaymentTransactionService;

    @Test
    public void handleWebhook_completedPaidSession_marksOrderCompleted() throws Exception {
        // Arrange
        final String payload = "{\"data\":{\"object\":{\"metadata\":{\"siteUid\":\"electronics\"}}}}";
        final Event event = new Event();
        event.setType("checkout.session.completed");

        final Session session = new Session();
        session.setId("cs_test_123");
        session.setStatus("complete");
        session.setPaymentStatus("paid");
        session.setClientReferenceId("order-001");
        session.setMetadata(Map.of("orderCode", "order-001", "siteUid", "electronics", "paymentFlow", "checkout"));

        final OrderModel order = new OrderModel();
        order.setCode("order-001");

        final DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                stripeClientFactory, stripeConfigurationService, stripePaymentTransactionService) {
            @Override
            protected Optional<Session> extractSession(final Event stripeEvent) {
                return Optional.of(session);
            }
        };

        when(stripeConfigurationService.getWebhookSecret("electronics")).thenReturn("webhook-secret-test");
        when(stripeClientFactory.constructEvent(payload, "signature", "webhook-secret-test")).thenReturn(event);
        when(stripePaymentTransactionService.findOrderByPaymentReference("order-001", "cs_test_123"))
                .thenReturn(Optional.of(order));

        // Act
        service.handleWebhook(payload, "signature", null);

        // Assert
        verify(stripeConfigurationService).getWebhookSecret("electronics");
        verify(stripePaymentTransactionService).markCheckoutSessionCompleted(eq(order), any());
    }

    @Test
    public void handleWebhook_paymentIntentSucceeded_marksOrderCompleted() throws Exception {
        // Arrange
        final String payload = "{\"data\":{\"object\":{\"metadata\":{\"siteUid\":\"electronics\"}}}}";
        final Event event = new Event();
        event.setType("payment_intent.succeeded");

        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId("pi_test_123");
        paymentIntent.setStatus("succeeded");
        paymentIntent.setAmount(1999L);
        paymentIntent.setCurrency("usd");
        paymentIntent.setMetadata(Map.of("orderCode", "order-002", "siteUid", "electronics", "paymentFlow", "elements"));

        final OrderModel order = new OrderModel();
        order.setCode("order-002");

        final DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                stripeClientFactory, stripeConfigurationService, stripePaymentTransactionService) {
            @Override
            protected Optional<PaymentIntent> extractPaymentIntent(final Event stripeEvent) {
                return Optional.of(paymentIntent);
            }
        };

        when(stripeConfigurationService.getWebhookSecret("electronics")).thenReturn("webhook-secret-test");
        when(stripeClientFactory.constructEvent(payload, "signature", "webhook-secret-test")).thenReturn(event);
        when(stripePaymentTransactionService.findOrderByPaymentReference("order-002", "pi_test_123"))
                .thenReturn(Optional.of(order));

        // Act
        service.handleWebhook(payload, "signature", null);

        // Assert
        verify(stripeConfigurationService).getWebhookSecret("electronics");
        verify(stripePaymentTransactionService).markPaymentIntentSucceeded(eq(order), any());
    }

    @Test
    public void handleWebhook_paymentIntentForWrongSite_ignoresEvent() throws Exception {
        // Arrange
        final Event event = new Event();
        event.setType("payment_intent.succeeded");

        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId("pi_test_999");
        paymentIntent.setStatus("succeeded");
        paymentIntent.setMetadata(Map.of("orderCode", "order-003", "siteUid", "apparel", "paymentFlow", "elements"));

        final DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                stripeClientFactory, stripeConfigurationService, stripePaymentTransactionService) {
            @Override
            protected Optional<PaymentIntent> extractPaymentIntent(final Event stripeEvent) {
                return Optional.of(paymentIntent);
            }
        };

        when(stripeConfigurationService.getWebhookSecret("electronics")).thenReturn("webhook-secret-test");
        when(stripeClientFactory.constructEvent("payload", "signature", "webhook-secret-test")).thenReturn(event);

        // Act
        service.handleWebhook("payload", "signature", "electronics");

        // Assert
        verify(stripePaymentTransactionService, never()).markPaymentIntentSucceeded(any(), any());
        verify(stripePaymentTransactionService, never()).markPaymentIntentFailed(any(), any());
    }

    @Test
    public void handleWebhook_checkoutCompletedWithMismatchedRequestedSite_ignoresEvent() throws Exception {
        // Arrange
        final Event event = new Event();
        event.setType("checkout.session.completed");

        final Session session = new Session();
        session.setId("cs_test_456");
        session.setStatus("complete");
        session.setPaymentStatus("paid");
        session.setClientReferenceId("order-004");
        session.setMetadata(Map.of("orderCode", "order-004", "siteUid", "apparel-uk", "paymentFlow", "checkout"));

        final DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                stripeClientFactory, stripeConfigurationService, stripePaymentTransactionService) {
            @Override
            protected Optional<Session> extractSession(final Event stripeEvent) {
                return Optional.of(session);
            }
        };

        when(stripeConfigurationService.getWebhookSecret("electronics")).thenReturn("webhook-secret-test");
        when(stripeClientFactory.constructEvent("payload", "signature", "webhook-secret-test")).thenReturn(event);

        // Act
        service.handleWebhook("payload", "signature", "electronics");

        // Assert
        verify(stripePaymentTransactionService, never()).markCheckoutSessionCompleted(any(), any());
    }

    @Test
    public void handleWebhook_withoutSiteMetadata_usesGlobalWebhookSecret() throws Exception {
        // Arrange
        final Event event = new Event();
        event.setType("customer.created");

        final DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                stripeClientFactory, stripeConfigurationService, stripePaymentTransactionService);

        when(stripeConfigurationService.getWebhookSecret(null)).thenReturn("global-webhook-secret");
        when(stripeClientFactory.constructEvent("{\"data\":{\"object\":{}}}", "signature", "global-webhook-secret"))
                .thenReturn(event);

        // Act
        service.handleWebhook("{\"data\":{\"object\":{}}}", "signature", null);

        // Assert
        verify(stripeConfigurationService).getWebhookSecret(null);
    }

    @Test(expected = StripeIntegrationException.class)
    public void handleWebhook_checkoutSessionEventWithoutExtractableSession_throwsException() throws Exception {
        // Arrange
        final Event event = new Event();
        event.setId("evt_checkout");
        event.setType("checkout.session.completed");

        final DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                stripeClientFactory, stripeConfigurationService, stripePaymentTransactionService);

        when(stripeConfigurationService.getWebhookSecret("electronics")).thenReturn("webhook-secret-test");
        when(stripeClientFactory.constructEvent("payload", "signature", "webhook-secret-test")).thenReturn(event);

        // Act
        service.handleWebhook("payload", "signature", "electronics");
    }

    @Test(expected = StripeIntegrationException.class)
    public void handleWebhook_paymentIntentEventWithoutExtractablePaymentIntent_throwsException() throws Exception {
        // Arrange
        final Event event = new Event();
        event.setId("evt_payment_intent");
        event.setType("payment_intent.succeeded");

        final DefaultStripeWebhookService service = new DefaultStripeWebhookService(
                stripeClientFactory, stripeConfigurationService, stripePaymentTransactionService);

        when(stripeConfigurationService.getWebhookSecret("electronics")).thenReturn("webhook-secret-test");
        when(stripeClientFactory.constructEvent("payload", "signature", "webhook-secret-test")).thenReturn(event);

        // Act
        service.handleWebhook("payload", "signature", "electronics");
    }
}
