package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;

import de.hybris.bootstrap.annotations.IntegrationTest;

import org.junit.Test;

import jakarta.annotation.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Servicelayer integration tests for Stripe configuration resolution.
 */
@IntegrationTest
public class DefaultStripeConfigurationServiceIntegrationTest extends AbstractStripeServicesIntegrationTest {

    @Resource
    private StripeConfigurationService stripeConfigurationService;

    @Test
    public void getSecretKey_siteScopedPropertyConfigured_returnsSiteScopedValue() {
        overrideProperty("stripe-int-site." + StripeServicesConstants.PROPERTY_SECRET_KEY, "sk-config-site-value");

        final String secretKey = stripeConfigurationService.getSecretKey("stripe-int-site");

        assertEquals("sk-config-site-value", secretKey);
    }

    @Test
    public void getSecretKey_siteScopedPlaceholderConfigured_fallsBackToGlobalValue() {
        overrideProperty(StripeServicesConstants.PROPERTY_SECRET_KEY, "sk-config-global-value");
        overrideProperty("stripe-int-site." + StripeServicesConstants.PROPERTY_SECRET_KEY, "insert your secret key");

        final String secretKey = stripeConfigurationService.getSecretKey("stripe-int-site");

        assertEquals("sk-config-global-value", secretKey);
    }

    @Test
    public void getSuccessUrl_placeholderMissing_appendsCheckoutSessionPlaceholder() {
        overrideProperty(StripeServicesConstants.PROPERTY_SUCCESS_URL, "https://example.com/checkout/success-local");

        final String successUrl = stripeConfigurationService.getSuccessUrl("stripe-int-site");

        assertEquals("https://example.com/checkout/success-local?session_id={CHECKOUT_SESSION_ID}", successUrl);
    }

    @Test
    public void getSuccessUrl_invalidUrlConfigured_throwsExceptionWithCheckedKeys() {
        overrideProperty(StripeServicesConstants.PROPERTY_SUCCESS_URL, "not-a-valid-url");

        try {
            stripeConfigurationService.getSuccessUrl("stripe-int-site");
        } catch (final StripeIntegrationException exception) {
            assertTrue(exception.getMessage().contains(StripeServicesConstants.PROPERTY_SUCCESS_URL));
            assertTrue(exception.getMessage()
                    .contains("stripe-int-site." + StripeServicesConstants.PROPERTY_SUCCESS_URL));
            return;
        }

        throw new AssertionError("Expected StripeIntegrationException for invalid Stripe success URL");
    }

    @Test
    public void getElementsReturnUrl_missingExplicitValue_fallsBackToSuccessUrlWithoutPlaceholder() {
        overrideProperty(StripeServicesConstants.PROPERTY_SUCCESS_URL, "https://example.com/fallback/success");
        overrideProperty(StripeServicesConstants.PROPERTY_ELEMENTS_RETURN_URL, "");
        overrideProperty("stripe-int-site." + StripeServicesConstants.PROPERTY_ELEMENTS_RETURN_URL, "");

        final String returnUrl = stripeConfigurationService.getElementsReturnUrl("stripe-int-site");

        assertEquals("https://example.com/fallback/success", returnUrl);
    }

    @Test(expected = StripeIntegrationException.class)
    public void getSecretKey_placeholderConfigured_throwsException() {
        overrideProperty(StripeServicesConstants.PROPERTY_SECRET_KEY, "insert your secret key");
        overrideProperty("stripe-int-site." + StripeServicesConstants.PROPERTY_SECRET_KEY, null);

        stripeConfigurationService.getSecretKey("stripe-int-site");
    }
}
