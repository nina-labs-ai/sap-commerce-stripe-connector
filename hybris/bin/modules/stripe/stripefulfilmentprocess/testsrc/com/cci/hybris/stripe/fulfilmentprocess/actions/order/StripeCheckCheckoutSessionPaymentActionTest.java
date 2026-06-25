/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.actions.order;

import com.cci.hybris.stripe.fulfilmentprocess.constants.StripefulfilmentprocessConstants;
import com.cci.hybris.stripe.fulfilmentprocess.service.StripeCheckoutSessionPaymentStatusService;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class StripeCheckCheckoutSessionPaymentActionTest
{
	@InjectMocks
	private StripeCheckCheckoutSessionPaymentAction testObj;

	@Mock
	private StripeCheckoutSessionPaymentStatusService stripeCheckoutSessionPaymentStatusService;
	@Mock
	private OrderProcessModel process;
	@Mock
	private OrderModel order;
	@Mock
	private ModelService modelService;

	@Before
	public void setUp()
	{
		testObj.setModelService(modelService);
		when(process.getOrder()).thenReturn(order);
	}

	@Test
	public void getTransitions_containsOkNokAndWait()
	{
		final Set<String> transitions = testObj.getTransitions();
		assertTrue(transitions.contains(StripefulfilmentprocessConstants.TRANSITION_OK));
		assertTrue(transitions.contains(StripefulfilmentprocessConstants.TRANSITION_NOK));
		assertTrue(transitions.contains(StripefulfilmentprocessConstants.TRANSITION_WAIT));
	}

	@Test
	public void execute_nullProcess_returnsNok()
	{
		final String result = testObj.execute(null);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_NOK, result);
		verifyNoInteractions(stripeCheckoutSessionPaymentStatusService);
	}

	@Test
	public void execute_missingOrder_returnsNok()
	{
		when(process.getOrder()).thenReturn(null);
		when(process.getCode()).thenReturn("process-code");
		final String result = testObj.execute(process);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_NOK, result);
		verifyNoInteractions(stripeCheckoutSessionPaymentStatusService);
	}

	@Test
	public void execute_serviceReturnsOk_returnsOk()
	{
		when(stripeCheckoutSessionPaymentStatusService.resolvePaymentTransition(order))
				.thenReturn(StripefulfilmentprocessConstants.TRANSITION_OK);
		final String result = testObj.execute(process);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_OK, result);
		verify(stripeCheckoutSessionPaymentStatusService).resolvePaymentTransition(order);
	}

	@Test
	public void execute_serviceReturnsWait_returnsWait()
	{
		when(stripeCheckoutSessionPaymentStatusService.resolvePaymentTransition(order))
				.thenReturn(StripefulfilmentprocessConstants.TRANSITION_WAIT);
		final String result = testObj.execute(process);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_WAIT, result);
		verify(stripeCheckoutSessionPaymentStatusService).resolvePaymentTransition(order);
	}

	@Test
	public void execute_serviceReturnsNok_returnsNok()
	{
		when(stripeCheckoutSessionPaymentStatusService.resolvePaymentTransition(order))
				.thenReturn(StripefulfilmentprocessConstants.TRANSITION_NOK);
		final String result = testObj.execute(process);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_NOK, result);
		verify(stripeCheckoutSessionPaymentStatusService).resolvePaymentTransition(order);
	}

	@Test
	public void getStripeCheckoutSessionPaymentStatusService_returnsInjectedDependency()
	{
		final ExposedStripeCheckCheckoutSessionPaymentAction action =
				new ExposedStripeCheckCheckoutSessionPaymentAction(stripeCheckoutSessionPaymentStatusService);
		assertSame(stripeCheckoutSessionPaymentStatusService, action.exposedStripeCheckoutSessionPaymentStatusService());
	}

	private static class ExposedStripeCheckCheckoutSessionPaymentAction extends StripeCheckCheckoutSessionPaymentAction
	{
		private ExposedStripeCheckCheckoutSessionPaymentAction(
				final StripeCheckoutSessionPaymentStatusService stripeCheckoutSessionPaymentStatusService)
		{
			super(stripeCheckoutSessionPaymentStatusService);
		}

		private StripeCheckoutSessionPaymentStatusService exposedStripeCheckoutSessionPaymentStatusService()
		{
			return getStripeCheckoutSessionPaymentStatusService();
		}
	}
}
