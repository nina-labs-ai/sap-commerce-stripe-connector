package com.cci.hybris.stripe.occ.controllers;

import com.cci.hybris.stripe.facades.data.StripePaymentElementFacadeData;
import com.cci.hybris.stripe.facades.elements.StripePaymentElementFacade;
import com.cci.hybris.stripe.occ.dto.StripePaymentElementWsDTO;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class StripePaymentElementControllerTest {

    @InjectMocks
    private StripePaymentElementController controller;

    @Mock
    private StripePaymentElementFacade stripePaymentElementFacade;
    @Mock
    private CartLoaderStrategy cartLoaderStrategy;
    @Mock
    private DataMapper dataMapper;

    @Test
    public void createPaymentIntent_facadeReturnsData_mapsResponse() {
        // Arrange
        final StripePaymentElementFacadeData facadeData = new StripePaymentElementFacadeData();
        facadeData.setId("pi_test_123");
        facadeData.setClientSecret("pi_test_123_secret_123");
        facadeData.setPublishableKey("pk_test");
        facadeData.setFormattedAmount("$15.00");
        when(stripePaymentElementFacade.createPaymentIntentForCart()).thenReturn(facadeData);

        // Act
        final StripePaymentElementWsDTO result = controller.createPaymentIntent("current", null);

        // Assert
        verify(cartLoaderStrategy).loadCart("current");
        assertEquals("pi_test_123", result.getId());
        assertEquals("pk_test", result.getPublishableKey());
        assertEquals("$15.00", result.getFormattedAmount());
    }

    @Test
    public void getPaymentIntent_facadeReturnsData_mapsResponse() {
        // Arrange
        final StripePaymentElementFacadeData facadeData = new StripePaymentElementFacadeData();
        facadeData.setId("pi_test_123");
        facadeData.setClientSecret("pi_test_123_secret_123");
        facadeData.setPublishableKey("pk_test");

        when(stripePaymentElementFacade.getPaymentIntent("pi_test_123")).thenReturn(facadeData);

        // Act
        final StripePaymentElementWsDTO result = controller.getPaymentIntent("current", null, "pi_test_123");

        // Assert
        verify(cartLoaderStrategy).loadCart("current");
        assertEquals("pi_test_123", result.getId());
        assertEquals("pi_test_123_secret_123", result.getClientSecret());
        assertEquals("pk_test", result.getPublishableKey());
    }

    @Test
    public void cancelPaymentIntent_facadeReturnsData_mapsResponse() {
        // Arrange
        final StripePaymentElementFacadeData facadeData = new StripePaymentElementFacadeData();
        facadeData.setId("pi_test_123");
        facadeData.setStatus("canceled");
        facadeData.setPublishableKey("pk_test");

        when(stripePaymentElementFacade.cancelPaymentIntentForCart("pi_test_123")).thenReturn(facadeData);

        // Act
        final StripePaymentElementWsDTO result = controller.cancelPaymentIntent("current", null, "pi_test_123");

        // Assert
        verify(cartLoaderStrategy).loadCart("current");
        assertEquals("pi_test_123", result.getId());
        assertEquals("canceled", result.getStatus());
        assertEquals("pk_test", result.getPublishableKey());
    }

    @Test
    public void createPaymentIntent_anonymousRequestWithCartId_loadsExplicitCart() {
        // Arrange
        final StripePaymentElementFacadeData facadeData = new StripePaymentElementFacadeData();
        facadeData.setId("pi_test_anon");
        when(stripePaymentElementFacade.createPaymentIntentForCart()).thenReturn(facadeData);

        // Act
        final StripePaymentElementWsDTO result = controller.createPaymentIntent("anonymous", "00012345");

        // Assert
        verify(cartLoaderStrategy).loadCart("00012345");
        assertEquals("pi_test_anon", result.getId());
    }

    @Test
    public void finalizePaymentIntent_facadeReturnsOrder_mapsResponse() throws Exception {
        // Arrange
        final OrderData orderData = new OrderData();
        orderData.setCode("order-001");
        final de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO orderWsDTO =
                new de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO();
        orderWsDTO.setCode("order-001");
        when(stripePaymentElementFacade.finalizePaymentIntentForContext("pi_test_123", "cart-001"))
                .thenReturn(orderData);
        when(dataMapper.map(orderData,
                de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO.class, "FULL"))
                        .thenReturn(orderWsDTO);

        // Act
        final de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO result =
                controller.finalizePaymentIntent("anonymous", "cart-001", "FULL", "pi_test_123");

        // Assert
        verify(cartLoaderStrategy).loadCart("cart-001");
        verify(stripePaymentElementFacade).finalizePaymentIntentForContext("pi_test_123", "cart-001");
        assertEquals("order-001", result.getCode());
    }

    @Test
    public void finalizePaymentIntent_expiredAnonymousCartStillDelegatesToFacade() throws Exception {
        // Arrange
        final OrderData orderData = new OrderData();
        orderData.setCode("order-001");
        final de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO orderWsDTO =
                new de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO();
        orderWsDTO.setCode("order-001");
        doThrow(new RuntimeException("Cart expired")).when(cartLoaderStrategy).loadCart("cart-001");
        when(stripePaymentElementFacade.finalizePaymentIntentForContext("pi_test_123", "cart-001"))
                .thenReturn(orderData);
        when(dataMapper.map(orderData,
                de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO.class, "FULL"))
                        .thenReturn(orderWsDTO);

        // Act
        final de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO result =
                controller.finalizePaymentIntent("anonymous", "cart-001", "FULL", "pi_test_123");

        // Assert
        verify(cartLoaderStrategy).loadCart("cart-001");
        verify(stripePaymentElementFacade).finalizePaymentIntentForContext("pi_test_123", "cart-001");
        assertEquals("order-001", result.getCode());
    }
}
