
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

/**
 * Metadata helper utilities for Stripe ownership and site resolution.
 */
public final class StripeMetadataUtils {

    private StripeMetadataUtils() {
    }

    /**
     * Resolves order code from Stripe metadata, falling back to the supplied value.
     *
     * @param metadata Stripe metadata
     * @param fallback fallback value
     * @return resolved order code or {@code null}
     */
    public static String resolveOrderCode(final Map<String, String> metadata, final String fallback) {
        if (metadata != null && StringUtils.isNotBlank(metadata.get(StripeServicesConstants.METADATA_ORDER_CODE))) {
            return metadata.get(StripeServicesConstants.METADATA_ORDER_CODE);
        }
        return fallback;
    }

    /**
     * Returns whether metadata contains expected site ownership.
     *
     * @param metadata Stripe metadata
     * @param siteId expected site identifier
     * @return {@code true} when site ownership matches
     */
    public static boolean hasExpectedSite(final Map<String, String> metadata, final String siteId) {
        return metadata != null && Objects.equals(siteId, metadata.get(StripeServicesConstants.METADATA_SITE_UID));
    }

    /**
     * Returns whether metadata matches expected order code and site identifier.
     *
     * @param metadata Stripe metadata
     * @param orderCode expected order code
     * @param siteId expected site identifier
     * @return {@code true} when ownership matches
     */
    public static boolean matchesOrderAndSite(final Map<String, String> metadata,
                                              final String orderCode,
                                              final String siteId) {
        return metadata != null
                && Objects.equals(orderCode, metadata.get(StripeServicesConstants.METADATA_ORDER_CODE))
                && Objects.equals(siteId, metadata.get(StripeServicesConstants.METADATA_SITE_UID));
    }

    /**
     * Resolves expected site identifier from explicit argument or order context.
     *
     * @param requestedSiteId explicit site identifier
     * @param order owning order
     * @return resolved site identifier
     */
    public static String resolveExpectedSiteId(final String requestedSiteId, final AbstractOrderModel order) {
        return StringUtils.defaultIfBlank(requestedSiteId, order.getSite().getUid());
    }

    /**
     * Resolves expected site identifier from explicit argument or metadata.
     *
     * @param requestedSiteId explicit site identifier
     * @param metadata Stripe metadata
     * @return resolved site identifier or {@code null}
     */
    public static String resolveExpectedSiteId(final String requestedSiteId, final Map<String, String> metadata) {
        if (StringUtils.isNotBlank(requestedSiteId)) {
            return requestedSiteId;
        }
        return metadata == null ? null : metadata.get(StripeServicesConstants.METADATA_SITE_UID);
    }

    /**
     * Returns whether a Checkout Session belongs to the supplied order and site.
     *
     * @param order expected owner
     * @param session Checkout Session
     * @return {@code true} when ownership matches
     */
    public static boolean matchesCheckoutSessionOwnership(final AbstractOrderModel order, final Session session) {
        final String expectedSiteId = order.getSite().getUid();
        if (Objects.equals(order.getCode(), session.getClientReferenceId())) {
            return hasExpectedSite(session.getMetadata(), expectedSiteId);
        }

        return matchesOrderAndSite(session.getMetadata(), order.getCode(), expectedSiteId);
    }

    /**
     * Returns whether a PaymentIntent belongs to the supplied order and site.
     *
     * @param order expected owner
     * @param paymentIntent PaymentIntent
     * @return {@code true} when ownership matches
     */
    public static boolean matchesPaymentIntentOwnership(final AbstractOrderModel order, final PaymentIntent paymentIntent) {
        return matchesOrderAndSite(paymentIntent.getMetadata(), order.getCode(), order.getSite().getUid());
    }
}
