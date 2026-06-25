package com.cci.hybris.stripe.facades.util;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.order.CartModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UnitTest
public class StripeFacadeAmountUtilsTest {

    @Test
    public void resolveDisplayAmount_grossCart_usesTotalPriceOnly() {
        final CartModel cart = new CartModel();
        cart.setTotalPrice(Double.valueOf(12D));
        cart.setTotalTax(Double.valueOf(3D));
        cart.setNet(Boolean.FALSE);

        assertEquals(12D, StripeFacadeAmountUtils.resolveDisplayAmount(cart), 0.0D);
    }

    @Test
    public void resolveDisplayAmount_netCart_addsTax() {
        final CartModel cart = new CartModel();
        cart.setTotalPrice(Double.valueOf(12D));
        cart.setTotalTax(Double.valueOf(3D));
        cart.setNet(Boolean.TRUE);

        assertEquals(15D, StripeFacadeAmountUtils.resolveDisplayAmount(cart), 0.0D);
    }

    @Test
    public void resolveDisplayAmount_nullOrder_returnsZero() {
        assertEquals(0D, StripeFacadeAmountUtils.resolveDisplayAmount(null), 0.0D);
    }

    @Test
    public void toMajorUnits_convertsMinorAmountByDigits() {
        assertEquals(12.34D, StripeFacadeAmountUtils.toMajorUnits(1234L, 2), 0.0000001D);
        assertEquals(1234D, StripeFacadeAmountUtils.toMajorUnits(1234L, 0), 0.0000001D);
        assertEquals(12.345D, StripeFacadeAmountUtils.toMajorUnits(12345L, 3), 0.0000001D);
        assertEquals(-0.5D, StripeFacadeAmountUtils.toMajorUnits(-50L, 2), 0.0000001D);
    }
}
