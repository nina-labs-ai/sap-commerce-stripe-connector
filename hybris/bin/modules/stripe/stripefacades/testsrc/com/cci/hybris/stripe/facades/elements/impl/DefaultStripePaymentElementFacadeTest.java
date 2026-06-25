package com.cci.hybris.stripe.facades.elements.impl;

import com.cci.hybris.stripe.facades.data.StripePaymentElementFacadeData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.service.StripePaymentLifecycleService;
import com.cci.hybris.stripe.services.service.StripePaymentIntentService;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.opffacades.calculators.CalculatorStrategy;
import de.hybris.platform.order.CartService;
import de.hybris.platform.site.BaseSiteService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.Silent.class)
public class DefaultStripePaymentElementFacadeTest {

    private static final String SITE_UID = "electronics";
    private static final String RETURN_URL = "https://example.com/return";
    private static final String FORMATTED_AMOUNT = "$12.00";
    private static final String PUBLISHABLE_KEY = "pk_test";

    @InjectMocks
    private DefaultStripePaymentElementFacade facade;

    @Mock
    private CartService cartService;
    @Mock
    private BaseSiteService baseSiteService;
    @Mock
    private StripePaymentIntentService stripePaymentIntentService;
    @Mock
    private StripeConfigurationService stripeConfigurationService;
    @Mock
    private CalculatorStrategy calculatorStrategy;
    @Mock
    private StripePaymentLifecycleService stripePaymentLifecycleService;
    @Mock
    private StripePaymentTransactionService stripePaymentTransactionService;
    @Mock
    private CheckoutFacade checkoutFacade;
    @Mock
    private OrderFacade orderFacade;
    @Mock
    private PriceData priceData;

    @Test
    public void createPaymentIntentForCart_validSessionCart_mapsFacadeData() {
        // Arrange
        final CartModel cart = createGrossCart();
        final StripePaymentIntentData paymentIntentData = createPaymentIntent("pi_test_123", "requires_payment_method");
        stubSessionCartContext(cart);
        when(stripePaymentIntentService.createOrUpdatePaymentIntent(cart)).thenReturn(paymentIntentData);
        stubDisplayConfig(cart);

        // Act
        final StripePaymentElementFacadeData result = facade.createPaymentIntentForCart();

        // Assert
        assertEquals("pi_test_123", result.getId());
        assertEquals(PUBLISHABLE_KEY, result.getPublishableKey());
        assertEquals(FORMATTED_AMOUNT, result.getFormattedAmount());
        assertEquals(RETURN_URL, result.getReturnUrl());
    }

    @Test
    public void getPaymentIntent_validSessionCart_usesCartScopedLookup() {
        // Arrange
        final CartModel cart = createGrossCart();
        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId("pi_test_123");
        paymentIntentData.setClientSecret("pi_test_123_secret_123");

        stubSessionCartContext(cart);
        when(stripePaymentIntentService.getPaymentIntent(cart, "pi_test_123", SITE_UID)).thenReturn(paymentIntentData);
        stubDisplayConfig(cart);

        // Act
        final StripePaymentElementFacadeData result = facade.getPaymentIntent("pi_test_123");

        // Assert
        assertEquals("pi_test_123", result.getId());
        assertEquals(FORMATTED_AMOUNT, result.getFormattedAmount());
        verify(stripePaymentIntentService).getPaymentIntent(cart, "pi_test_123", SITE_UID);
    }

    @Test
    public void cancelPaymentIntentForCart_validSessionCart_returnsMappedLifecycleData() {
        // Arrange
        final CartModel cart = createGrossCart();

        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId("pi_test_123");
        paymentIntentData.setStatus("canceled");
        paymentIntentData.setClientReferenceId("cart-001");

        stubSessionCartContext(cart);
        when(stripePaymentLifecycleService.cancelPaymentIntent(cart, "pi_test_123")).thenReturn(paymentIntentData);
        stubDisplayConfig(cart);

        // Act
        final StripePaymentElementFacadeData result = facade.cancelPaymentIntentForCart("pi_test_123");

        // Assert
        assertEquals("pi_test_123", result.getId());
        assertEquals("canceled", result.getStatus());
        assertEquals(FORMATTED_AMOUNT, result.getFormattedAmount());
        verify(stripePaymentLifecycleService).cancelPaymentIntent(cart, "pi_test_123");
    }

