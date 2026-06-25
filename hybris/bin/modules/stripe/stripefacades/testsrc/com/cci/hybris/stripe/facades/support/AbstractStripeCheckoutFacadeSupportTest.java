package com.cci.hybris.stripe.facades.support;

import com.cci.hybris.stripe.facades.constants.StripeFacadesConstants;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class AbstractStripeCheckoutFacadeSupportTest {

    private static final String NO_CART_MESSAGE = "No test cart.";
    private static final String NO_SITE_MESSAGE = "No test site.";

    @InjectMocks
    private TestableSupport support;

    @Mock
    private CartService cartService;
    @Mock
    private BaseSiteService baseSiteService;
    @Mock
    private CheckoutFacade checkoutFacade;
    @Mock
    private OrderFacade orderFacade;

    @Test
    public void requireSessionCart_whenMissing_throwsConfiguredMessage() {
        when(cartService.hasSessionCart()).thenReturn(false);

        final StripeIntegrationException exception =
                assertThrows(StripeIntegrationException.class, support::requireSessionCart);

        assertEquals(NO_CART_MESSAGE, exception.getMessage());
    }

    @Test
    public void requireCurrentSiteId_whenMissing_throwsConfiguredMessage() {
        when(baseSiteService.getCurrentBaseSite()).thenReturn(null);

        final StripeIntegrationException exception =
                assertThrows(StripeIntegrationException.class, support::requireCurrentSiteId);

        assertEquals(NO_SITE_MESSAGE, exception.getMessage());
    }

    @Test
    public void resolveSessionCartCode_whenNoSessionCart_returnsNull() {
        when(cartService.hasSessionCart()).thenReturn(false);

        assertNull(support.resolveSessionCartCode());
    }

    @Test
    public void placeOrderFromCart_whenCheckoutReturnsNull_throwsStripeException() throws InvalidCartException {
        final CartModel cart = new CartModel();
        when(checkoutFacade.placeOrder()).thenReturn(null);

        final StripeIntegrationException exception =
                assertThrows(StripeIntegrationException.class, () -> support.placeOrderFromCart(cart));

        assertEquals(StripeFacadesConstants.MESSAGE_NO_ORDER_FOR_PAID_STRIPE_CART, exception.getMessage());
        verify(cartService).setSessionCart(cart);
    }

    @Test
    public void placeOrderFromCart_whenCheckoutReturnsOrderData_returnsOrder() throws InvalidCartException {
        final CartModel cart = new CartModel();
        final OrderData orderData = new OrderData();
        orderData.setCode("00010001");

        when(checkoutFacade.placeOrder()).thenReturn(orderData);

        final OrderData result = support.placeOrderFromCart(cart);

        assertEquals("00010001", result.getCode());
        verify(cartService).setSessionCart(cart);
    }

    @Test
    public void resolveOrderData_returnsOrderFacadeResult() {
        final OrderModel orderModel = new OrderModel();
        orderModel.setCode("00010002");
        final OrderData orderData = new OrderData();
        orderData.setCode("00010002");
        when(orderFacade.getOrderDetailsForCodeWithoutUser("00010002")).thenReturn(orderData);

        final OrderData result = support.resolveOrderData(orderModel);

        assertEquals("00010002", result.getCode());
    }

    private static class TestableSupport extends AbstractStripeCheckoutFacadeSupport {

        @Override
        protected String getNoSessionCartMessage() {
            return NO_CART_MESSAGE;
        }

        @Override
        protected String getNoCurrentBaseSiteMessage() {
            return NO_SITE_MESSAGE;
        }

        protected CartModel requireSessionCart() {
            return getSessionCart();
        }

        protected String requireCurrentSiteId() {
            return getCurrentSiteId();
        }

        protected String resolveSessionCartCode() {
            return getSessionCartCode();
        }

        protected OrderData placeOrderFromCart(final CartModel cart) throws InvalidCartException {
            return placeOrder(cart);
        }

        protected OrderData resolveOrderData(final OrderModel orderModel) {
            return getOrderData(orderModel);
        }
    }
}
