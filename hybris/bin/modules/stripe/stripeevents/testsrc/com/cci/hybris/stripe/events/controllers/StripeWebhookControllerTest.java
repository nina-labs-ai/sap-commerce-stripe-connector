package com.cci.hybris.stripe.events.controllers;

import com.cci.hybris.stripe.services.service.StripeWebhookService;

import de.hybris.bootstrap.annotations.UnitTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.verify;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class StripeWebhookControllerTest {

    @InjectMocks
    private StripeWebhookController controller;

    @Mock
    private StripeWebhookService stripeWebhookService;

    @Test
    public void handleWebhook_validPayload_delegatesToService() {
        controller.handleWebhook("payload", "signature", "electronics");
        verify(stripeWebhookService).handleWebhook("payload", "signature", "electronics");
    }

    @Test
    public void handleWebhook_withoutRequestedSite_delegatesToService() {
        controller.handleWebhook("payload", "signature", null);
        verify(stripeWebhookService).handleWebhook("payload", "signature", null);
    }
}