    @Test
    public void finalizePaymentIntentForContext_paidCartPresent_placesOrderFromResolvedCart() throws Exception {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setSite(createBaseSite("electronics"));

        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId("pi_test_123");
        paymentIntentData.setStatus("succeeded");

        final OrderData orderData = new OrderData();
        orderData.setCode("order-001");

        when(cartService.hasSessionCart()).thenReturn(false);
        when(baseSiteService.getCurrentBaseSite()).thenReturn(createBaseSite("electronics"));
        when(stripePaymentTransactionService.findOrderByPaymentReference("pi_test_123"))
                .thenReturn(java.util.Optional.empty());
        when(stripePaymentTransactionService.findOrderByPaymentReference("cart-001", "pi_test_123"))
                .thenReturn(java.util.Optional.of(cart));
        when(stripePaymentIntentService.getPaymentIntent(cart, "pi_test_123", "electronics"))
                .thenReturn(paymentIntentData);
        when(checkoutFacade.placeOrder()).thenReturn(orderData);

        // Act
        final OrderData result = facade.finalizePaymentIntentForContext("pi_test_123", "cart-001");

        // Assert
        assertEquals("order-001", result.getCode());
        verify(stripePaymentTransactionService).markPaymentIntentSucceeded(cart, paymentIntentData);
        verify(cartService).setSessionCart(cart);
    }

    @Test
    public void finalizePaymentIntentForContext_loadedSessionCartWithMatchingPaymentIntent_placesOrder() throws Exception {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setSite(createBaseSite("electronics"));

        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId("pi_test_guid");
        paymentIntentData.setStatus("succeeded");

        final OrderData orderData = new OrderData();
        orderData.setCode("order-001");

        when(cartService.hasSessionCart()).thenReturn(true);
        when(cartService.getSessionCart()).thenReturn(cart);
        when(baseSiteService.getCurrentBaseSite()).thenReturn(createBaseSite("electronics"));
        when(stripePaymentTransactionService.hasPaymentTransactionEntry(cart, "pi_test_guid")).thenReturn(true);
        when(stripePaymentIntentService.getPaymentIntent(cart, "pi_test_guid", "electronics"))
                .thenReturn(paymentIntentData);
        when(checkoutFacade.placeOrder()).thenReturn(orderData);

        // Act
        final OrderData result = facade.finalizePaymentIntentForContext("pi_test_guid",
                "a73e751e-650b-4edd-9e8f-363164dd8b46");

        // Assert
        assertEquals("order-001", result.getCode());
        verify(stripePaymentTransactionService).hasPaymentTransactionEntry(cart, "pi_test_guid");
        verify(stripePaymentTransactionService).markPaymentIntentSucceeded(cart, paymentIntentData);
        verify(cartService).setSessionCart(cart);
    }

    @Test
    public void finalizePaymentIntentForContext_loadedSessionCartMatchingGuid_placesOrderWithoutPreexistingEntry()
            throws Exception {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setGuid("a73e751e-650b-4edd-9e8f-363164dd8b46");
        cart.setSite(createBaseSite("electronics"));

        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId("pi_test_guid_context");
        paymentIntentData.setStatus("succeeded");

        final OrderData orderData = new OrderData();
        orderData.setCode("order-001");

        when(cartService.hasSessionCart()).thenReturn(true);
        when(cartService.getSessionCart()).thenReturn(cart);
        when(baseSiteService.getCurrentBaseSite()).thenReturn(createBaseSite("electronics"));
        when(stripePaymentTransactionService.hasPaymentTransactionEntry(cart, "pi_test_guid_context")).thenReturn(false);
        when(stripePaymentIntentService.getPaymentIntent(cart, "pi_test_guid_context", "electronics"))
                .thenReturn(paymentIntentData);
        when(checkoutFacade.placeOrder()).thenReturn(orderData);

        // Act
        final OrderData result = facade.finalizePaymentIntentForContext("pi_test_guid_context",
                "a73e751e-650b-4edd-9e8f-363164dd8b46");

        // Assert
        assertEquals("order-001", result.getCode());
        verify(stripePaymentTransactionService).hasPaymentTransactionEntry(cart, "pi_test_guid_context");
        verify(stripePaymentTransactionService).markPaymentIntentSucceeded(cart, paymentIntentData);
        verify(cartService).setSessionCart(cart);
    }

