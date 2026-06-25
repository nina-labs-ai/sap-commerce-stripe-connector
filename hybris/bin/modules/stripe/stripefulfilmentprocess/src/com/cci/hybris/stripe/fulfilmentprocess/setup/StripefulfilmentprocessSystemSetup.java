
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.setup;

import static com.cci.hybris.stripe.fulfilmentprocess.constants.StripefulfilmentprocessConstants.PLATFORM_LOGO_CODE;

import de.hybris.platform.core.initialization.SystemSetup;

import com.cci.hybris.stripe.fulfilmentprocess.constants.StripefulfilmentprocessConstants;
import com.cci.hybris.stripe.fulfilmentprocess.service.StripefulfilmentprocessService;


@SystemSetup(extension = StripefulfilmentprocessConstants.EXTENSIONNAME)
public class StripefulfilmentprocessSystemSetup
{
	private final StripefulfilmentprocessService stripefulfilmentprocessService;

	/**
	 * Creates the system setup with the fulfilment-process support service.
	 *
	 * @param stripefulfilmentprocessService fulfilment-process support service
	 */
	public StripefulfilmentprocessSystemSetup(final StripefulfilmentprocessService stripefulfilmentprocessService)
	{
		this.stripefulfilmentprocessService = stripefulfilmentprocessService;
	}

	@SystemSetup(process = SystemSetup.Process.ALL, type = SystemSetup.Type.ESSENTIAL)
	public void createEssentialData()
	{
		getStripefulfilmentprocessService().createLogo(PLATFORM_LOGO_CODE);
	}

	/**
	 * Exposes setup service dependency for extensible subclasses.
	 *
	 * @return fulfilment-process setup service
	 */
	protected StripefulfilmentprocessService getStripefulfilmentprocessService()
	{
		return stripefulfilmentprocessService;
	}
}
