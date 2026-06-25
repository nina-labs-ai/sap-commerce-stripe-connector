
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;

/**
 * Configuration helper utilities for site-aware Stripe property resolution.
 */
public final class StripeConfigurationUtils {

    private StripeConfigurationUtils() {
    }

    /**
     * Builds the site-specific property name for a global property key.
     *
     * @param siteId base-site identifier
     * @param propertyName global property key
     * @return site-specific property key or {@code null}
     */
    public static String buildSitePropertyName(final String siteId, final String propertyName) {
        return StringUtils.isBlank(siteId)
                ? null
                : (siteId + StripeServicesConstants.SITE_PROPERTY_SEPARATOR + propertyName);
    }

    /**
     * Returns whether a value is non-blank and not a setup placeholder.
     *
     * @param value property value
     * @return {@code true} when the value can be used
     */
    public static boolean isUsableValue(final String value) {
        return StringUtils.isNotBlank(value) && !isPlaceholderValue(value);
    }

    /**
     * Returns whether the value is a setup placeholder.
     *
     * @param value property value
     * @return {@code true} when the value is a placeholder
     */
    public static boolean isPlaceholderValue(final String value) {
        return StringUtils.trimToEmpty(value).toLowerCase(Locale.ROOT)
                .startsWith(StripeServicesConstants.PLACEHOLDER_PREFIX);
    }

    /**
     * Returns checked property names for site/global lookup.
     *
     * @param siteId base-site identifier
     * @param propertyName global property key
     * @return checked keys
     */
    public static List<String> getCheckedPropertyNames(final String siteId, final String propertyName) {
        final List<String> propertyNames = new ArrayList<>();
        final String sitePropertyName = buildSitePropertyName(siteId, propertyName);
        if (StringUtils.isNotBlank(sitePropertyName)) {
            propertyNames.add(sitePropertyName);
        }
        propertyNames.add(propertyName);
        return propertyNames;
    }

    /**
     * Builds the missing-property error message.
     *
     * @param siteId base-site identifier
     * @param propertyName property key
     * @return message text
     */
    public static String buildMissingPropertyMessage(final String siteId, final String propertyName) {
        return "Missing Stripe configuration property: " + propertyName + " (checked "
                + String.join(", ", getCheckedPropertyNames(siteId, propertyName)) + ")";
    }

    /**
     * Builds the invalid-url error message.
     *
     * @param siteId base-site identifier
     * @param propertyName property key
     * @param url property value
     * @return message text
     */
    public static String buildInvalidUrlMessage(final String siteId, final String propertyName, final String url) {
        return "Invalid Stripe URL property: " + propertyName + " (checked "
                + String.join(", ", getCheckedPropertyNames(siteId, propertyName)) + ", value=" + url + ")";
    }

    /**
     * Validates that a URL value is an absolute HTTP(S) URL.
     *
     * @param siteId base-site identifier
     * @param propertyName property key
     * @param url property value
     * @return validated value
     */
    public static String validateAbsoluteUrl(final String siteId, final String propertyName, final String url) {
        final String sanitizedUrl = StripeUrlUtils.sanitizeUrlForValidation(url);
        try {
            final URI uri = new URI(sanitizedUrl);
            final String scheme = uri.getScheme();
            if (!(StripeServicesConstants.URL_SCHEME_HTTP.equalsIgnoreCase(scheme)
                    || StripeServicesConstants.URL_SCHEME_HTTPS.equalsIgnoreCase(scheme))
                    || StringUtils.isBlank(uri.getHost())) {
                throw new StripeIntegrationException(buildInvalidUrlMessage(siteId, propertyName, url));
            }
        } catch (final URISyntaxException exception) {
            throw new StripeIntegrationException(buildInvalidUrlMessage(siteId, propertyName, url), exception);
        }
        return url;
    }
}
