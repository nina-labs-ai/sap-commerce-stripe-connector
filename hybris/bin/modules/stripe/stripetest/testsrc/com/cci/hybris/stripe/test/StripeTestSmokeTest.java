package com.cci.hybris.stripe.test;

import com.cci.hybris.stripe.test.constants.StripetestConstants;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Basic assertions for stripetest extension descriptors.
 */
@UnitTest
public class StripeTestSmokeTest
{
	/**
	 * Verifies extension constants align with descriptor expectations.
	 */
	@Test
	public void extensionConstantsMatchDescriptorValues()
	{
		assertEquals("stripetest", StripetestConstants.EXTENSIONNAME);
		assertEquals("stripetestPlatformLogo", StripetestConstants.PLATFORM_LOGO_CODE);
	}
}
