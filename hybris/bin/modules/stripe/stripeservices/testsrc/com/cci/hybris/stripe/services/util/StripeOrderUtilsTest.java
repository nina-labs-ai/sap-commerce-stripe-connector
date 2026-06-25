package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UnitTest
public class StripeOrderUtilsTest {

    @Test
    public void validateOrderWithContext_withValidContext_doesNotThrow() {
        final OrderModel order = new OrderModel();
        order.setSite(new BaseSiteModel());
        order.setCurrency(new CurrencyModel());

        StripeOrderUtils.validateOrderWithContext(order, "missing context");
    }

    @Test(expected = StripeIntegrationException.class)
    public void validateOrderWithContext_withNullOrder_throwsException() {
        StripeOrderUtils.validateOrderWithContext(null, "missing context");
    }

    @Test(expected = StripeIntegrationException.class)
    public void validateOrderWithContext_withMissingSite_throwsException() {
        final OrderModel order = new OrderModel();
        order.setCurrency(new CurrencyModel());

        StripeOrderUtils.validateOrderWithContext(order, "missing context");
    }

    @Test(expected = StripeIntegrationException.class)
    public void validateOrderWithContext_withMissingCurrency_throwsException() {
        final OrderModel order = new OrderModel();
        order.setSite(new BaseSiteModel());

        StripeOrderUtils.validateOrderWithContext(order, "missing context");
    }

    @Test
    public void extractCustomerEmail_withContactEmail_returnsContactEmail() {
        final CustomerModel customer = mock(CustomerModel.class);
        when(customer.getUid()).thenReturn("fallback@example.com");
        when(customer.getContactEmail()).thenReturn("contact@example.com");

        final OrderModel order = new OrderModel();
        order.setUser(customer);

        assertEquals("contact@example.com", StripeOrderUtils.extractCustomerEmail(order));
    }

    @Test
    public void extractCustomerEmail_withBlankContactEmail_fallsBackToUid() {
        final CustomerModel customer = mock(CustomerModel.class);
        when(customer.getUid()).thenReturn("fallback@example.com");
        when(customer.getContactEmail()).thenReturn("  ");

        final OrderModel order = new OrderModel();
        order.setUser(customer);

        assertEquals("fallback@example.com", StripeOrderUtils.extractCustomerEmail(order));
    }

    @Test
    public void extractCustomerEmail_withNonCustomerUser_returnsNull() {
        final UserModel user = new UserModel();
        user.setUid("plain-user");

        final OrderModel order = new OrderModel();
        order.setUser(user);

        assertNull(StripeOrderUtils.extractCustomerEmail(order));
    }

    @Test
    public void resolveReturnCartId_withCartGuid_returnsGuid() {
        final CartModel cart = new CartModel();
        cart.setCode("00012345");
        cart.setGuid("cart-guid-123");

        assertEquals("cart-guid-123", StripeOrderUtils.resolveReturnCartId(cart));
    }

    @Test
    public void resolveReturnCartId_withBlankGuid_returnsCartCode() {
        final CartModel cart = new CartModel();
        cart.setCode("00012345");
        cart.setGuid(" ");

        assertEquals("00012345", StripeOrderUtils.resolveReturnCartId(cart));
    }

    @Test
    public void resolveReturnCartId_withOrder_returnsOrderCode() {
        final AbstractOrderModel order = new OrderModel();
        order.setCode("00077777");

        assertEquals("00077777", StripeOrderUtils.resolveReturnCartId(order));
    }
}
