
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.exception;

/**
 * Runtime exception used for Stripe integration failures.
 */
public class StripeIntegrationException extends RuntimeException {

    /**
     * Creates the exception with a message.
     *
     * @param message failure description
     */
    public StripeIntegrationException(final String message) {
        super(message);
    }

    /**
     * Creates the exception with a message and cause.
     *
     * @param message failure description
     * @param cause underlying failure
     */
    public StripeIntegrationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
