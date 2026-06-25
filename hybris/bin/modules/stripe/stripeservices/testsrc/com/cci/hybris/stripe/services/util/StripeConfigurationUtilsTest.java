package com.cci.hybris.stripe.services.util;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

import de.hybris.bootstrap.annotations.UnitTest;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripeConfigurationUtilsTest {

    @Test
    public void buildSitePropertyName_withSite_returnsSiteScopedName() {
        assertEquals("apparel.stripe.secret.key",
                StripeConfigurationUtils.buildSitePropertyName("apparel", StripeServicesConstants.PROPERTY_SECRET_KEY));
    }

    @Test
    public void buildSitePropertyName_withoutSite_returnsNull() {
        assertNull(StripeConfigurationUtils.buildSitePropertyName(null, StripeServicesConstants.PROPERTY_SECRET_KEY));
    }

    @Test
    public void getCheckedPropertyNames_withSite_containsSiteAndGlobal() {
        final List<String> checked = StripeConfigurationUtils.getCheckedPropertyNames("electronics",
                StripeServicesConstants.PROPERTY_SUCCESS_URL);
        assertEquals(2, checked.size());
        assertEquals("electronics." + StripeServicesConstants.PROPERTY_SUCCESS_URL, checked.get(0));
        assertEquals(StripeServicesConstants.PROPERTY_SUCCESS_URL, checked.get(1));
    }

    @Test
    public void isPlaceholderValue_withSetupPlaceholder_returnsTrue() {
        assertTrue(StripeConfigurationUtils.isPlaceholderValue("insert your key"));
    }

    @Test
    public void validateAbsoluteUrl_withPlaceholder_returnsOriginalValue() {
        final String url = "https://example.local/return?session_id={CHECKOUT_SESSION_ID}";
        assertEquals(url, StripeConfigurationUtils.validateAbsoluteUrl("electronics",
                StripeServicesConstants.PROPERTY_SUCCESS_URL, url));
    }

    @Test(expected = StripeIntegrationException.class)
    public void validateAbsoluteUrl_withInvalidScheme_throwsException() {
        StripeConfigurationUtils.validateAbsoluteUrl("electronics",
                StripeServicesConstants.PROPERTY_SUCCESS_URL, "mailto://example.local/return");
    }
}
