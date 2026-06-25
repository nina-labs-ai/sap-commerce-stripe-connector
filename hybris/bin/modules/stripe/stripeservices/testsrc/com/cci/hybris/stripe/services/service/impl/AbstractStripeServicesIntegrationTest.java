package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.stripe.Stripe;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.ServicelayerTransactionalTest;
import de.hybris.platform.servicelayer.config.ConfigurationService;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Assume;

import jakarta.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Shared servicelayer integration-test helpers for Stripe services.
 */
public abstract class AbstractStripeServicesIntegrationTest extends ServicelayerTransactionalTest {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String STRIPE_PLACEHOLDER_PREFIX = "insert your";

    @Resource
    protected ModelService modelService;
    @Resource
    protected ConfigurationService configurationService;
    @Resource
    protected CommonI18NService commonI18NService;

    private final Map<String, Object> originalProperties = new LinkedHashMap<>();

    @After
    public void restoreRuntimeProperties() {
        final Configuration configuration = configurationService.getConfiguration();
        originalProperties.forEach((key, value) -> {
            if (value == null) {
                configuration.clearProperty(key);
            } else {
                configuration.setProperty(key, value);
            }
        });
        originalProperties.clear();
    }

    protected void overrideProperty(final String key, final String value) {
        final Configuration configuration = configurationService.getConfiguration();
        if (!originalProperties.containsKey(key)) {
            originalProperties.put(key, configuration.getProperty(key));
        }
        if (value == null) {
            configuration.clearProperty(key);
        } else {
            configuration.setProperty(key, value);
        }
    }

    protected String getRuntimeProperty(final String key) {
        return configurationService.getConfiguration().getString(key);
    }

    protected void prepareLiveStripeCheckoutConfiguration(final String siteUid) {
        final String secretKey = getRuntimeProperty(StripeServicesConstants.PROPERTY_SECRET_KEY);
        final String successUrl = getRuntimeProperty(StripeServicesConstants.PROPERTY_SUCCESS_URL);
        final String cancelUrl = getRuntimeProperty(StripeServicesConstants.PROPERTY_CANCEL_URL);

        Assume.assumeTrue("Stripe secret key must be configured for live integration tests.",
                isConfiguredValue(secretKey));
        Assume.assumeTrue("Stripe success URL must be configured for live integration tests.",
                isConfiguredValue(successUrl));
        Assume.assumeTrue("Stripe cancel URL must be configured for live integration tests.",
                isConfiguredValue(cancelUrl));

        overrideProperty(siteUid + "." + StripeServicesConstants.PROPERTY_SECRET_KEY, secretKey);
        overrideProperty(siteUid + "." + StripeServicesConstants.PROPERTY_SUCCESS_URL, successUrl);
        overrideProperty(siteUid + "." + StripeServicesConstants.PROPERTY_CANCEL_URL, cancelUrl);
    }

    protected void prepareLiveStripeWebhookConfiguration(final String siteUid) {
        final String webhookSecret = getRuntimeProperty(StripeServicesConstants.PROPERTY_WEBHOOK_SECRET);

        prepareLiveStripeCheckoutConfiguration(siteUid);
        Assume.assumeTrue("Stripe webhook secret must be configured for webhook integration tests.",
                isConfiguredValue(webhookSecret));
        overrideProperty(siteUid + "." + StripeServicesConstants.PROPERTY_WEBHOOK_SECRET, webhookSecret);
    }

    protected OrderModel createOrder(final String orderCode, final String siteUid) {
        final BaseSiteModel site = modelService.create(BaseSiteModel.class);
        site.setUid(siteUid);
        modelService.save(site);

        final CustomerModel customer = modelService.create(CustomerModel.class);
        customer.setUid(orderCode.toLowerCase(Locale.ROOT) + "@example.com");
        customer.setName("Stripe Integration Tester");
        modelService.save(customer);

        final OrderModel order = modelService.create(OrderModel.class);
        order.setCode(orderCode);
        order.setDate(new Date());
        order.setCurrency(resolveUsdCurrency());
        order.setUser(customer);
        order.setSite(site);
        order.setNet(Boolean.FALSE);
        order.setTotalPrice(Double.valueOf(10.00d));
        order.setTotalTax(Double.valueOf(2.34d));
        order.setCalculated(Boolean.TRUE);
        order.setPaymentTransactions(new ArrayList<>());
        modelService.save(order);
        return order;
    }

