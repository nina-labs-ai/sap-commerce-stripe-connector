package com.cci.hybris.stripe.occtests;

import de.hybris.bootstrap.annotations.ManualTest;

import org.junit.Test;
import static org.junit.Assert.assertFalse;

@ManualTest
public class StripeOccSuiteMarkerTest
{
	@Test
	public void suiteExecutionIsTriggeredExplicitly()
	{
		// Marker test so SAP's manual test discovery can resolve the explicit suite run.
		assertFalse(Boolean.getBoolean("stripe.occtests.force.fail"));
	}
}