    @Test
    public void finalizePaymentIntentForContext_expiredAnonymousCartFallsBackToPaymentIntentOwnership() throws Exception {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setSite(createBaseSite("electronics"));

        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId("pi_test_expired");
        paymentIntentData.setStatus("succeeded");

        final OrderData orderData = new OrderData();
        orderData.setCode("order-001");

        lenient().when(cartService.hasSessionCart()).thenReturn(false);
        lenient().when(baseSiteService.getCurrentBaseSite()).thenReturn(createBaseSite("electronics"));
        lenient().when(stripePaymentTransactionService.findOrderByPaymentReference("pi_test_expired"))
                .thenReturn(java.util.Optional.of(cart));
        lenient().when(stripePaymentIntentService.getPaymentIntent(cart, "pi_test_expired", "electronics"))
                .thenReturn(paymentIntentData);
        lenient().when(checkoutFacade.placeOrder()).thenReturn(orderData);

        // Act
        final OrderData result = facade.finalizePaymentIntentForContext("pi_test_expired",
                "2e59f800-8c64-4196-b017-cb774dee88f3");

        // Assert
        assertEquals("order-001", result.getCode());
        verify(stripePaymentTransactionService).findOrderByPaymentReference("pi_test_expired");
        verify(stripePaymentTransactionService).markPaymentIntentSucceeded(cart, paymentIntentData);
        verify(cartService).setSessionCart(cart);
    }

    @Test
    public void finalizePaymentIntentForContext_cartGuidFallsBackToCodeLookupBeforeStripeValidation() throws Exception {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setSite(createBaseSite("electronics"));

        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId("pi_test_guid_lookup");
        paymentIntentData.setStatus("succeeded");

        final OrderData orderData = new OrderData();
        orderData.setCode("order-001");

        when(cartService.hasSessionCart()).thenReturn(false);
        when(baseSiteService.getCurrentBaseSite()).thenReturn(createBaseSite("electronics"));
        when(stripePaymentTransactionService.findOrderByPaymentReference("pi_test_guid_lookup"))
                .thenReturn(java.util.Optional.empty());
        when(stripePaymentTransactionService.findOrderByPaymentReference(
                "2e59f800-8c64-4196-b017-cb774dee88f3", "pi_test_guid_lookup"))
                .thenReturn(java.util.Optional.empty());
        when(stripePaymentTransactionService.findOrderByCode("2e59f800-8c64-4196-b017-cb774dee88f3"))
                .thenReturn(java.util.Optional.of(cart));
        when(stripePaymentIntentService.getPaymentIntent(cart, "pi_test_guid_lookup", "electronics"))
                .thenReturn(paymentIntentData);
        when(checkoutFacade.placeOrder()).thenReturn(orderData);

        // Act
        final OrderData result = facade.finalizePaymentIntentForContext("pi_test_guid_lookup",
                "2e59f800-8c64-4196-b017-cb774dee88f3");

        // Assert
        assertEquals("order-001", result.getCode());
        verify(stripePaymentTransactionService).findOrderByCode("2e59f800-8c64-4196-b017-cb774dee88f3");
        verify(stripePaymentTransactionService).markPaymentIntentSucceeded(cart, paymentIntentData);
        verify(cartService).setSessionCart(cart);
    }

