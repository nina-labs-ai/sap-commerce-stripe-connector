package com.cci.hybris.stripe.occtests;

import com.cci.hybris.stripe.occtests.constants.StripeocctestsConstants;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

@UnitTest
public class StripeOccTestsSmokeTest
{
	@Test
	public void extensionName_matchesDescriptor()
	{
		assertEquals("stripeocctests", StripeocctestsConstants.EXTENSIONNAME);
	}
}
