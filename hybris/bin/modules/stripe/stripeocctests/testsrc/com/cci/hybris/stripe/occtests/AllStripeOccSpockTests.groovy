package com.cci.hybris.stripe.occtests

import com.cci.hybris.stripe.occtests.controllers.StripeCheckoutControllerIntegrationTest
import com.cci.hybris.stripe.occtests.controllers.StripePaymentElementControllerIntegrationTest
import com.cci.hybris.stripe.occtests.controllers.StripeRefundControllerIntegrationTest
import de.hybris.bootstrap.annotations.IntegrationTest
import de.hybris.bootstrap.annotations.ManualTest
import de.hybris.platform.commercewebservicestests.setup.ResourceServerTestManager
import de.hybris.platform.commercewebservicestests.setup.TestSetupUtils
import de.hybris.platform.core.Registry
import de.hybris.platform.testframework.JUnit4OnlyTest
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite.class)
@Suite.SuiteClasses([StripeCheckoutControllerIntegrationTest, StripePaymentElementControllerIntegrationTest,
		StripeRefundControllerIntegrationTest])
@IntegrationTest
@ManualTest
@JUnit4OnlyTest
class AllStripeOccSpockTests {

	@BeforeClass
	static void setUpClass() {
		startResourceServerMock()
		TestSetupUtils.startServer()
		TestSetupUtils.loadData()
	}

	@AfterClass
	static void tearDown() {
		try {
			TestSetupUtils.stopServer()
			TestSetupUtils.cleanData()
		}
		finally {
			stopResourceServerMock()
		}
	}

	@Test
	static void testing() {
		// dummy test required by the suite runner
	}

	private static void startResourceServerMock() {
		ResourceServerTestManager resourceServerTestManager = Registry.applicationContext
				.getBean("resourceServerTestManager", ResourceServerTestManager)
		resourceServerTestManager.start()
	}

	private static void stopResourceServerMock() {
		ResourceServerTestManager resourceServerTestManager = Registry.applicationContext
				.getBean("resourceServerTestManager", ResourceServerTestManager)
		resourceServerTestManager.stop()
	}
}
