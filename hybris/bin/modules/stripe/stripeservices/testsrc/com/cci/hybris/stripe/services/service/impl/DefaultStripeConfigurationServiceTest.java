package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.servicelayer.config.ConfigurationService;

import org.apache.commons.configuration2.BaseConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStripeConfigurationServiceTest {

    @InjectMocks
    private DefaultStripeConfigurationService configurationService;

    @Mock
    private ConfigurationService hybrisConfigurationService;

    @Test
    public void getSuccessUrl_withoutPlaceholder_appendsSessionPlaceholder() {
        // Arrange
        final Configuration configuration = new BaseConfiguration();
        configuration.addProperty("stripe.checkout.success.url", "https://example.com/success");
        when(hybrisConfigurationService.getConfiguration()).thenReturn(configuration);

        // Act
        final String result = configurationService.getSuccessUrl(null);

        // Assert
        assertEquals("https://example.com/success?session_id={CHECKOUT_SESSION_ID}", result);
    }

    @Test
    public void getElementsReturnUrl_withoutDedicatedProperty_fallsBackToSuccessUrlWithoutPlaceholder() {
        // Arrange
        final Configuration configuration = new BaseConfiguration();
        configuration.addProperty("stripe.checkout.success.url", "https://example.com/success");
        configuration.addProperty("stripe.cancel.url", "https://example.com/cancel");
        when(hybrisConfigurationService.getConfiguration()).thenReturn(configuration);

        // Act
        final String result = configurationService.getElementsReturnUrl(null);

        // Assert
        assertEquals("https://example.com/success", result);
    }

    @Test(expected = StripeIntegrationException.class)
    public void getSecretKey_missingProperty_throwsException() {
        // Arrange
        when(hybrisConfigurationService.getConfiguration()).thenReturn(new BaseConfiguration());

        // Act
        configurationService.getSecretKey("electronics");
    }
}
