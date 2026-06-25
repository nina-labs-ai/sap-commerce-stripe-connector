
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.util;

import de.hybris.platform.core.model.order.AbstractOrderModel;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Amount utilities for Stripe amount conversions and order totals.
 */
public final class StripeAmountUtils {

    private StripeAmountUtils() {
    }

    /**
     * Converts a major-unit amount into Stripe minor units.
     *
     * @param amount amount in major units
     * @param digits currency digits
     * @return amount in minor units
     */
    public static long toMinorUnits(final BigDecimal amount, final int digits) {
        return amount.movePointRight(digits).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /**
     * Calculates the order total expected by Stripe.
     *
     * @param order order to total
     * @return order total in major units
     */
    public static BigDecimal calculateOrderTotal(final AbstractOrderModel order) {
        final double totalPrice = order.getTotalPrice() == null ? 0D : order.getTotalPrice();
        final double totalTax = order.getTotalTax() == null ? 0D : order.getTotalTax();
        return Boolean.TRUE.equals(order.getNet()) ? BigDecimal.valueOf(totalPrice + totalTax) : BigDecimal.valueOf(totalPrice);
    }

    /**
     * Converts a Stripe minor-unit amount into a major-unit amount.
     *
     * @param amount amount in minor units
     * @param digits currency digits
     * @return amount in major units
     */
    public static BigDecimal toMajorAmount(final Long amount, final int digits) {
        return amount == null ? BigDecimal.ZERO : BigDecimal.valueOf(amount, digits);
    }
}
