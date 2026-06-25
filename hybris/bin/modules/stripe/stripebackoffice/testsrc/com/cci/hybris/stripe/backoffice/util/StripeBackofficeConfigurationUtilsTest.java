/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.backoffice.util;

import com.cci.hybris.stripe.backoffice.constants.StripebackofficeConstants;
import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@UnitTest
public class StripeBackofficeConfigurationUtilsTest
{
	@Test
	public void isConfigured_handlesNullAndBlank()
	{
		assertFalse(StripeBackofficeConfigurationUtils.isConfigured(null));
		assertFalse(StripeBackofficeConfigurationUtils.isConfigured(" "));
		assertTrue(StripeBackofficeConfigurationUtils.isConfigured("value"));
	}

	@Test
	public void isConfiguredValue_rejectsPlaceholder()
	{
		assertFalse(StripeBackofficeConfigurationUtils.isConfiguredValue(" Insert Your publishable key "));
		assertTrue(StripeBackofficeConfigurationUtils.isConfiguredValue("configured-publishable-key"));
	}

	@Test
	public void buildSitePropertyKey_returnsNullWhenSiteMissing()
	{
		assertNull(StripeBackofficeConfigurationUtils.buildSitePropertyKey(null, "stripe.key"));
		assertNull(StripeBackofficeConfigurationUtils.buildSitePropertyKey(" ", "stripe.key"));
		assertEquals("electronics.stripe.key",
				StripeBackofficeConfigurationUtils.buildSitePropertyKey("electronics", "stripe.key"));
	}

	@Test
	public void buildMissingMessage_usesExpectedText()
	{
		assertEquals("Missing global property stripe.secret.key",
				StripeBackofficeConfigurationUtils.buildMissingMessage(null, "stripe.secret.key"));
		assertEquals("Missing configuration. Checked electronics.stripe.secret.key and stripe.secret.key",
				StripeBackofficeConfigurationUtils.buildMissingMessage("electronics.stripe.secret.key", "stripe.secret.key"));
	}

	@Test
	public void buildDisplayValue_masksSecretsAndShortensLongValues()
	{
		assertEquals("", StripeBackofficeConfigurationUtils.buildDisplayValue("stripe.any", "abc", false));
		assertEquals("****3456",
				StripeBackofficeConfigurationUtils.buildDisplayValue(StripebackofficeConstants.STRIPE_SECRET_KEY,
						"secret-key-123456", true));
		assertEquals("****",
				StripeBackofficeConfigurationUtils.buildDisplayValue(StripebackofficeConstants.STRIPE_WEBHOOK_SECRET,
						"whs", true));
		assertEquals("publ...3456",
				StripeBackofficeConfigurationUtils.buildDisplayValue(StripebackofficeConstants.STRIPE_PUBLISHABLE_KEY,
						"publishable-key-123456", true));
		assertEquals("12345678", StripeBackofficeConfigurationUtils.buildDisplayValue("stripe.any", "12345678", true));
	}

	@Test
	public void isSecretProperty_recognizesOnlySecretKeys()
	{
		assertTrue(StripeBackofficeConfigurationUtils.isSecretProperty(StripebackofficeConstants.STRIPE_SECRET_KEY));
		assertTrue(StripeBackofficeConfigurationUtils.isSecretProperty(StripebackofficeConstants.STRIPE_WEBHOOK_SECRET));
		assertFalse(StripeBackofficeConfigurationUtils.isSecretProperty(StripebackofficeConstants.STRIPE_PUBLISHABLE_KEY));
	}
}
