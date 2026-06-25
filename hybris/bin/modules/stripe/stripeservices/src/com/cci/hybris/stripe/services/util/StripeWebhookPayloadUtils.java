
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Webhook payload helper utilities for Stripe event/object extraction.
 */
public final class StripeWebhookPayloadUtils {

    private static final String JSON_KEY_DATA = "data";
    private static final String JSON_KEY_OBJECT = "object";
    private static final String JSON_KEY_METADATA = "metadata";

    private StripeWebhookPayloadUtils() {
    }

    /**
     * Extracts a Checkout Session object from a Stripe event.
     *
     * @param event Stripe event
     * @return Checkout Session when present
     */
    public static Optional<Session> extractSession(final Event event) {
        return extractSession(extractStripeObject(event));
    }

    /**
     * Extracts a Checkout Session from a Stripe object.
     *
     * @param stripeObject Stripe object
     * @return Checkout Session when present
     */
    public static Optional<Session> extractSession(final StripeObject stripeObject) {
        return stripeObject instanceof Session ? Optional.of((Session) stripeObject) : Optional.empty();
    }

    /**
     * Extracts a PaymentIntent object from a Stripe event.
     *
     * @param event Stripe event
     * @return PaymentIntent when present
     */
    public static Optional<PaymentIntent> extractPaymentIntent(final Event event) {
        return extractPaymentIntent(extractStripeObject(event));
    }

    /**
     * Extracts a PaymentIntent from a Stripe object.
     *
     * @param stripeObject Stripe object
     * @return PaymentIntent when present
     */
    public static Optional<PaymentIntent> extractPaymentIntent(final StripeObject stripeObject) {
        return stripeObject instanceof PaymentIntent ? Optional.of((PaymentIntent) stripeObject) : Optional.empty();
    }

    /**
     * Extracts the Stripe object from a Stripe event payload.
     *
     * @param event Stripe event
     * @return Stripe object or {@code null}
     */
    public static StripeObject extractStripeObject(final Event event) {
        if (event == null || event.getData() == null) {
            return null;
        }

        final EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer == null) {
            return null;
        }

        final Optional<StripeObject> stripeObject = deserializer.getObject();
        if (stripeObject.isPresent()) {
            return stripeObject.get();
        }

        try {
            return deserializer.deserializeUnsafe();
        } catch (final EventDataObjectDeserializationException exception) {
            return null;
        }
    }

    /**
     * Resolves site id used for webhook signature verification.
     *
     * @param payload raw webhook payload
     * @param requestedSiteId caller-provided site id
     * @return resolved site id
     */
    public static String resolveVerificationSiteId(final String payload, final String requestedSiteId) {
        return StringUtils.isNotBlank(requestedSiteId) ? requestedSiteId : extractSiteIdFromPayload(payload);
    }

    /**
     * Extracts site id from payload metadata.
     *
     * @param payload raw webhook payload
     * @return site id or {@code null}
     */
    public static String extractSiteIdFromPayload(final String payload) {
        if (StringUtils.isBlank(payload)) {
            return null;
        }

        try {
            final JsonObject metadata = resolveMetadata(payload);
            return extractTrimmedValue(metadata, StripeServicesConstants.METADATA_SITE_UID);
        } catch (final IllegalStateException exception) {
            return null;
        }
    }

    /**
     * Resolves metadata object from payload.
     *
     * @param payload raw webhook payload
     * @return metadata object or {@code null}
     */
    public static JsonObject resolveMetadata(final String payload) {
        final JsonElement root = JsonParser.parseString(payload);
        final JsonObject data = getChildObject(root, JSON_KEY_DATA);
        final JsonObject object = getChildObject(data, JSON_KEY_OBJECT);
        return getChildObject(object, JSON_KEY_METADATA);
    }

    /**
     * Returns a named child object from a parent json element.
     *
     * @param parent parent element
     * @param key child key
     * @return child object or {@code null}
     */
    public static JsonObject getChildObject(final JsonElement parent, final String key) {
        if (parent == null || !parent.isJsonObject()) {
            return null;
        }

        final JsonElement child = parent.getAsJsonObject().get(key);
        return child != null && child.isJsonObject() ? child.getAsJsonObject() : null;
    }

    /**
     * Returns a trimmed string value for a metadata key.
     *
     * @param metadata metadata json object
     * @param key metadata key
     * @return trimmed value or {@code null}
     */
    public static String extractTrimmedValue(final JsonObject metadata, final String key) {
        if (metadata == null) {
            return null;
        }

        final JsonElement value = metadata.get(key);
        return value == null || value.isJsonNull() ? null : StringUtils.trimToNull(value.getAsString());
    }
}
