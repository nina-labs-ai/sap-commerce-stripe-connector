/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.setup;

import com.cci.hybris.stripe.fulfilmentprocess.constants.StripefulfilmentprocessConstants;
import com.cci.hybris.stripe.fulfilmentprocess.service.StripefulfilmentprocessService;
import de.hybris.bootstrap.annotations.UnitTest;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@UnitTest
public class StripefulfilmentprocessSystemSetupTest
{
	@Test
	public void createEssentialData_delegatesToService()
	{
		final StripefulfilmentprocessService service = mock(StripefulfilmentprocessService.class);
		final ExposedStripefulfilmentprocessSystemSetup setup = new ExposedStripefulfilmentprocessSystemSetup(service);

		setup.createEssentialData();

		verify(service).createLogo(StripefulfilmentprocessConstants.PLATFORM_LOGO_CODE);
	}

	@Test
	public void getStripefulfilmentprocessService_returnsInjectedDependency()
	{
		final StripefulfilmentprocessService service = mock(StripefulfilmentprocessService.class);
		final ExposedStripefulfilmentprocessSystemSetup setup = new ExposedStripefulfilmentprocessSystemSetup(service);
		assertSame(service, setup.exposedStripefulfilmentprocessService());
	}

	private static class ExposedStripefulfilmentprocessSystemSetup extends StripefulfilmentprocessSystemSetup
	{
		private ExposedStripefulfilmentprocessSystemSetup(final StripefulfilmentprocessService stripefulfilmentprocessService)
		{
			super(stripefulfilmentprocessService);
		}

		private StripefulfilmentprocessService exposedStripefulfilmentprocessService()
		{
			return getStripefulfilmentprocessService();
		}
	}
}
