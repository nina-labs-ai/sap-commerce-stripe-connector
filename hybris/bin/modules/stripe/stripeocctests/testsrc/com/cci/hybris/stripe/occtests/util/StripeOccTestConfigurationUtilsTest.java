
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.occtests.util;

import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit coverage for {@link StripeOccTestConfigurationUtils}.
 */
@UnitTest
public class StripeOccTestConfigurationUtilsTest
{
	private static final String PLACEHOLDER_PREFIX = "insert your";

	@Test
	public void resolveSiteUidReturnsTailSegment()
	{
		assertEquals("electronics-spa", StripeOccTestConfigurationUtils.resolveSiteUid("/occ/v2/electronics-spa"));
	}

	@Test
	public void resolveSiteUidReturnsNullForBlankInput()
	{
		assertNull(StripeOccTestConfigurationUtils.resolveSiteUid(" "));
		assertNull(StripeOccTestConfigurationUtils.resolveSiteUid(null));
	}

	@Test
	public void stripCheckoutSessionPlaceholderRemovesQueryParts()
	{
		assertEquals("https://storefront.test/return",
				StripeOccTestConfigurationUtils.stripCheckoutSessionPlaceholder(
						"https://storefront.test/return?session_id={CHECKOUT_SESSION_ID}", "{CHECKOUT_SESSION_ID}"));
		assertEquals("https://storefront.test/return?lang=en",
				StripeOccTestConfigurationUtils.stripCheckoutSessionPlaceholder(
						"https://storefront.test/return?lang=en&session_id={CHECKOUT_SESSION_ID}", "{CHECKOUT_SESSION_ID}"));
	}

	@Test
	public void isConfiguredValueRejectsPlaceholderOrBlank()
	{
		assertFalse(StripeOccTestConfigurationUtils.isConfiguredValue("insert your test key", PLACEHOLDER_PREFIX));
		assertFalse(StripeOccTestConfigurationUtils.isConfiguredValue("", PLACEHOLDER_PREFIX));
		assertFalse(StripeOccTestConfigurationUtils.isConfiguredValue(" ", PLACEHOLDER_PREFIX));
	}

	@Test
	public void isConfiguredValueAcceptsEffectiveValue()
	{
		assertTrue(StripeOccTestConfigurationUtils.isConfiguredValue("site-publishable-key", PLACEHOLDER_PREFIX));
	}
}
