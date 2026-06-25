package com.cci.hybris.stripe.backoffice.service.impl;

import com.cci.hybris.stripe.backoffice.constants.StripebackofficeConstants;
import com.cci.hybris.stripe.backoffice.data.StripeBackofficeConfigurationStatus;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.config.ConfigurationService;

import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStripeBackofficeConfigurationServiceTest
{
	@Mock
	private ConfigurationService configurationService;

	@Mock
	private Configuration configuration;

	@Test
	public void getConfigurationStatuses_mixedValues_returnsMaskedStatuses()
	{
		when(configurationService.getConfiguration()).thenReturn(configuration);
		when(configuration.getString("electronics." + StripebackofficeConstants.STRIPE_SECRET_KEY, ""))
				.thenReturn("insert your secret key");
		when(configuration.getString(StripebackofficeConstants.STRIPE_SECRET_KEY, "")).thenReturn("secret-key-123456");
		when(configuration.getString(StripebackofficeConstants.STRIPE_PUBLISHABLE_KEY, "")).thenReturn("publishable-key-123456");
		when(configuration.getString(StripebackofficeConstants.STRIPE_WEBHOOK_SECRET, "")).thenReturn("webhook-secret-123456");
		when(configuration.getString(StripebackofficeConstants.STRIPE_CHECKOUT_SUCCESS_URL, ""))
				.thenReturn("https://shop.example/success");
		when(configuration.getString(StripebackofficeConstants.STRIPE_CHECKOUT_CANCEL_URL, "")).thenReturn(" ");
		when(configuration.getString(StripebackofficeConstants.STRIPE_ELEMENTS_RETURN_URL, ""))
				.thenReturn("https://shop.example/elements/return");

		final DefaultStripeBackofficeConfigurationService service =
				new DefaultStripeBackofficeConfigurationService(configurationService);

		final List<StripeBackofficeConfigurationStatus> statuses = service.getConfigurationStatuses("electronics");

		assertEquals(6, statuses.size());
		assertEquals("****3456", statuses.get(0).getDisplayValue());
		assertEquals(StripebackofficeConstants.STRIPE_SECRET_KEY, statuses.get(0).getEffectiveKey());
		assertTrue(statuses.get(0).getDetailMessage().contains("Falling back to global"));
		assertEquals("publ...3456", statuses.get(1).getDisplayValue());
		assertEquals("****3456", statuses.get(2).getDisplayValue());
		assertTrue(statuses.get(3).isConfigured());
		assertEquals("http...cess", statuses.get(3).getDisplayValue());
		assertFalse(statuses.get(4).isConfigured());
		assertEquals("", statuses.get(4).getDisplayValue());
		assertEquals("http...turn", statuses.get(5).getDisplayValue());
		assertEquals(StripebackofficeConstants.STRIPE_ELEMENTS_RETURN_URL, statuses.get(5).getEffectiveKey());
	}

	@Test
	public void getConfigurationStatuses_siteOverrideConfigured_reportsSiteSpecificKey()
	{
		when(configurationService.getConfiguration()).thenReturn(configuration);
		when(configuration.getString("electronics." + StripebackofficeConstants.STRIPE_PUBLISHABLE_KEY, ""))
				.thenReturn("site-publishable-key");

		final DefaultStripeBackofficeConfigurationService service =
				new DefaultStripeBackofficeConfigurationService(configurationService);

		final List<StripeBackofficeConfigurationStatus> statuses = service.getConfigurationStatuses("electronics");
		final StripeBackofficeConfigurationStatus publishableKeyStatus = statuses.get(1);

		assertEquals("electronics." + StripebackofficeConstants.STRIPE_PUBLISHABLE_KEY,
				publishableKeyStatus.getEffectiveKey());
		assertTrue(publishableKeyStatus.getDetailMessage().contains("Using site override"));
	}

	@Test
	public void getConfigurationService_returnsInjectedDependency()
	{
		final ExposedDefaultStripeBackofficeConfigurationService service =
				new ExposedDefaultStripeBackofficeConfigurationService(configurationService);
		assertSame(configurationService, service.exposedConfigurationService());
	}

	private static class ExposedDefaultStripeBackofficeConfigurationService extends DefaultStripeBackofficeConfigurationService
	{
		private ExposedDefaultStripeBackofficeConfigurationService(final ConfigurationService configurationService)
		{
			super(configurationService);
		}

		private ConfigurationService exposedConfigurationService()
		{
			return getConfigurationService();
		}
	}
}
