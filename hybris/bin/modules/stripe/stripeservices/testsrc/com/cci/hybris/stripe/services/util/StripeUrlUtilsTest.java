package com.cci.hybris.stripe.services.util;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.model.order.CartModel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UnitTest
public class StripeUrlUtilsTest {

    @Test
    public void appendAndStripSessionPlaceholder_preserveUrlStructure() {
        final String withPlaceholder = StripeUrlUtils.appendSessionPlaceholder("https://example.local/success");
        assertEquals("https://example.local/success?session_id={CHECKOUT_SESSION_ID}", withPlaceholder);
        assertEquals("https://example.local/success", StripeUrlUtils.stripSessionPlaceholder(withPlaceholder));
    }

    @Test
    public void appendContextParameters_withCartGuid_appendsEncodedContext() {
        final CartModel cart = new CartModel();
        cart.setCode("cart-001");
        cart.setGuid("cart guid 001");
        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");
        cart.setSite(site);

        final String result = StripeUrlUtils.appendContextParameters("https://example.local/success", cart);
        assertEquals("https://example.local/success?cartId=cart+guid+001&orderCode=cart-001", result);
    }
}
