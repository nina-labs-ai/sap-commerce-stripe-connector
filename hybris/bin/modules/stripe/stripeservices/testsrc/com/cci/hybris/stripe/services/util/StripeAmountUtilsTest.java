package com.cci.hybris.stripe.services.util;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.OrderModel;

import java.math.BigDecimal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UnitTest
public class StripeAmountUtilsTest {

    @Test
    public void toMinorUnits_withHalfUpRounding_returnsExpectedMinorAmount() {
        assertEquals(1235L, StripeAmountUtils.toMinorUnits(new BigDecimal("12.345"), 2));
    }

    @Test
    public void calculateOrderTotal_withNetOrder_includesTax() {
        final OrderModel order = new OrderModel();
        order.setTotalPrice(Double.valueOf(10D));
        order.setTotalTax(Double.valueOf(2D));
        order.setNet(Boolean.TRUE);

        assertEquals(new BigDecimal("12.0"), StripeAmountUtils.calculateOrderTotal(order));
    }

    @Test
    public void toMajorAmount_withNullAmount_returnsZero() {
        assertEquals(BigDecimal.ZERO, StripeAmountUtils.toMajorAmount(null, 2));
        assertEquals(new BigDecimal("12.34"), StripeAmountUtils.toMajorAmount(Long.valueOf(1234L), 2));
    }
}
