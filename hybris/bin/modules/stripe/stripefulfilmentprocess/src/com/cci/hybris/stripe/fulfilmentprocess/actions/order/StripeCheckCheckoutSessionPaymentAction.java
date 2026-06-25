
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.actions.order;

import com.cci.hybris.stripe.fulfilmentprocess.constants.StripefulfilmentprocessConstants;
import com.cci.hybris.stripe.fulfilmentprocess.service.StripeCheckoutSessionPaymentStatusService;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.processengine.action.AbstractAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * Checks whether a Stripe Checkout Session payment is complete enough for fulfilment to continue.
 */
public class StripeCheckCheckoutSessionPaymentAction extends AbstractAction<OrderProcessModel>
{
	private static final Logger LOG = LogManager.getLogger(StripeCheckCheckoutSessionPaymentAction.class);

	private final StripeCheckoutSessionPaymentStatusService stripeCheckoutSessionPaymentStatusService;

	/**
	 * Creates the action with its payment-status dependency.
	 *
	 * @param stripeCheckoutSessionPaymentStatusService
	 *           service used to resolve payment transitions
	 */
	public StripeCheckCheckoutSessionPaymentAction(
			final StripeCheckoutSessionPaymentStatusService stripeCheckoutSessionPaymentStatusService)
	{
		this.stripeCheckoutSessionPaymentStatusService = stripeCheckoutSessionPaymentStatusService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getTransitions()
	{
		return AbstractAction.createTransitions(
				StripefulfilmentprocessConstants.TRANSITION_OK,
				StripefulfilmentprocessConstants.TRANSITION_NOK,
				StripefulfilmentprocessConstants.TRANSITION_WAIT);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String execute(final OrderProcessModel process)
	{
		if (process == null)
		{
			LOG.error("Stripe order process was null.");
			return StripefulfilmentprocessConstants.TRANSITION_NOK;
		}

		final OrderModel order = process.getOrder();
		if (order == null)
		{
			LOG.error("Order not found for Stripe order process [{}].", process.getCode());
			return StripefulfilmentprocessConstants.TRANSITION_NOK;
		}

		return getStripeCheckoutSessionPaymentStatusService().resolvePaymentTransition(order);
	}

	/**
	 * Exposes payment-status service dependency for extensible subclasses.
	 *
	 * @return payment-status resolver service
	 */
	protected StripeCheckoutSessionPaymentStatusService getStripeCheckoutSessionPaymentStatusService()
	{
		return stripeCheckoutSessionPaymentStatusService;
	}
}