    @Test
    public void finalizePaymentIntentForContext_usesStripeMetadataWhenLocalOwnershipLookupFails() throws Exception {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setSite(createBaseSite("electronics"));

        final StripePaymentIntentData metadataPaymentIntent = new StripePaymentIntentData();
        metadataPaymentIntent.setId("pi_test_metadata");
        metadataPaymentIntent.setClientReferenceId("cart-001");

        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId("pi_test_metadata");
        paymentIntentData.setStatus("succeeded");

        final OrderData orderData = new OrderData();
        orderData.setCode("order-001");

        when(cartService.hasSessionCart()).thenReturn(false);
        when(baseSiteService.getCurrentBaseSite()).thenReturn(createBaseSite("electronics"));
        when(stripePaymentTransactionService.findOrderByPaymentReference("pi_test_metadata"))
                .thenReturn(java.util.Optional.empty());
        when(stripePaymentTransactionService.findOrderByPaymentReference("missing-guid", "pi_test_metadata"))
                .thenReturn(java.util.Optional.empty());
        when(stripePaymentTransactionService.findOrderByCode("missing-guid"))
                .thenReturn(java.util.Optional.empty());
        when(stripePaymentIntentService.getPaymentIntentForSite("pi_test_metadata", "electronics"))
                .thenReturn(metadataPaymentIntent);
        when(stripePaymentTransactionService.findOrderByCode("cart-001"))
                .thenReturn(java.util.Optional.of(cart));
        when(stripePaymentIntentService.getPaymentIntent(cart, "pi_test_metadata", "electronics"))
                .thenReturn(paymentIntentData);
        when(checkoutFacade.placeOrder()).thenReturn(orderData);

        // Act
        final OrderData result = facade.finalizePaymentIntentForContext("pi_test_metadata", "missing-guid");

        // Assert
        assertEquals("order-001", result.getCode());
        verify(stripePaymentIntentService).getPaymentIntentForSite("pi_test_metadata", "electronics");
        verify(stripePaymentTransactionService).findOrderByCode("cart-001");
        verify(stripePaymentTransactionService).markPaymentIntentSucceeded(cart, paymentIntentData);
        verify(cartService).setSessionCart(cart);
    }

    protected BaseSiteModel createBaseSite(final String uid) {
        final BaseSiteModel baseSite = new BaseSiteModel();
        baseSite.setUid(uid);
        return baseSite;
    }

    protected CartModel createGrossCart() {
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setTotalPrice(Double.valueOf(12D));
        cart.setTotalTax(Double.valueOf(3D));
        cart.setNet(Boolean.FALSE);
        return cart;
    }

    protected StripePaymentIntentData createPaymentIntent(final String id, final String status) {
        final StripePaymentIntentData paymentIntentData = new StripePaymentIntentData();
        paymentIntentData.setId(id);
        paymentIntentData.setClientSecret(id + "_secret_123");
        paymentIntentData.setStatus(status);
        paymentIntentData.setAmount(Long.valueOf(1500L));
        paymentIntentData.setCurrency("usd");
        return paymentIntentData;
    }

    protected void stubSessionCartContext(final CartModel cart) {
        when(cartService.hasSessionCart()).thenReturn(true);
        when(cartService.getSessionCart()).thenReturn(cart);
        when(baseSiteService.getCurrentBaseSite()).thenReturn(createBaseSite(SITE_UID));
    }

    protected void stubDisplayConfig(final CartModel cart) {
        when(stripeConfigurationService.getPublishableKey(SITE_UID)).thenReturn(PUBLISHABLE_KEY);
        when(stripeConfigurationService.getElementsReturnUrl(SITE_UID)).thenReturn(RETURN_URL);
        when(calculatorStrategy.createPrice(cart, Double.valueOf(12D))).thenReturn(priceData);
        when(priceData.getFormattedValue()).thenReturn(FORMATTED_AMOUNT);
    }
}
