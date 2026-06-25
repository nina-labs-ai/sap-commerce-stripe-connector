
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.service;

/**
 * Provides fulfilment-process support assets for the Stripe connector.
 */
public interface StripefulfilmentprocessService
{
	String getHybrisLogoUrl(String logoCode);

	/**
	 * Creates the fulfilment-process logo media when it does not already exist.
	 *
	 * @param logoCode media code to create or update
	 */
	void createLogo(String logoCode);
}
