
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.occtests.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Stateless helpers shared by Stripe OCC integration tests.
 */
public final class StripeOccTestConfigurationUtils
{
	private StripeOccTestConfigurationUtils()
	{
		// utility class
	}

	/**
	 * Resolves the current site uid from a base OCC path.
	 *
	 * @param basePath path in the form {@code /occ/v2/{siteUid}}
	 * @return current site uid or {@code null}
	 */
	public static String resolveSiteUid(final String basePath)
	{
		if (StringUtils.isBlank(basePath))
		{
			return null;
		}
		final List<String> pathSegments = List.of(StringUtils.split(basePath, '/'));
		if (pathSegments.isEmpty())
		{
			return null;
		}
		return pathSegments.get(pathSegments.size() - 1);
	}

	/**
	 * Removes checkout-session placeholder fragments from configured return urls.
	 *
	 * @param url source url
	 * @param checkoutSessionPlaceholder session placeholder token
	 * @return cleaned url string
	 */
	public static String stripCheckoutSessionPlaceholder(final String url, final String checkoutSessionPlaceholder)
	{
		return StringUtils.defaultString(url)
				.replace("?session_id=" + checkoutSessionPlaceholder, StringUtils.EMPTY)
				.replace("&session_id=" + checkoutSessionPlaceholder, StringUtils.EMPTY);
	}

	/**
	 * Determines whether a runtime property is configured with an effective value.
	 *
	 * @param value runtime property value
	 * @param placeholderPrefix known placeholder prefix
	 * @return {@code true} when the value is non-blank and not a placeholder
	 */
	public static boolean isConfiguredValue(final String value, final String placeholderPrefix)
	{
		return StringUtils.isNotBlank(value) && !value.startsWith(placeholderPrefix);
	}
}
