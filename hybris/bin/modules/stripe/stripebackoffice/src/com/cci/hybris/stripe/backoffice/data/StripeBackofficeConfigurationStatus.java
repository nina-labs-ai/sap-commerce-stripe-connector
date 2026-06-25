
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.backoffice.data;

/**
 * Small immutable view of one Stripe-related property for later backoffice use.
 */
public class StripeBackofficeConfigurationStatus
{
	private final String key;
	private final String effectiveKey;
	private final boolean configured;
	private final String displayValue;
	private final String detailMessage;

	/**
	 * Creates one immutable backoffice configuration status view.
	 *
	 * @param key logical configuration key
	 * @param effectiveKey resolved property key
	 * @param configured whether the value is configured
	 * @param displayValue support-safe display value
	 * @param detailMessage resolution detail message
	 */
	public StripeBackofficeConfigurationStatus(final String key, final String effectiveKey, final boolean configured,
			final String displayValue, final String detailMessage)
	{
		this.key = key;
		this.effectiveKey = effectiveKey;
		this.configured = configured;
		this.displayValue = displayValue;
		this.detailMessage = detailMessage;
	}

	public String getKey()
	{
		return key;
	}

	public String getEffectiveKey()
	{
		return effectiveKey;
	}

	public boolean isConfigured()
	{
		return configured;
	}

	public String getDisplayValue()
	{
		return displayValue;
	}

	public String getDetailMessage()
	{
		return detailMessage;
	}
}
