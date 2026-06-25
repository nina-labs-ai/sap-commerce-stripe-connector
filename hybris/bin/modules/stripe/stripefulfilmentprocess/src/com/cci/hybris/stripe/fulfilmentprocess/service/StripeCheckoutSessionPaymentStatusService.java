
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.service;

import de.hybris.platform.core.model.order.OrderModel;

/**
 * Resolves the current Stripe Checkout Session payment state for an order into
 * process transitions that can be used by business process actions.
 */
public interface StripeCheckoutSessionPaymentStatusService
{
	/**
	 * Evaluates the order payment state and returns a process transition.
	 *
	 * @param order
	 *           the order being checked
	 * @return {@code OK}, {@code WAIT}, or {@code NOK}
	 */
	String resolvePaymentTransition(OrderModel order);
}
