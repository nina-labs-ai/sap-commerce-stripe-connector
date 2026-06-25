package com.cci.hybris.stripe.services.util;

import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.model.order.OrderModel;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripeMetadataUtilsTest {

    @Test
    public void ownershipChecks_withMatchingOrderAndSite_returnTrue() {
        final OrderModel order = new OrderModel();
        order.setCode("order-001");
        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");
        order.setSite(site);

        final Session session = new Session();
        session.setClientReferenceId("order-001");
        session.setMetadata(Map.of("siteUid", "electronics"));

        final PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setMetadata(Map.of("orderCode", "order-001", "siteUid", "electronics"));

        assertTrue(StripeMetadataUtils.matchesCheckoutSessionOwnership(order, session));
        assertTrue(StripeMetadataUtils.matchesPaymentIntentOwnership(order, paymentIntent));
    }

    @Test
    public void ownershipChecks_withMismatchedSite_returnFalse() {
        final OrderModel order = new OrderModel();
        order.setCode("order-001");
        final BaseSiteModel site = new BaseSiteModel();
        site.setUid("electronics");
        order.setSite(site);

        final Session session = new Session();
        session.setClientReferenceId("order-001");
        session.setMetadata(Map.of("siteUid", "apparel"));

        assertFalse(StripeMetadataUtils.matchesCheckoutSessionOwnership(order, session));
    }
}
