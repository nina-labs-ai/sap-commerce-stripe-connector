package com.cci.hybris.stripe.facades.refund.impl;

import com.cci.hybris.stripe.facades.data.StripeRefundFacadeData;
import com.cci.hybris.stripe.services.data.StripeRefundData;
import com.cci.hybris.stripe.services.service.StripePaymentLifecycleService;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.commerceservices.order.CommerceOrderService;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.opffacades.calculators.CalculatorStrategy;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.math.BigDecimal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStripeRefundFacadeTest {

    @InjectMocks
    private DefaultStripeRefundFacade facade;

    @Mock
    private BaseStoreService baseStoreService;
    @Mock
    private UserService userService;
    @Mock
    private CheckoutCustomerStrategy checkoutCustomerStrategy;
    @Mock
    private CustomerAccountService customerAccountService;
    @Mock
    private CommerceOrderService commerceOrderService;
    @Mock
    private StripePaymentLifecycleService stripePaymentLifecycleService;
    @Mock
    private CalculatorStrategy calculatorStrategy;
    @Mock
    private PriceData priceData;

    @Test
    public void createRefundForOrder_currentCustomerOrder_mapsRefundData() {
        // Arrange
        final BaseStoreModel baseStore = new BaseStoreModel();
        final CustomerModel customer = new CustomerModel();
        final OrderModel order = createOrder("00001001");
        final StripeRefundData refundData = new StripeRefundData();
        refundData.setId("re_test_123");
        refundData.setPaymentIntentId("pi_test_123");
        refundData.setStatus("succeeded");
        refundData.setAmount(Long.valueOf(500L));
        refundData.setCurrency("usd");

        when(baseStoreService.getCurrentBaseStore()).thenReturn(baseStore);
        when(userService.getCurrentUser()).thenReturn(customer);
        when(checkoutCustomerStrategy.isAnonymousCheckout()).thenReturn(false);
        when(commerceOrderService.getOrderForPotentialId(customer, "00001001", baseStore)).thenReturn(order);
        when(stripePaymentLifecycleService.createRefund(order, "pi_test_123", new BigDecimal("5.00"))).thenReturn(refundData);
        when(calculatorStrategy.createPrice(order, Double.valueOf(5.00D))).thenReturn(priceData);
        when(priceData.getFormattedValue()).thenReturn("$5.00");

        // Act
        final StripeRefundFacadeData result = facade.createRefundForOrder("00001001", "pi_test_123",
                new BigDecimal("5.00"));

        // Assert
        assertEquals("re_test_123", result.getId());
        assertEquals("pi_test_123", result.getPaymentIntentId());
        assertEquals("00001001", result.getOrderCode());
        assertEquals("pi_test_123", result.getPaymentReference());
        assertEquals("$5.00", result.getFormattedAmount());
        verify(stripePaymentLifecycleService).createRefund(order, "pi_test_123", new BigDecimal("5.00"));
    }

    @Test
    public void createRefundForOrder_anonymousCheckout_resolvesGuestOrderByGuid() {
        // Arrange
        final BaseStoreModel baseStore = new BaseStoreModel();
        final OrderModel order = createOrder("guest-order-guid");
        final StripeRefundData refundData = new StripeRefundData();
        refundData.setId("re_test_guest");
        refundData.setAmount(Long.valueOf(500L));

        when(baseStoreService.getCurrentBaseStore()).thenReturn(baseStore);
        when(checkoutCustomerStrategy.isAnonymousCheckout()).thenReturn(true);
        when(customerAccountService.getOrderDetailsForGUID("guest-order-guid", baseStore)).thenReturn(order);
        when(stripePaymentLifecycleService.createRefund(order, "cs_test_123", null)).thenReturn(refundData);

        // Act
        final StripeRefundFacadeData result = facade.createRefundForOrder("guest-order-guid", "cs_test_123", null);

        // Assert
        assertEquals("re_test_guest", result.getId());
        assertEquals("guest-order-guid", result.getOrderCode());
        assertEquals("cs_test_123", result.getPaymentReference());
    }

    @Test(expected = UnknownIdentifierException.class)
    public void createRefundForOrder_nonCustomerPrincipal_throwsUnknownIdentifierException() {
        // Arrange
        final BaseStoreModel baseStore = new BaseStoreModel();
        final UserModel user = new UserModel();

        when(baseStoreService.getCurrentBaseStore()).thenReturn(baseStore);
        when(checkoutCustomerStrategy.isAnonymousCheckout()).thenReturn(false);
        when(userService.getCurrentUser()).thenReturn(user);

        try {
            // Act
            facade.createRefundForOrder("00001001", "pi_test_123", new BigDecimal("5.00"));
        } finally {
            // Assert
            verifyNoInteractions(commerceOrderService);
        }
    }

    protected OrderModel createOrder(final String code) {
        final CurrencyModel currency = new CurrencyModel();
        currency.setDigits(Integer.valueOf(2));
        currency.setIsocode("USD");

        final OrderModel order = new OrderModel();
        order.setCode(code);
        order.setCurrency(currency);
        return order;
    }
}
