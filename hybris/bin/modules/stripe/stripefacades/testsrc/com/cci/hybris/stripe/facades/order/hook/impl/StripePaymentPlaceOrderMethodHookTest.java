package com.cci.hybris.stripe.facades.order.hook.impl;

import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.commerceservices.service.data.CommerceOrderResult;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class StripePaymentPlaceOrderMethodHookTest {

    @InjectMocks
    private StripePaymentPlaceOrderMethodHook hook;

    @Mock
    private StripePaymentTransactionService stripePaymentTransactionService;

    @Test
    public void afterPlaceOrder_cartAndOrderPresent_synchronizesStripePayments() throws Exception {
        // Arrange
        final CartModel cart = new CartModel();
        final OrderModel order = new OrderModel();
        final CommerceCheckoutParameter parameter = new CommerceCheckoutParameter();
        parameter.setCart(cart);

        final CommerceOrderResult result = new CommerceOrderResult();
        result.setOrder(order);

        // Act
        hook.afterPlaceOrder(parameter, result);

        // Assert
        verify(stripePaymentTransactionService).synchronizeStripePaymentsToOrder(cart, order);
    }
}
