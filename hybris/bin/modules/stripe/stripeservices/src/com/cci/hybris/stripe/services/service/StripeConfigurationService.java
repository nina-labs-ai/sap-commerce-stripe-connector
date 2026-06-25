
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service;

/**
 * Resolves Stripe configuration values with optional site-specific overrides.
 */
public interface StripeConfigurationService {
    String getSecretKey(String siteId);
    String getPublishableKey(String siteId);
    String getWebhookSecret(String siteId);
    String getSuccessUrl(String siteId);
    String getCancelUrl(String siteId);
    String getElementsReturnUrl(String siteId);
}
