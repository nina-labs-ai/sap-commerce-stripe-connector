package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.factory.StripeClientFactory;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentIntentUpdateParams;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.CustomerModel;

import java.util.Optional;

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
public class DefaultStripePaymentIntentServiceTest {

    @InjectMocks
    private DefaultStripePaymentIntentService service;

    @Mock
    private StripeClientFactory stripeClientFactory;
    @Mock
    private StripeConfigurationService stripeConfigurationService;
    @Mock
    private StripePaymentTransactionService stripePaymentTransactionService;

    @Test
    public void createOrUpdatePaymentIntent_withoutExistingPaymentIntent_createsAndRegistersIntent() throws Exception {
        // Arrange
        final CartModel cart = createCart("cart-001", 12.5D, 2.5D, false);
        final PaymentIntent paymentIntent = createPaymentIntent("pi_test_123", 1250L, "requires_payment_method");

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripePaymentTransactionService.findLatestOpenPaymentIntentId(cart)).thenReturn(Optional.empty());
        when(stripeClientFactory.createPaymentIntent(eq("sk_test"), any(PaymentIntentCreateParams.class)))
                .thenReturn(paymentIntent);

        // Act
        final StripePaymentIntentData result = service.createOrUpdatePaymentIntent(cart);

        // Assert
        assertEquals("pi_test_123", result.getId());
        assertEquals("pi_test_123_secret_123", result.getClientSecret());

        final ArgumentCaptor<PaymentIntentCreateParams> paramsCaptor = ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
        verify(stripeClientFactory).createPaymentIntent(eq("sk_test"), paramsCaptor.capture());
        verify(stripePaymentTransactionService).registerPaymentIntent(cart, result);
        assertEquals(Long.valueOf(1250L), paramsCaptor.getValue().getAmount());
        assertEquals("usd", paramsCaptor.getValue().getCurrency());
    }

    @Test
    public void createOrUpdatePaymentIntent_existingPaymentIntentNeedsAmountRefresh_updatesIntent() throws Exception {
        // Arrange
        final CartModel cart = createCart("cart-002", 20D, 5D, true);
        final PaymentIntent existingPaymentIntent = createPaymentIntent("pi_test_456", 1500L, "requires_payment_method");
        final PaymentIntent updatedPaymentIntent = createPaymentIntent("pi_test_456", 2500L, "requires_payment_method");

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripePaymentTransactionService.findLatestOpenPaymentIntentId(cart)).thenReturn(Optional.of("pi_test_456"));
        when(stripeClientFactory.getPaymentIntent("sk_test", "pi_test_456")).thenReturn(existingPaymentIntent);
        when(stripeClientFactory.updatePaymentIntent(eq("sk_test"), eq("pi_test_456"), any(PaymentIntentUpdateParams.class)))
                .thenReturn(updatedPaymentIntent);

        // Act
        final StripePaymentIntentData result = service.createOrUpdatePaymentIntent(cart);

        // Assert
        assertEquals(Long.valueOf(2500L), result.getAmount());

        final ArgumentCaptor<PaymentIntentUpdateParams> paramsCaptor = ArgumentCaptor.forClass(PaymentIntentUpdateParams.class);
        verify(stripeClientFactory).updatePaymentIntent(eq("sk_test"), eq("pi_test_456"), paramsCaptor.capture());
        assertEquals(Long.valueOf(2500L), paramsCaptor.getValue().getAmount());
        org.junit.Assert.assertTrue(String.valueOf(paramsCaptor.getValue().getMetadata()).contains("siteUid=electronics"));
    }

    @Test(expected = StripeIntegrationException.class)
    public void getPaymentIntent_unownedPaymentIntent_throwsException() {
        // Arrange
        final CartModel cart = createCart("cart-003", 10D, 2D, false);

        // Act
        service.getPaymentIntent(cart, "pi_unowned", "electronics");
    }

    @Test
    public void getPaymentIntent_ownedPaymentIntent_registersIntentBeforeReturning() throws Exception {
        // Arrange
        final CartModel cart = createCart("cart-001", 10D, 2D, false);
        final PaymentIntent paymentIntent = createPaymentIntent("pi_test_get", 1000L, "requires_payment_method");

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getPaymentIntent("sk_test", "pi_test_get")).thenReturn(paymentIntent);
        when(stripePaymentTransactionService.hasPaymentTransactionEntry(cart, "pi_test_get")).thenReturn(false);

        // Act
        final StripePaymentIntentData result = service.getPaymentIntent(cart, "pi_test_get", "electronics");

        // Assert
        assertEquals("pi_test_get", result.getId());
        verify(stripePaymentTransactionService).registerPaymentIntent(cart, result);
    }

    @Test(expected = StripeIntegrationException.class)
    public void getPaymentIntent_wrongSiteMetadata_throwsException() throws Exception {
        // Arrange
        final CartModel cart = createCart("cart-001", 10D, 2D, false);
        final PaymentIntent paymentIntent = createPaymentIntent("pi_test_get", 1000L, "requires_payment_method");
        paymentIntent.setMetadata(java.util.Map.of("orderCode", "cart-001", "siteUid", "apparel"));

        when(stripeConfigurationService.getSecretKey("electronics")).thenReturn("sk_test");
        when(stripeClientFactory.getPaymentIntent("sk_test", "pi_test_get")).thenReturn(paymentIntent);
        when(stripePaymentTransactionService.hasPaymentTransactionEntry(cart, "pi_test_get")).thenReturn(true);

        // Act
        service.getPaymentIntent(cart, "pi_test_get", "electronics");
    }

    protected CartModel createCart(final String code, final double totalPrice, final double totalTax, final boolean net) {
        final CartModel cart = new CartModel();
        cart.setCode(code);
        cart.setTotalPrice(Double.valueOf(totalPrice));
        cart.setTotalTax(Double.valueOf(totalTax));
        cart.setNet(Boolean.valueOf(net));

        final BaseSiteModel baseSite = new BaseSiteModel();
        baseSite.setUid("electronics");
        cart.setSite(baseSite);

        final CurrencyModel currency = new CurrencyModel();
        currency.setDigits(Integer.valueOf(2));
        currency.setIsocode("USD");
        cart.setCurrency(currency);

        final CustomerModel customer = new CustomerModel();
        customer.setUid("customer@example.com");
        cart.setUser(customer);
        return cart;
    }

    protected PaymentIntent createPaymentIntent(final String id, final long amount, final String status) {
        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId(id);
        paymentIntent.setClientSecret(id + "_secret_123");
        paymentIntent.setAmount(amount);
        paymentIntent.setCurrency("usd");
        paymentIntent.setStatus(status);
        paymentIntent.setMetadata(java.util.Map.of("orderCode", "cart-001", "siteUid", "electronics"));
        return paymentIntent;
    }
}
