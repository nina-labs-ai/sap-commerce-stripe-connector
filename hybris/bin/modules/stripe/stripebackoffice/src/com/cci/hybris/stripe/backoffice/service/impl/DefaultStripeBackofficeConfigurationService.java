
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.backoffice.service.impl;

import com.cci.hybris.stripe.backoffice.constants.StripebackofficeConstants;
import com.cci.hybris.stripe.backoffice.data.StripeBackofficeConfigurationStatus;
import com.cci.hybris.stripe.backoffice.service.StripeBackofficeConfigurationService;
import com.cci.hybris.stripe.backoffice.util.StripeBackofficeConfigurationUtils;

import de.hybris.platform.servicelayer.config.ConfigurationService;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;

/**
 * Default masked Stripe configuration reader for support-facing backoffice tooling.
 */
public class DefaultStripeBackofficeConfigurationService implements StripeBackofficeConfigurationService
{
	private final ConfigurationService configurationService;

	/**
	 * Creates the service with the platform configuration source.
	 *
	 * @param configurationService platform configuration service
	 */
	public DefaultStripeBackofficeConfigurationService(final ConfigurationService configurationService)
	{
		this.configurationService = configurationService;
	}

	@Override
	public List<StripeBackofficeConfigurationStatus> getConfigurationStatuses()
	{
		return getConfigurationStatuses(null);
	}

	@Override
	public List<StripeBackofficeConfigurationStatus> getConfigurationStatuses(final String siteId)
	{
		final Configuration configuration = getConfigurationService().getConfiguration();
		final List<StripeBackofficeConfigurationStatus> statuses = new ArrayList<>();

		for (final String key : StripebackofficeConstants.VISIBLE_CONFIGURATION_KEYS)
		{
			final StripeConfigurationStatusContext statusContext = resolveStatusContext(configuration, siteId, key);
			statuses.add(new StripeBackofficeConfigurationStatus(key, statusContext.getEffectiveKey(),
					statusContext.isConfigured(), StripeBackofficeConfigurationUtils.buildDisplayValue(key, statusContext.getResolvedValue(),
							statusContext.isConfigured()), statusContext.getDetailMessage()));
		}

		return statuses;
	}

	/**
	 * Resolves the effective property value and source for a visible Stripe configuration key.
	 *
	 * @param configuration platform configuration
	 * @param siteId optional base site identifier
	 * @param propertyKey configuration key to resolve
	 * @return effective resolution context
	 */
	protected StripeConfigurationStatusContext resolveStatusContext(final Configuration configuration, final String siteId,
			final String propertyKey)
	{
		final String sitePropertyKey = StripeBackofficeConfigurationUtils.buildSitePropertyKey(siteId, propertyKey);
		final String siteValue = StringUtils.isBlank(sitePropertyKey) ? "" : configuration.getString(sitePropertyKey, "");
		final StripeConfigurationStatusContext statusContext;
		if (StripeBackofficeConfigurationUtils.isConfiguredValue(siteValue))
		{
			statusContext = new StripeConfigurationStatusContext(sitePropertyKey, siteValue.trim(), true,
					"Using site override " + sitePropertyKey);
		}
		else
		{
			final String globalValue = configuration.getString(propertyKey, "");
			if (StripeBackofficeConfigurationUtils.isConfiguredValue(globalValue))
			{
				if (StringUtils.isNotBlank(sitePropertyKey) && StripeBackofficeConfigurationUtils.isConfigured(siteValue))
				{
					statusContext = new StripeConfigurationStatusContext(propertyKey, globalValue.trim(), true,
							"Falling back to global " + propertyKey + " because " + sitePropertyKey
									+ " is blank or a placeholder");
				}
				else
				{
					statusContext = new StripeConfigurationStatusContext(propertyKey, globalValue.trim(), true,
							"Using global property " + propertyKey);
				}
			}
			else
			{
				statusContext = new StripeConfigurationStatusContext(StringUtils.defaultIfBlank(sitePropertyKey, propertyKey), "", false,
						StripeBackofficeConfigurationUtils.buildMissingMessage(sitePropertyKey, propertyKey));
			}
		}
		return statusContext;
	}
	protected ConfigurationService getConfigurationService()
	{
		return configurationService;
	}

	/**
	 * Small immutable holder for an effective property resolution.
	 */
	protected static final class StripeConfigurationStatusContext
	{
		private final String effectiveKey;
		private final String resolvedValue;
		private final boolean configured;
		private final String detailMessage;

		StripeConfigurationStatusContext(final String effectiveKey, final String resolvedValue, final boolean configured,
				final String detailMessage)
		{
			this.effectiveKey = effectiveKey;
			this.resolvedValue = resolvedValue;
			this.configured = configured;
			this.detailMessage = detailMessage;
		}

		String getEffectiveKey()
		{
			return effectiveKey;
		}

		String getResolvedValue()
		{
			return resolvedValue;
		}

		boolean isConfigured()
		{
			return configured;
		}

		String getDetailMessage()
		{
			return detailMessage;
		}
	}
}
