package com.cci.hybris.stripe.services.util;

import com.google.gson.JsonObject;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import de.hybris.bootstrap.annotations.UnitTest;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripeWebhookPayloadUtilsTest {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String WEBHOOK_SECRET = "webhook-secret-test";

    @Test
    public void extractSession_withSessionStripeObject_returnsSession() {
        final Session session = new Session();
        assertTrue(StripeWebhookPayloadUtils.extractSession(session).isPresent());
    }

    @Test
    public void extractPaymentIntent_withPaymentIntentStripeObject_returnsPaymentIntent() {
        final PaymentIntent paymentIntent = new PaymentIntent();
        assertTrue(StripeWebhookPayloadUtils.extractPaymentIntent(paymentIntent).isPresent());
    }

    @Test
    public void extractSession_withWebhookApiVersionMismatch_usesUnsafeDeserializer() throws Exception {
        final Event event = constructEvent(buildEventPayload("checkout.session", "cs_test_123"));

        final Optional<Session> session = StripeWebhookPayloadUtils.extractSession(event);

        assertTrue(session.isPresent());
        assertEquals("cs_test_123", session.get().getId());
    }

    @Test
    public void extractPaymentIntent_withWebhookApiVersionMismatch_usesUnsafeDeserializer() throws Exception {
        final Event event = constructEvent(buildEventPayload("payment_intent", "pi_test_123"));

        final Optional<PaymentIntent> paymentIntent = StripeWebhookPayloadUtils.extractPaymentIntent(event);

        assertTrue(paymentIntent.isPresent());
        assertEquals("pi_test_123", paymentIntent.get().getId());
    }

    @Test
    public void resolveVerificationSiteId_withRequestedSite_prefersRequestedSite() {
        assertEquals("electronics", StripeWebhookPayloadUtils.resolveVerificationSiteId("{}", "electronics"));
    }

    @Test
    public void extractSiteIdFromPayload_withMetadata_returnsTrimmedSiteId() {
        final String payload = "{\"data\":{\"object\":{\"metadata\":{\"siteUid\":\" electronics \"}}}}";
        assertEquals("electronics", StripeWebhookPayloadUtils.extractSiteIdFromPayload(payload));
    }

    @Test
    public void extractSiteIdFromPayload_withoutMetadata_returnsNull() {
        assertNull(StripeWebhookPayloadUtils.extractSiteIdFromPayload("{\"data\":{\"object\":{}}}"));
    }

    @Test
    public void resolveMetadata_withNestedPayload_returnsMetadataObject() {
        final JsonObject metadata = StripeWebhookPayloadUtils.resolveMetadata(
                "{\"data\":{\"object\":{\"metadata\":{\"k\":\"v\"}}}}");
        assertEquals("v", StripeWebhookPayloadUtils.extractTrimmedValue(metadata, "k"));
        assertFalse(metadata.isJsonNull());
    }

    protected Event constructEvent(final String payload) throws Exception {
        return Webhook.constructEvent(payload, createSignedStripeHeader(payload), WEBHOOK_SECRET);
    }

    protected String buildEventPayload(final String objectType, final String objectId) {
        return "{"
                + "\"id\":\"evt_test\","
                + "\"object\":\"event\","
                + "\"api_version\":\"2020-08-27\","
                + "\"type\":\"" + objectType + ".updated\","
                + "\"data\":{\"object\":{"
                + "\"id\":\"" + objectId + "\","
                + "\"object\":\"" + objectType + "\","
                + "\"metadata\":{"
                + "\"siteUid\":\"apparel-uk\","
                + "\"paymentFlow\":\"checkout\""
                + "}"
                + "}}"
                + "}";
    }

    protected String createSignedStripeHeader(final String payload) throws Exception {
        final long timestamp = System.currentTimeMillis() / 1000L;
        final String signedPayload = timestamp + "." + payload;

        final Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));

        return "t=" + timestamp + ",v1=" + toHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
    }

    protected String toHex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (final byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(value & 0xff)));
        }
        return builder.toString();
    }
}