    protected CurrencyModel resolveUsdCurrency() {
        try {
            return commonI18NService.getCurrency("USD");
        } catch (final RuntimeException exception) {
            final CurrencyModel currency = modelService.create(CurrencyModel.class);
            currency.setIsocode("USD");
            currency.setDigits(Integer.valueOf(2));
            currency.setSymbol("$");
            currency.setActive(Boolean.TRUE);
            modelService.save(currency);
            return currency;
        }
    }

    protected PaymentTransactionModel getSingleTransaction(final OrderModel order) {
        modelService.refresh(order);
        assertNotNull(order.getPaymentTransactions());
        assertEquals(1, order.getPaymentTransactions().size());
        return order.getPaymentTransactions().get(0);
    }

    protected PaymentTransactionEntryModel findEntry(final PaymentTransactionModel transaction,
                                                     final PaymentTransactionType type,
                                                     final String requestId) {
        final List<PaymentTransactionEntryModel> entries = transaction.getEntries();
        assertNotNull(entries);
        for (final PaymentTransactionEntryModel entry : entries) {
            if (type.equals(entry.getType()) && requestId.equals(entry.getRequestId())) {
                return entry;
            }
        }
        return null;
    }

    protected String uniqueCode(final String prefix) {
        return prefix + "-" + System.currentTimeMillis();
    }

    protected String createSignedStripeHeader(final String payload, final String webhookSecret) throws Exception {
        final long timestamp = System.currentTimeMillis() / 1000L;
        final String signedPayload = timestamp + "." + payload;

        final Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));

        return "t=" + timestamp + ",v1=" + toHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
    }

    protected String buildCheckoutSessionEventPayload(final String eventId,
                                                      final String eventType,
                                                      final String sessionId,
                                                      final String sessionUrl,
                                                      final String orderCode,
                                                      final String siteUid,
                                                      final String status,
                                                      final String paymentStatus) {
        return "{"
                + "\"id\":\"" + escapeJson(eventId) + "\","
                + "\"object\":\"event\","
                + "\"api_version\":\"" + escapeJson(Stripe.API_VERSION) + "\","
                + "\"type\":\"" + escapeJson(eventType) + "\","
                + "\"data\":{\"object\":{"
                + "\"id\":\"" + escapeJson(sessionId) + "\","
                + "\"object\":\"checkout.session\","
                + "\"url\":\"" + escapeJson(sessionUrl) + "\","
                + "\"status\":\"" + escapeJson(status) + "\","
                + "\"payment_status\":\"" + escapeJson(paymentStatus) + "\","
                + "\"client_reference_id\":\"" + escapeJson(orderCode) + "\","
                + "\"metadata\":{"
                + "\"orderCode\":\"" + escapeJson(orderCode) + "\","
                + "\"siteUid\":\"" + escapeJson(siteUid) + "\","
                + "\"paymentFlow\":\"checkout\","
                + "\"orderType\":\"OrderModel\""
                + "}"
                + "}}"
                + "}";
    }

    protected String buildPaymentIntentEventPayload(final String eventId,
                                                    final String eventType,
                                                    final String paymentIntentId,
                                                    final String orderCode,
                                                    final String siteUid,
                                                    final String status) {
        return "{"
                + "\"id\":\"" + escapeJson(eventId) + "\","
                + "\"object\":\"event\","
                + "\"api_version\":\"" + escapeJson(Stripe.API_VERSION) + "\","
                + "\"type\":\"" + escapeJson(eventType) + "\","
                + "\"data\":{\"object\":{"
                + "\"id\":\"" + escapeJson(paymentIntentId) + "\","
                + "\"object\":\"payment_intent\","
                + "\"status\":\"" + escapeJson(status) + "\","
                + "\"currency\":\"usd\","
                + "\"amount\":1000,"
                + "\"client_secret\":\"" + escapeJson(paymentIntentId) + "_secret\","
                + "\"metadata\":{"
                + "\"orderCode\":\"" + escapeJson(orderCode) + "\","
                + "\"siteUid\":\"" + escapeJson(siteUid) + "\","
                + "\"paymentFlow\":\"elements\","
                + "\"orderType\":\"OrderModel\""
                + "}"
                + "}}"
                + "}";
    }

    protected String toHex(final byte[] bytes) {
        final StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (final byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02x", Integer.valueOf(value & 0xff)));
        }
        return builder.toString();
    }

    protected String escapeJson(final String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    protected boolean isConfiguredValue(final String value) {
        return StringUtils.isNotBlank(value) && !value.startsWith(STRIPE_PLACEHOLDER_PREFIX);
    }
}
