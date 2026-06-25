
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.util;

import com.cci.hybris.stripe.facades.constants.StripeFacadesConstants;
import com.cci.hybris.stripe.facades.data.StripeCheckoutSessionFacadeData;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;

import java.util.Objects;

/**
 * Utility methods used by Stripe Checkout facades.
 */
public final class StripeCheckoutFacadeUtils {

    private StripeCheckoutFacadeUtils() {
        // utility class
    }

    /**
     * Converts service-level Stripe checkout session data into facade data.
     *
     * @param source service data
     * @return facade data
     */
    public static StripeCheckoutSessionFacadeData toFacadeData(final StripeCheckoutSessionData source) {
        final StripeCheckoutSessionFacadeData data = new StripeCheckoutSessionFacadeData();
        data.setId(source.getId());
        data.setUrl(source.getUrl());
        data.setStatus(source.getStatus());
        data.setPaymentStatus(source.getPaymentStatus());
        data.setClientReferenceId(source.getClientReferenceId());
        return data;
    }

    /**
     * Returns whether the Stripe Checkout session can be finalized into an SAP Commerce order.
     *
     * @param sessionData Stripe Checkout session data
     * @return {@code true} when Stripe status is complete or paid
     */
    public static boolean isFinalizableSession(final StripeCheckoutSessionData sessionData) {
        return sessionData != null
                && (StripeFacadesConstants.STRIPE_CHECKOUT_PAYMENT_STATUS_PAID.equalsIgnoreCase(sessionData.getPaymentStatus())
                || StripeFacadesConstants.STRIPE_CHECKOUT_STATUS_COMPLETE.equalsIgnoreCase(sessionData.getStatus()));
    }

    /**
     * Returns whether the session client reference matches the expected local context code.
     *
     * @param expectedCode expected cart or order code
     * @param sessionData Stripe Checkout session data
     * @return {@code true} when the codes match
     */
    public static boolean hasMatchingClientReference(final String expectedCode, final StripeCheckoutSessionData sessionData) {
        return sessionData != null && Objects.equals(expectedCode, sessionData.getClientReferenceId());
    }
}
