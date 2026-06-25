
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

import java.math.BigDecimal;

/**
 * Validation helper utilities for deterministic Stripe input checks.
 */
public final class StripeValidationUtils {

    private StripeValidationUtils() {
    }

    /**
     * Validates that amount is positive when provided.
     *
     * @param amount amount value
     * @param errorMessage error message to use when invalid
     */
    public static void validatePositiveAmount(final BigDecimal amount, final String errorMessage) {
        if (amount != null && amount.signum() <= 0) {
            throw new StripeIntegrationException(errorMessage);
        }
    }
}
