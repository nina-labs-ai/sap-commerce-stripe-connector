package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.CustomerModel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStripeCheckoutSessionServiceTest {

    @InjectMocks
    private DefaultStripeCheckoutSessionService service;

    @Mock
    private StripeClientFactory stripeClientFactory;
    @Mock
    private StripeConfigurationService stripeConfigurationService;
    @Mock
    private StripePaymentTransactionService stripePaymentTransactionService;
    @Test
    public void createCheckoutSession_validCart_createsSessionAndRegistersTransaction() throws Exception {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");

        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");
        cart.setSite(site);

        final CurrencyModel currency = new CurrencyModel();
        currency.setDigits(Integer.valueOf(2));
        currency.setIsocode("USD");
        cart.setCurrency(currency);
        cart.setNet(Boolean.FALSE);
        cart.setTotalPrice(Double.valueOf(10.00));
        cart.setTotalTax(Double.valueOf(2.34));

        final CustomerModel customer = new CustomerModel();
        customer.setUid("shopper@example.com");
        cart.setUser(customer);

        final Session session = new Session();
        session.setId("cs_test_123");
        session.setUrl("https://checkout.stripe.test/session");
        session.setStatus("open");
        session.setPaymentStatus("unpaid");
        session.setClientReferenceId("cart-001");

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeConfigurationService.getSuccessUrl("electronics")).thenReturn("https://example.com/success?session_id={CHECKOUT_SESSION_ID}");
        when(stripeConfigurationService.getCancelUrl("electronics")).thenReturn("https://example.com/cancel?session_id={CHECKOUT_SESSION_ID}");
        when(stripeClientFactory.createCheckoutSession(eq("sk_test"), any(SessionCreateParams.class))).thenReturn(session);

        // Act
        final StripeCheckoutSessionData result = service.createCheckoutSession(cart);

        // Assert
        assertEquals("cs_test_123", result.getId());
        assertEquals("https://checkout.stripe.test/session", result.getUrl());

        final ArgumentCaptor<SessionCreateParams> paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);
        verify(stripeClientFactory).createCheckoutSession(eq("sk_test"), paramsCaptor.capture());
        verify(stripePaymentTransactionService).registerCheckoutSession(cart, result);
        assertEquals("cart-001", paramsCaptor.getValue().getClientReferenceId());
        assertEquals("shopper@example.com", paramsCaptor.getValue().getCustomerEmail());
        assertEquals("https://example.com/success?session_id={CHECKOUT_SESSION_ID}&cartId=cart-001&orderCode=cart-001",
                paramsCaptor.getValue().getSuccessUrl());
        assertEquals("https://example.com/cancel?session_id={CHECKOUT_SESSION_ID}&cartId=cart-001&orderCode=cart-001",
                paramsCaptor.getValue().getCancelUrl());
        assertEquals(Long.valueOf(1000L),
                paramsCaptor.getValue().getLineItems().get(0).getPriceData().getUnitAmount());
    }

    @Test
    public void getCheckoutSession_matchingSite_returnsSession() throws Exception {
        // Arrange
        final Session session = new Session();
        session.setId("cs_test_get");
        session.setClientReferenceId("cart-001");
        session.setMetadata(java.util.Map.of("siteUid", "electronics"));

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_get")).thenReturn(session);

        // Act
        final StripeCheckoutSessionData result = service.getCheckoutSession("cs_test_get", "electronics", "cart-001");

        // Assert
        assertEquals("cs_test_get", result.getId());
    }

    @Test
    public void getCheckoutSession_registeredSessionWithoutExpectedOrderCode_returnsSession() throws Exception {
        // Arrange
        final Session session = new Session();
        session.setId("cs_test_get");
        session.setClientReferenceId("cart-001");
        session.setMetadata(java.util.Map.of("siteUid", "electronics", "orderCode", "cart-001"));

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_get")).thenReturn(session);
        when(stripePaymentTransactionService.findOrderByPaymentReference("cart-001", "cs_test_get"))
                .thenReturn(java.util.Optional.of(new CartModel()));

        // Act
        final StripeCheckoutSessionData result = service.getCheckoutSession("cs_test_get", "electronics", null);

        // Assert
        assertEquals("cs_test_get", result.getId());
    }

    @Test(expected = StripeIntegrationException.class)
    public void getCheckoutSession_wrongSite_throwsException() throws Exception {
        // Arrange
        final Session session = new Session();
        session.setId("cs_test_get");
        session.setClientReferenceId("cart-001");
        session.setMetadata(java.util.Map.of("siteUid", "apparel"));

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_get")).thenReturn(session);

        // Act
        service.getCheckoutSession("cs_test_get", "electronics", "cart-001");
    }

    @Test(expected = StripeIntegrationException.class)
    public void getCheckoutSession_wrongOrderCode_throwsException() throws Exception {
        // Arrange
        final Session session = new Session();
        session.setId("cs_test_get");
        session.setClientReferenceId("cart-002");
        session.setMetadata(java.util.Map.of("siteUid", "electronics"));

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_get")).thenReturn(session);

        // Act
        service.getCheckoutSession("cs_test_get", "electronics", "cart-001");
    }

    @Test(expected = StripeIntegrationException.class)
    public void getCheckoutSession_unregisteredSessionWithoutExpectedOrderCode_throwsException() throws Exception {
        // Arrange
        final Session session = new Session();
        session.setId("cs_test_get");
        session.setClientReferenceId("cart-002");
        session.setMetadata(java.util.Map.of("siteUid", "electronics", "orderCode", "cart-002"));

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getCheckoutSession("sk_test", "cs_test_get")).thenReturn(session);
        when(stripePaymentTransactionService.findOrderByPaymentReference("cart-002", "cs_test_get"))
                .thenReturn(java.util.Optional.empty());

        // Act
        service.getCheckoutSession("cs_test_get", "electronics", null);
    }
}
