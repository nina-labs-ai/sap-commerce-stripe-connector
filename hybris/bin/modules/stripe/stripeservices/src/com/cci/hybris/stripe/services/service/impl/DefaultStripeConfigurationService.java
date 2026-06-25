
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;
import com.cci.hybris.stripe.services.service.StripeConfigurationService;
import com.cci.hybris.stripe.services.util.StripeConfigurationUtils;
import com.cci.hybris.stripe.services.util.StripeUrlUtils;

import de.hybris.platform.servicelayer.config.ConfigurationService;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;

/**
 * Reads Stripe configuration from local properties.
 */
public class DefaultStripeConfigurationService implements StripeConfigurationService {

    private final ConfigurationService configurationService;

    /**
     * Creates the configuration service backed by SAP Commerce properties.
     *
     * @param configurationService configuration service
     */
    public DefaultStripeConfigurationService(final ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
    @Override
    public String getSecretKey(final String siteId) {
        return getRequiredProperty(siteId, StripeServicesConstants.PROPERTY_SECRET_KEY);
    }
    @Override
    public String getPublishableKey(final String siteId) {
        return getRequiredProperty(siteId, StripeServicesConstants.PROPERTY_PUBLISHABLE_KEY);
    }
    @Override
    public String getWebhookSecret(final String siteId) {
        return getRequiredProperty(siteId, StripeServicesConstants.PROPERTY_WEBHOOK_SECRET);
    }
    @Override
    public String getSuccessUrl(final String siteId) {
        return StripeUrlUtils.appendSessionPlaceholder(validateAbsoluteUrl(siteId, StripeServicesConstants.PROPERTY_SUCCESS_URL,
                getRequiredProperty(siteId, StripeServicesConstants.PROPERTY_SUCCESS_URL)));
    }
    @Override
    public String getCancelUrl(final String siteId) {
        return StripeUrlUtils.appendSessionPlaceholder(validateAbsoluteUrl(siteId, StripeServicesConstants.PROPERTY_CANCEL_URL,
                getRequiredProperty(siteId, StripeServicesConstants.PROPERTY_CANCEL_URL)));
    }
    @Override
    public String getElementsReturnUrl(final String siteId) {
        final String configuredValue = getOptionalProperty(siteId, StripeServicesConstants.PROPERTY_ELEMENTS_RETURN_URL);

        if (configuredValue != null) {
            return validateAbsoluteUrl(siteId, StripeServicesConstants.PROPERTY_ELEMENTS_RETURN_URL, configuredValue);
        }

        return StripeUrlUtils.stripSessionPlaceholder(getSuccessUrl(siteId));
    }
    protected String getRequiredProperty(final String siteId, final String propertyName) {
        final String value = getOptionalProperty(siteId, propertyName);

        if (value == null) {
            throw new StripeIntegrationException(StripeConfigurationUtils.buildMissingPropertyMessage(siteId, propertyName));
        }

        return value;
    }
    protected String getOptionalProperty(final String siteId, final String propertyName) {
        final Configuration configuration = getConfigurationService().getConfiguration();
        final String sitePropertyName = StripeConfigurationUtils.buildSitePropertyName(siteId, propertyName);
        final String siteValue = StringUtils.isBlank(sitePropertyName) ? null : configuration.getString(sitePropertyName);
        if (siteValue != null && StripeConfigurationUtils.isUsableValue(siteValue)) {
            return siteValue.trim();
        }

        final String globalValue = configuration.getString(propertyName);
        if (globalValue != null && StripeConfigurationUtils.isUsableValue(globalValue)) {
            return globalValue.trim();
        }

        return null;
    }

    /**
     * Validates that the supplied property value is an absolute HTTP or HTTPS URL.
     *
     * @param siteId base-site identifier
     * @param propertyName property name
     * @param url configured URL
     * @return validated URL
     */
    protected String validateAbsoluteUrl(final String siteId, final String propertyName, final String url) {
        return StripeConfigurationUtils.validateAbsoluteUrl(siteId, propertyName, url);
    }
    protected ConfigurationService getConfigurationService() {
        return configurationService;
    }
}
