
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;

/**
 * URL helper utilities for Stripe configuration and return-flow context.
 */
public final class StripeUrlUtils {

    private StripeUrlUtils() {
    }

    /**
     * Replaces dynamic placeholders so URI validation can run on configured URLs.
     *
     * @param url configured URL
     * @return sanitized URL for validation
     */
    public static String sanitizeUrlForValidation(final String url) {
        return url.replace(StripeServicesConstants.CHECKOUT_SESSION_PLACEHOLDER,
                StripeServicesConstants.CHECKOUT_SESSION_PLACEHOLDER_VALIDATION_VALUE);
    }

    /**
     * Appends checkout session placeholder when URL does not already contain it.
     *
     * @param url configured URL
     * @return URL with placeholder
     */
    public static String appendSessionPlaceholder(final String url) {
        if (url.contains(StripeServicesConstants.CHECKOUT_SESSION_PLACEHOLDER)) {
            return url;
        }

        final String separator = url.contains(StripeServicesConstants.URL_QUERY_SEPARATOR)
                ? StripeServicesConstants.URL_AMPERSAND
                : StripeServicesConstants.URL_QUERY_SEPARATOR;
        return url + separator + StripeServicesConstants.QUERY_PARAM_SESSION_ID + StripeServicesConstants.URL_EQUALS
                + StripeServicesConstants.CHECKOUT_SESSION_PLACEHOLDER;
    }

    /**
     * Removes the checkout session placeholder from a URL.
     *
     * @param url configured URL
     * @return URL without placeholder
     */
    public static String stripSessionPlaceholder(final String url) {
        return url.replace(StripeServicesConstants.URL_QUERY_SEPARATOR + StripeServicesConstants.QUERY_PARAM_SESSION_ID
                        + StripeServicesConstants.URL_EQUALS + StripeServicesConstants.CHECKOUT_SESSION_PLACEHOLDER, "")
                .replace(StripeServicesConstants.URL_AMPERSAND + StripeServicesConstants.QUERY_PARAM_SESSION_ID
                        + StripeServicesConstants.URL_EQUALS + StripeServicesConstants.CHECKOUT_SESSION_PLACEHOLDER, "");
    }

    /**
     * Appends context query parameters used by hosted return flows.
     *
     * @param url configured storefront URL
     * @param order order used for Stripe session
     * @return URL with appended context parameters
     */
    public static String appendContextParameters(final String url, final AbstractOrderModel order) {
        if (StringUtils.isBlank(url) || order == null || StringUtils.isBlank(order.getCode())) {
            return url;
        }

        final StringBuilder builder = new StringBuilder(url);
        appendQueryParameter(builder, StripeServicesConstants.QUERY_PARAM_CART_ID, StripeOrderUtils.resolveReturnCartId(order));
        appendQueryParameter(builder, StripeServicesConstants.QUERY_PARAM_ORDER_CODE, order.getCode());
        return builder.toString();
    }

    /**
     * Appends a query parameter to a URL builder.
     *
     * @param builder URL builder
     * @param name query parameter name
     * @param value query parameter value
     */
    public static void appendQueryParameter(final StringBuilder builder, final String name, final String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }

        builder.append(builder.indexOf(StripeServicesConstants.URL_QUERY_SEPARATOR) >= 0
                        ? StripeServicesConstants.URL_AMPERSAND
                        : StripeServicesConstants.URL_QUERY_SEPARATOR)
                .append(name)
                .append(StripeServicesConstants.URL_EQUALS)
                .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
    }
}
