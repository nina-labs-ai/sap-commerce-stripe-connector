/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.backoffice.constants;

import java.util.List;

/**
 * Constants used by the Stripe backoffice extension.
 */
public final class StripebackofficeConstants extends GeneratedStripebackofficeConstants
{
	public static final String EXTENSIONNAME = "stripebackoffice";
	public static final String STRIPE_SECRET_KEY = "stripe.secret.key";
	public static final String STRIPE_PUBLISHABLE_KEY = "stripe.publishable.key";
	public static final String STRIPE_WEBHOOK_SECRET = "stripe.webhook.secret";
	public static final String STRIPE_CHECKOUT_SUCCESS_URL = "stripe.checkout.success.url";
	public static final String STRIPE_CHECKOUT_CANCEL_URL = "stripe.checkout.cancel.url";
	public static final String STRIPE_ELEMENTS_RETURN_URL = "stripe.elements.return.url";
	public static final List<String> VISIBLE_CONFIGURATION_KEYS = List.of(
			STRIPE_SECRET_KEY,
			STRIPE_PUBLISHABLE_KEY,
			STRIPE_WEBHOOK_SECRET,
			STRIPE_CHECKOUT_SUCCESS_URL,
			STRIPE_CHECKOUT_CANCEL_URL,
			STRIPE_ELEMENTS_RETURN_URL);

	private StripebackofficeConstants()
	{
		// utility class
	}
}
