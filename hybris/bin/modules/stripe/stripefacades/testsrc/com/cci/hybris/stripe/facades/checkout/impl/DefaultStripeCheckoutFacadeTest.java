package com.cci.hybris.stripe.facades.checkout.impl;

import com.cci.hybris.stripe.facades.data.StripeCheckoutSessionFacadeData;
import com.cci.hybris.stripe.facades.data.StripePublicConfigurationFacadeData;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.service.StripeCheckoutSessionService;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentLifecycleService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.site.BaseSiteService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStripeCheckoutFacadeTest {

    @InjectMocks
    private DefaultStripeCheckoutFacade facade;

    @Mock
    private CartService cartService;
    @Mock
    private BaseSiteService baseSiteService;
    @Mock
    private StripeCheckoutSessionService stripeCheckoutSessionService;
    @Mock
    private StripeConfigurationService stripeConfigurationService;
    @Mock
    private StripePaymentLifecycleService stripePaymentLifecycleService;
    @Mock
    private StripePaymentTransactionService stripePaymentTransactionService;
    @Mock
    private CheckoutFacade checkoutFacade;
    @Mock
    private OrderFacade orderFacade;

    @Test
    public void createCheckoutSessionForCart_sessionCartAvailable_returnsMappedData() {
        // Arrange
        final CartModel cart = new CartModel();
        final StripeCheckoutSessionData sessionData = new StripeCheckoutSessionData();
        sessionData.setId("cs_test_123");
        sessionData.setUrl("https://checkout.stripe.test/session");

        when(cartService.hasSessionCart()).thenReturn(true);
        when(cartService.getSessionCart()).thenReturn(cart);
        when(stripeCheckoutSessionService.createCheckoutSession(cart)).thenReturn(sessionData);

        // Act
        final StripeCheckoutSessionFacadeData result = facade.createCheckoutSessionForCart();

        // Assert
        assertEquals("cs_test_123", result.getId());
        assertEquals("https://checkout.stripe.test/session", result.getUrl());
    }

    @Test
    public void getPublicConfiguration_currentSitePresent_returnsPublishableKeyAndPaymentOption() {
        // Arrange
        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");
        when(baseSiteService.getCurrentBaseSite()).thenReturn(site);
        when(stripeConfigurationService.getPublishableKey("electronics")).thenReturn("pk_test");

        // Act
        final StripePublicConfigurationFacadeData result = facade.getPublicConfiguration();

        // Assert
        assertEquals("pk_test", result.getPublishableKey());
        assertEquals("stripe-checkout", result.getPaymentOptionId());
    }

    @Test
    public void getCheckoutSessionForContext_sessionCartAvailable_returnsMappedData() {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");

        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");

        final StripeCheckoutSessionData sessionData = new StripeCheckoutSessionData();
        sessionData.setId("cs_test_123");

        when(cartService.hasSessionCart()).thenReturn(true);
        when(cartService.getSessionCart()).thenReturn(cart);
        when(baseSiteService.getCurrentBaseSite()).thenReturn(site);
        when(stripeCheckoutSessionService.getCheckoutSession("cs_test_123", "electronics", "cart-001"))
                .thenReturn(sessionData);

        // Act
        final StripeCheckoutSessionFacadeData result = facade.getCheckoutSessionForContext("cs_test_123", null);

        // Assert
        assertEquals("cs_test_123", result.getId());
    }

    @Test
    public void getCheckoutSessionForContext_explicitOrderCodeProvided_skipsSessionCartLookup() {
        // Arrange
        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");

        final StripeCheckoutSessionData sessionData = new StripeCheckoutSessionData();
        sessionData.setId("cs_test_123");

        when(baseSiteService.getCurrentBaseSite()).thenReturn(site);
        when(stripeCheckoutSessionService.getCheckoutSession("cs_test_123", "electronics", "cart-guest-guid"))
                .thenReturn(sessionData);

        // Act
        final StripeCheckoutSessionFacadeData result = facade.getCheckoutSessionForContext("cs_test_123", "cart-guest-guid");

        // Assert
        assertEquals("cs_test_123", result.getId());
    }

    @Test
    public void expireCheckoutSessionForCart_sessionCartAvailable_returnsMappedData() {
        // Arrange
        final CartModel cart = new CartModel();
        final StripeCheckoutSessionData sessionData = new StripeCheckoutSessionData();
        sessionData.setId("cs_test_123");
        sessionData.setStatus("expired");
        sessionData.setPaymentStatus("unpaid");

        when(cartService.hasSessionCart()).thenReturn(true);
        when(cartService.getSessionCart()).thenReturn(cart);
        when(stripePaymentLifecycleService.expireCheckoutSession(cart, "cs_test_123")).thenReturn(sessionData);

        // Act
        final StripeCheckoutSessionFacadeData result = facade.expireCheckoutSessionForCart("cs_test_123");

        // Assert
        assertEquals("cs_test_123", result.getId());
        assertEquals("expired", result.getStatus());
        assertEquals("unpaid", result.getPaymentStatus());
    }

    @Test
    public void finalizeCheckoutSessionForContext_existingOrderPresent_returnsExistingOrderData() throws InvalidCartException {
        // Arrange
        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");

        final StripeCheckoutSessionData sessionData = new StripeCheckoutSessionData();
        sessionData.setId("cs_test_123");
        sessionData.setClientReferenceId("cart-001");
        sessionData.setStatus("complete");
        sessionData.setPaymentStatus("paid");

        final OrderModel order = new OrderModel();
        order.setCode("00001001");

        final OrderData orderData = new OrderData();
        orderData.setCode("00001001");

        when(baseSiteService.getCurrentBaseSite()).thenReturn(site);
        when(cartService.hasSessionCart()).thenReturn(false);
        when(stripeCheckoutSessionService.getCheckoutSession("cs_test_123", "electronics", "cart-001"))
                .thenReturn(sessionData);
        when(stripePaymentTransactionService.findOrderByPaymentReference("cart-001", "cs_test_123"))
                .thenReturn(java.util.Optional.of(order));
        when(orderFacade.getOrderDetailsForCodeWithoutUser("00001001")).thenReturn(orderData);

        // Act
        final OrderData result = facade.finalizeCheckoutSessionForContext("cs_test_123", "cart-001");

        // Assert
        assertEquals("00001001", result.getCode());
    }

    @Test
    public void finalizeCheckoutSessionForContext_paidCartPresent_placesOrderFromResolvedCart() throws InvalidCartException {
        // Arrange
        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");

        final CartModel cart = new CartModel();
        cart.setCode("cart-001");

        final StripeCheckoutSessionData sessionData = new StripeCheckoutSessionData();
        sessionData.setId("cs_test_123");
        sessionData.setClientReferenceId("cart-001");
        sessionData.setStatus("complete");
        sessionData.setPaymentStatus("paid");

        final OrderData orderData = new OrderData();
        orderData.setCode("00001002");

        when(baseSiteService.getCurrentBaseSite()).thenReturn(site);
        when(cartService.hasSessionCart()).thenReturn(false);
        when(stripeCheckoutSessionService.getCheckoutSession("cs_test_123", "electronics", "cart-001"))
                .thenReturn(sessionData);
        when(stripePaymentTransactionService.findOrderByPaymentReference("cart-001", "cs_test_123"))
                .thenReturn(java.util.Optional.of(cart));
        when(checkoutFacade.placeOrder()).thenReturn(orderData);

        // Act
        final OrderData result = facade.finalizeCheckoutSessionForContext("cs_test_123", "cart-001");

        // Assert
        verify(cartService).setSessionCart(cart);
        assertEquals("00001002", result.getCode());
    }

    @Test(expected = com.cci.hybris.stripe.services.exception.StripeIntegrationException.class)
    public void finalizeCheckoutSessionForContext_missingContextCode_throwsException() throws InvalidCartException {
        // Arrange
        when(cartService.hasSessionCart()).thenReturn(false);

        // Act
        facade.finalizeCheckoutSessionForContext("cs_test_123", null);
    }
}
