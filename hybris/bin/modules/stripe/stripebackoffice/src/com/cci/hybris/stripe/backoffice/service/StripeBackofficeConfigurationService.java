
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.backoffice.service;

import com.cci.hybris.stripe.backoffice.data.StripeBackofficeConfigurationStatus;

import java.util.List;

/**
 * Exposes a masked view of Stripe configuration values for later backoffice support tooling.
 */
public interface StripeBackofficeConfigurationService
{
	List<StripeBackofficeConfigurationStatus> getConfigurationStatuses();
	List<StripeBackofficeConfigurationStatus> getConfigurationStatuses(String siteId);
}
