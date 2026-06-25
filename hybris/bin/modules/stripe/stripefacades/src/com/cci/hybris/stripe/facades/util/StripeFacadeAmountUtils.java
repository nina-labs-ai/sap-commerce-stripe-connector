
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.util;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.math.BigDecimal;

/**
 * Deterministic amount calculation helpers used by Stripe facades.
 */
public final class StripeFacadeAmountUtils {

    private StripeFacadeAmountUtils() {
        // utility class
    }

    /**
     * Resolves the order amount shown to the storefront.
     *
     * @param order source order
     * @return order amount in major units
     */
    public static double resolveDisplayAmount(final AbstractOrderModel order) {
        if (order == null) {
            return 0D;
        }
        final double totalPrice = order.getTotalPrice() == null ? 0D : order.getTotalPrice();
        final double totalTax = order.getTotalTax() == null ? 0D : order.getTotalTax();
        return Boolean.TRUE.equals(order.getNet()) ? (totalPrice + totalTax) : totalPrice;
    }

    /**
     * Converts an amount from minor units to major units.
     *
     * @param amountMinor amount in minor units
     * @param currencyDigits number of decimal digits for the currency
     * @return amount in major units
     */
    public static double toMajorUnits(final long amountMinor, final int currencyDigits) {
        return BigDecimal.valueOf(amountMinor).movePointLeft(currencyDigits).doubleValue();
    }
}
