package com.cci.hybris.stripe.facades.util;

import com.cci.hybris.stripe.facades.data.StripeRefundFacadeData;
import com.cci.hybris.stripe.services.data.StripeRefundData;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UnitTest
public class StripeRefundFacadeUtilsTest {

    @Test
    public void toFacadeData_mapsSourceAndContextFields() {
        final StripeRefundData source = new StripeRefundData();
        source.setId("re_test_123");
        source.setPaymentIntentId("pi_test_123");
        source.setStatus("succeeded");
        source.setAmount(Long.valueOf(500L));
        source.setCurrency("usd");

        final StripeRefundFacadeData result = StripeRefundFacadeUtils.toFacadeData(
                source, "00001001", "pi_test_123", "$5.00");

        assertEquals("re_test_123", result.getId());
        assertEquals("pi_test_123", result.getPaymentIntentId());
        assertEquals("succeeded", result.getStatus());
        assertEquals(Long.valueOf(500L), result.getAmount());
        assertEquals("usd", result.getCurrency());
        assertEquals("00001001", result.getOrderCode());
        assertEquals("pi_test_123", result.getPaymentReference());
        assertEquals("$5.00", result.getFormattedAmount());
    }

    @Test(expected = NullPointerException.class)
    public void toFacadeData_nullSource_throwsNullPointerException() {
        StripeRefundFacadeUtils.toFacadeData(null, "00001001", "pi_test_123", "$5.00");
    }
}
