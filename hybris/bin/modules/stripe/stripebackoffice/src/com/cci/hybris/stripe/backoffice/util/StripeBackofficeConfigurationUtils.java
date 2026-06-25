
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.backoffice.util;

import com.cci.hybris.stripe.backoffice.constants.StripebackofficeConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Stateless helpers for Stripe backoffice configuration resolution and display masking.
 */
public final class StripeBackofficeConfigurationUtils
{
	private static final int VISIBLE_PREFIX_LENGTH = 4;
	private static final int VISIBLE_SUFFIX_LENGTH = 4;
	private static final String MASK_PREFIX = "****";
	private static final String SITE_PROPERTY_SEPARATOR = ".";
	private static final String PLACEHOLDER_PREFIX = "insert your";

	private StripeBackofficeConfigurationUtils()
	{
		// utility class
	}

	/**
	 * Checks whether a property contains a non-blank value.
	 *
	 * @param value property value
	 * @return {@code true} when value is non-blank
	 */
	public static boolean isConfigured(final String value)
	{
		return StringUtils.isNotBlank(value);
	}

	/**
	 * Checks whether a property contains a usable non-placeholder value.
	 *
	 * @param value property value
	 * @return {@code true} when configured and not an "insert your" placeholder
	 */
	public static boolean isConfiguredValue(final String value)
	{
		return isConfigured(value) && !StringUtils.trim(value).toLowerCase(Locale.ROOT).startsWith(PLACEHOLDER_PREFIX);
	}

	/**
	 * Builds a site-scoped property key.
	 *
	 * @param siteId base site identifier
	 * @param propertyKey global property key
	 * @return site property key or {@code null} when site is blank
	 */
	public static String buildSitePropertyKey(final String siteId, final String propertyKey)
	{
		if (StringUtils.isBlank(siteId))
		{
			return null;
		}
		return siteId + SITE_PROPERTY_SEPARATOR + propertyKey;
	}

	/**
	 * Builds the message used when neither site nor global property is configured.
	 *
	 * @param sitePropertyKey site-scoped property key
	 * @param propertyKey global property key
	 * @return missing-configuration message
	 */
	public static String buildMissingMessage(final String sitePropertyKey, final String propertyKey)
	{
		if (StringUtils.isBlank(sitePropertyKey))
		{
			return "Missing global property " + propertyKey;
		}
		return "Missing configuration. Checked " + sitePropertyKey + " and " + propertyKey;
	}

	/**
	 * Builds the backoffice display value for configured properties.
	 *
	 * @param key resolved configuration key
	 * @param rawValue configured value
	 * @param configured configuration flag
	 * @return masked or abbreviated display value
	 */
	public static String buildDisplayValue(final String key, final String rawValue, final boolean configured)
	{
		final String displayValue;
		if (!configured)
		{
			displayValue = "";
		}
		else
		{
			final String value = StringUtils.defaultString(rawValue);
			if (isSecretProperty(key))
			{
				displayValue = maskSecret(value);
			}
			else if (value.length() <= VISIBLE_PREFIX_LENGTH + VISIBLE_SUFFIX_LENGTH)
			{
				displayValue = value;
			}
			else
			{
				displayValue = value.substring(0, VISIBLE_PREFIX_LENGTH) + "..."
						+ value.substring(value.length() - VISIBLE_SUFFIX_LENGTH);
			}
		}
		return displayValue;
	}

	/**
	 * Checks whether a key points to secret material.
	 *
	 * @param key configuration key
	 * @return {@code true} when the key is secret
	 */
	public static boolean isSecretProperty(final String key)
	{
		return StripebackofficeConstants.STRIPE_SECRET_KEY.equals(key)
				|| StripebackofficeConstants.STRIPE_WEBHOOK_SECRET.equals(key);
	}

	private static String maskSecret(final String value)
	{
		if (value.length() <= VISIBLE_SUFFIX_LENGTH)
		{
			return MASK_PREFIX;
		}
		return MASK_PREFIX + value.substring(value.length() - VISIBLE_SUFFIX_LENGTH);
	}
}
