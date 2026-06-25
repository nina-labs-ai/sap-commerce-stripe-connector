package com.cci.hybris.stripe.occ.controllers;

import com.cci.hybris.stripe.facades.data.StripeRefundFacadeData;
import com.cci.hybris.stripe.facades.refund.StripeRefundFacade;
import com.cci.hybris.stripe.occ.dto.StripeRefundRequestWsDTO;
import com.cci.hybris.stripe.occ.dto.StripeRefundWsDTO;

import de.hybris.bootstrap.annotations.UnitTest;

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
public class StripeRefundControllerTest {

    @InjectMocks
    private StripeRefundController controller;

    @Mock
    private StripeRefundFacade stripeRefundFacade;

    @Test
    public void createRefund_facadeReturnsData_mapsResponse() {
        // Arrange
        final StripeRefundRequestWsDTO request = new StripeRefundRequestWsDTO();
        request.setPaymentReference("pi_test_123");
        request.setAmount(new BigDecimal("5.00"));

        final StripeRefundFacadeData facadeData = new StripeRefundFacadeData();
        facadeData.setId("re_test_123");
        facadeData.setPaymentIntentId("pi_test_123");
        facadeData.setStatus("succeeded");
        facadeData.setAmount(Long.valueOf(500L));
        facadeData.setCurrency("usd");
        facadeData.setFormattedAmount("$5.00");
        facadeData.setOrderCode("00001001");
        facadeData.setPaymentReference("pi_test_123");
        when(stripeRefundFacade.createRefundForOrder("00001001", "pi_test_123", new BigDecimal("5.00")))
                .thenReturn(facadeData);

        // Act
        final StripeRefundWsDTO result = controller.createRefund("00001001", request);

        // Assert
        assertEquals("re_test_123", result.getId());
        assertEquals("pi_test_123", result.getPaymentIntentId());
        assertEquals("$5.00", result.getFormattedAmount());
        assertEquals("00001001", result.getOrderCode());
        verify(stripeRefundFacade).createRefundForOrder("00001001", "pi_test_123", new BigDecimal("5.00"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void createRefund_missingPaymentReference_throwsIllegalArgumentException() {
        // Arrange
        final StripeRefundRequestWsDTO request = new StripeRefundRequestWsDTO();
        request.setAmount(new BigDecimal("5.00"));

        try {
            // Act
            controller.createRefund("00001001", request);
        } finally {
            // Assert
            verifyNoInteractions(stripeRefundFacade);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void createRefund_nonPositiveAmount_throwsIllegalArgumentException() {
        // Arrange
        final StripeRefundRequestWsDTO request = new StripeRefundRequestWsDTO();
        request.setPaymentReference("pi_test_123");
        request.setAmount(BigDecimal.ZERO);

        try {
            // Act
            controller.createRefund("00001001", request);
        } finally {
            // Assert
            verifyNoInteractions(stripeRefundFacade);
        }
    }
}
