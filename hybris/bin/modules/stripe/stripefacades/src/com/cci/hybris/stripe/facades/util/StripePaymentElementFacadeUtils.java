
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.util;

import com.cci.hybris.stripe.facades.constants.StripeFacadesConstants;
import com.cci.hybris.stripe.facades.data.StripePaymentElementFacadeData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;

import de.hybris.platform.core.model.order.CartModel;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility methods used by Stripe Payment Elements facades.
 */
public final class StripePaymentElementFacadeUtils {

    private StripePaymentElementFacadeUtils() {
        // utility class
    }

    /**
     * Converts service-level PaymentIntent data into facade data.
     *
     * @param source service data
     * @param publishableKey publishable key for the current site
     * @param paymentOptionId storefront payment option identifier
     * @param paymentMethod storefront payment method identifier
     * @param returnUrl storefront return URL
     * @param formattedAmount formatted amount for storefront display
     * @return facade data
     */
    public static StripePaymentElementFacadeData toFacadeData(final StripePaymentIntentData source,
                                                              final String publishableKey,
                                                              final String paymentOptionId,
                                                              final String paymentMethod,
                                                              final String returnUrl,
                                                              final String formattedAmount) {
        final StripePaymentElementFacadeData data = new StripePaymentElementFacadeData();
        data.setId(source.getId());
        data.setClientSecret(source.getClientSecret());
        data.setStatus(source.getStatus());
        data.setAmount(source.getAmount());
        data.setCurrency(source.getCurrency());
        data.setClientReferenceId(source.getClientReferenceId());
        data.setPublishableKey(publishableKey);
        data.setPaymentOptionId(paymentOptionId);
        data.setPaymentMethod(paymentMethod);
        data.setReturnUrl(returnUrl);
        data.setFormattedAmount(formattedAmount);
        return data;
    }

    /**
     * Returns whether the PaymentIntent can be finalized into an SAP Commerce order.
     *
     * @param paymentIntentData PaymentIntent data
     * @return {@code true} when status is succeeded or requires_capture
     */
    public static boolean isFinalizablePaymentIntent(final StripePaymentIntentData paymentIntentData) {
        return paymentIntentData != null
                && (StripeFacadesConstants.STRIPE_PAYMENT_INTENT_STATUS_SUCCEEDED.equalsIgnoreCase(paymentIntentData.getStatus())
                || StripeFacadesConstants.STRIPE_PAYMENT_INTENT_STATUS_REQUIRES_CAPTURE
                        .equalsIgnoreCase(paymentIntentData.getStatus()));
    }

    /**
     * Returns whether the provided order code matches the cart code or guid.
     *
     * @param cart session cart
     * @param orderCode caller-provided context code
     * @return {@code true} when the context matches cart code or guid
     */
    public static boolean matchesCheckoutContext(final CartModel cart, final String orderCode) {
        return cart != null
                && StringUtils.isNotBlank(orderCode)
                && (Objects.equals(orderCode, cart.getCode()) || Objects.equals(orderCode, cart.getGuid()));
    }
}
