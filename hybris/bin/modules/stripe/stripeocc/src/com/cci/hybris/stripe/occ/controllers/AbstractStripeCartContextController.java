
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.occ.controllers;

import com.cci.hybris.stripe.occ.constants.StripeOccConstants;

import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;

import jakarta.annotation.Resource;

import org.apache.commons.lang3.StringUtils;

/**
 * Shared OCC cart-context loading rules for Stripe checkout return flows.
 */
public abstract class AbstractStripeCartContextController {

    @Resource(name = "cartLoaderStrategy")
    private CartLoaderStrategy cartLoaderStrategy;

    protected void loadCartForContext(
            final String userId,
            final String cartId,
            final String anonymousCartRequiredMessage) {
        if (isAnonymous(userId)) {
            if (StringUtils.isBlank(cartId)) {
                throw new IllegalArgumentException(anonymousCartRequiredMessage);
            }
            getCartLoaderStrategy().loadCart(cartId);
            return;
        }

        getCartLoaderStrategy().loadCart(StripeOccConstants.CURRENT_CART_ID);
    }

    protected void loadCartForReadContext(
            final String userId,
            final String cartId,
            final String orderCode,
            final String anonymousCartRequiredMessage) {
        if (isAnonymous(userId)
                && (StringUtils.isBlank(cartId) || StringUtils.isNotBlank(orderCode))) {
            return;
        }

        loadCartForContext(userId, cartId, anonymousCartRequiredMessage);
    }

    protected void loadCartForFinalizeContextIfPossible(
            final String userId,
            final String cartId,
            final String anonymousCartRequiredMessage) {
        try {
            loadCartForContext(userId, cartId, anonymousCartRequiredMessage);
        } catch (final RuntimeException exception) {
            // Ignore stale cart reload failures on the return path and let Stripe id lookups resolve ownership.
        }
    }

    protected boolean isAnonymous(final String userId) {
        return StripeOccConstants.ANONYMOUS_USER_ID.equalsIgnoreCase(StringUtils.defaultString(userId));
    }

    protected String resolveReference(final String orderCode, final String cartId) {
        return StringUtils.defaultIfBlank(orderCode, cartId);
    }

    protected CartLoaderStrategy getCartLoaderStrategy() {
        return cartLoaderStrategy;
    }
}
