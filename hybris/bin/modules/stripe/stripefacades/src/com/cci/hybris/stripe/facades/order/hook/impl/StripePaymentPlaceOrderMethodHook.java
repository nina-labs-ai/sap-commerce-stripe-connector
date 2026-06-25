
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.order.hook.impl;

import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;

import de.hybris.platform.commerceservices.order.hook.CommercePlaceOrderMethodHook;
import de.hybris.platform.commerceservices.service.data.CommerceCheckoutParameter;
import de.hybris.platform.commerceservices.service.data.CommerceOrderResult;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.InvalidCartException;

/**
 * Synchronizes Stripe checkout payment state from the source cart onto the placed order.
 */
public class StripePaymentPlaceOrderMethodHook implements CommercePlaceOrderMethodHook {

    private final StripePaymentTransactionService stripePaymentTransactionService;

    /**
     * Creates the hook with the payment transaction service used to copy Stripe state to the order.
     *
     * @param stripePaymentTransactionService payment transaction service
     */
    public StripePaymentPlaceOrderMethodHook(final StripePaymentTransactionService stripePaymentTransactionService) {
        this.stripePaymentTransactionService = stripePaymentTransactionService;
    }

    @Override
    public void afterPlaceOrder(final CommerceCheckoutParameter parameter, final CommerceOrderResult result)
            throws InvalidCartException {
        if (parameter == null || parameter.getCart() == null || result == null || result.getOrder() == null) {
            return;
        }
        if (result.getOrder() instanceof OrderModel orderModel) {
            stripePaymentTransactionService.synchronizeStripePaymentsToOrder(parameter.getCart(), orderModel);
        }
    }

    @Override
    public void beforePlaceOrder(final CommerceCheckoutParameter parameter) throws InvalidCartException {
        // No-op.
    }

    @Override
    public void beforeSubmitOrder(final CommerceCheckoutParameter parameter, final CommerceOrderResult result)
            throws InvalidCartException {
        // No-op.
    }
}
