
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.util;

import com.cci.hybris.stripe.facades.data.StripeRefundFacadeData;
import com.cci.hybris.stripe.services.data.StripeRefundData;

/**
 * Utility methods used by Stripe refund facades.
 */
public final class StripeRefundFacadeUtils {

    private StripeRefundFacadeUtils() {
        // utility class
    }

    /**
     * Converts service-level refund data into facade data.
     *
     * @param source service data
     * @param orderCode SAP Commerce order code
     * @param paymentReference Stripe payment reference
     * @param formattedAmount formatted storefront amount
     * @return facade data
     */
    public static StripeRefundFacadeData toFacadeData(final StripeRefundData source,
                                                      final String orderCode,
                                                      final String paymentReference,
                                                      final String formattedAmount) {
        final StripeRefundFacadeData data = new StripeRefundFacadeData();
        data.setId(source.getId());
        data.setPaymentIntentId(source.getPaymentIntentId());
        data.setStatus(source.getStatus());
        data.setAmount(source.getAmount());
        data.setCurrency(source.getCurrency());
        data.setOrderCode(orderCode);
        data.setPaymentReference(paymentReference);
        data.setFormattedAmount(formattedAmount);
        return data;
    }
}
