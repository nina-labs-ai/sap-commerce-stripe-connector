
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.CustomerModel;

import org.apache.commons.lang3.StringUtils;

/**
 * Order and customer helper utilities for Stripe service flows.
 */
public final class StripeOrderUtils {

    private StripeOrderUtils() {
    }

    /**
     * Validates that the order has site and currency context required for Stripe.
     *
     * @param order order to validate
     * @param errorMessage message used when validation fails
     */
    public static void validateOrderWithContext(final AbstractOrderModel order, final String errorMessage) {
        if (order == null || order.getSite() == null || order.getCurrency() == null) {
            throw new StripeIntegrationException(errorMessage);
        }
    }

    /**
     * Extracts the customer email used for Stripe receipts.
     *
     * @param order order to inspect
     * @return customer email or {@code null}
     */
    public static String extractCustomerEmail(final AbstractOrderModel order) {
        if (order.getUser() instanceof CustomerModel customer) {
            return StringUtils.defaultIfBlank(customer.getContactEmail(), customer.getUid());
        }
        return null;
    }

    /**
     * Resolves the cart identifier expected by anonymous OCC return flows.
     *
     * @param order order or cart used for the Stripe session
     * @return cart guid for anonymous carts, otherwise the order code
     */
    public static String resolveReturnCartId(final AbstractOrderModel order) {
        if (order instanceof CartModel cart) {
            return StringUtils.defaultIfBlank(cart.getGuid(), cart.getCode());
        }
        return order.getCode();
    }
}
