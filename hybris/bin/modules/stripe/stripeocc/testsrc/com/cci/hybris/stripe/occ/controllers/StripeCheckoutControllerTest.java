package com.cci.hybris.stripe.occ.controllers;

import com.cci.hybris.stripe.facades.checkout.StripeCheckoutFacade;
import com.cci.hybris.stripe.facades.data.StripeCheckoutSessionFacadeData;
import com.cci.hybris.stripe.facades.data.StripePublicConfigurationFacadeData;
import com.cci.hybris.stripe.occ.dto.StripeCheckoutSessionWsDTO;
import com.cci.hybris.stripe.occ.dto.StripePublicConfigurationWsDTO;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class StripeCheckoutControllerTest {

    @InjectMocks
    private StripeCheckoutController controller;

    @Mock
    private StripeCheckoutFacade stripeCheckoutFacade;
    @Mock
    private CartLoaderStrategy cartLoaderStrategy;
    @Mock
    private DataMapper dataMapper;

    @Test
    public void createCheckoutSession_facadeReturnsData_mapsResponse() {
        // Arrange
        final StripeCheckoutSessionFacadeData facadeData = new StripeCheckoutSessionFacadeData();
        facadeData.setId("cs_test_123");
        facadeData.setUrl("https://checkout.stripe.test/session");
        when(stripeCheckoutFacade.createCheckoutSessionForCart()).thenReturn(facadeData);

        // Act
        final StripeCheckoutSessionWsDTO result = controller.createCheckoutSession("current", null);

        // Assert
        verify(cartLoaderStrategy).loadCart("current");
        assertEquals("cs_test_123", result.getId());
        assertEquals("https://checkout.stripe.test/session", result.getUrl());
    }

    @Test
    public void getConfiguration_facadeReturnsData_mapsResponse() {
        // Arrange
        final StripePublicConfigurationFacadeData facadeData = new StripePublicConfigurationFacadeData();
        facadeData.setPublishableKey("pk_test");
        facadeData.setPaymentOptionId("stripe-checkout");
        when(stripeCheckoutFacade.getPublicConfiguration()).thenReturn(facadeData);

        // Act
        final StripePublicConfigurationWsDTO result = controller.getConfiguration();

        // Assert
        assertEquals("pk_test", result.getPublishableKey());
        assertEquals("stripe-checkout", result.getPaymentOptionId());
    }

    @Test
    public void expireCheckoutSession_facadeReturnsData_mapsResponse() {
        // Arrange
        final StripeCheckoutSessionFacadeData facadeData = new StripeCheckoutSessionFacadeData();
        facadeData.setId("cs_test_123");
        facadeData.setStatus("expired");
        when(stripeCheckoutFacade.expireCheckoutSessionForCart("cs_test_123")).thenReturn(facadeData);

        // Act
        final StripeCheckoutSessionWsDTO result = controller.expireCheckoutSession("current", "cs_test_123", null);

        // Assert
        verify(cartLoaderStrategy).loadCart("current");
        assertEquals("cs_test_123", result.getId());
        assertEquals("expired", result.getStatus());
    }

    @Test
    public void getCheckoutSession_anonymousCartIdProvidedWithoutOrderCode_loadsAnonymousCartBeforeFacadeLookup() {
        // Arrange
        final StripeCheckoutSessionFacadeData facadeData = new StripeCheckoutSessionFacadeData();
        facadeData.setId("cs_test_123");
        when(stripeCheckoutFacade.getCheckoutSessionForContext(anyString(), anyString())).thenReturn(facadeData);

        // Act
        final StripeCheckoutSessionWsDTO result = controller.getCheckoutSession("anonymous", "guid-123", null, "cs_test_123");

        // Assert
        verify(cartLoaderStrategy).loadCart("guid-123");
        verify(stripeCheckoutFacade).getCheckoutSessionForContext("cs_test_123", "guid-123");
        assertEquals("cs_test_123", result.getId());
    }

    @Test
    public void getCheckoutSession_anonymousOrderCodeProvided_skipsCartLoadAndDelegatesLookup() {
        // Arrange
        final StripeCheckoutSessionFacadeData facadeData = new StripeCheckoutSessionFacadeData();
        facadeData.setId("cs_test_123");
        when(stripeCheckoutFacade.getCheckoutSessionForContext("cs_test_123", "cart-001")).thenReturn(facadeData);

        // Act
        final StripeCheckoutSessionWsDTO result = controller.getCheckoutSession("anonymous", "guid-123", "cart-001", "cs_test_123");

        // Assert
        verify(stripeCheckoutFacade).getCheckoutSessionForContext("cs_test_123", "cart-001");
        assertEquals("cs_test_123", result.getId());
    }

    @Test
    public void getCheckoutSession_anonymousWithoutCartId_skipsCartLoadAndDelegatesLookup() {
        // Arrange
        final StripeCheckoutSessionFacadeData facadeData = new StripeCheckoutSessionFacadeData();
        facadeData.setId("cs_test_123");
        when(stripeCheckoutFacade.getCheckoutSessionForContext("cs_test_123", null)).thenReturn(facadeData);

        // Act
        final StripeCheckoutSessionWsDTO result = controller.getCheckoutSession("anonymous", null, null, "cs_test_123");

        // Assert
        verify(stripeCheckoutFacade).getCheckoutSessionForContext("cs_test_123", null);
        assertEquals("cs_test_123", result.getId());
    }

    @Test
    public void finalizeCheckoutSession_facadeReturnsOrder_mapsResponse() throws Exception {
        // Arrange
        final OrderData orderData = new OrderData();
        orderData.setCode("00001001");

        final OrderWsDTO orderWsDTO = new OrderWsDTO();
        orderWsDTO.setCode("00001001");

        when(stripeCheckoutFacade.finalizeCheckoutSessionForContext("cs_test_123", "cart-001")).thenReturn(orderData);
        when(dataMapper.map(orderData, OrderWsDTO.class, "FULL")).thenReturn(orderWsDTO);

        // Act
        final OrderWsDTO result = controller.finalizeCheckoutSession("anonymous", "guid-123", "cart-001", "FULL", "cs_test_123");

        // Assert
        verify(stripeCheckoutFacade).finalizeCheckoutSessionForContext("cs_test_123", "cart-001");
        assertEquals("00001001", result.getCode());
    }
}
