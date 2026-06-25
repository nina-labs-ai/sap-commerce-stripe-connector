package com.cci.hybris.stripe.occ.constants;

import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;

/**
 * Compatibility constants for generated sources that still use the original extgen package.
 */
public final class StripeOccConstants {

    public static final String EXTENSIONNAME = "stripeocc";
    public static final String ANONYMOUS_USER_ID = "anonymous";
    public static final String CURRENT_CART_ID = "current";
    public static final String DEFAULT_FIELD_SET = FieldSetLevelHelper.DEFAULT_LEVEL;
    public static final String CHECKOUT_ANONYMOUS_CART_REQUIRED_MESSAGE =
            "Anonymous Stripe Checkout requests require a cartId.";
    public static final String ELEMENTS_ANONYMOUS_CART_REQUIRED_MESSAGE =
            "Anonymous Stripe Payment Elements requests require a cartId.";
    public static final String REFUND_PAYMENT_REFERENCE_REQUIRED_MESSAGE =
            "Stripe refund paymentReference is required.";
    public static final String REFUND_AMOUNT_POSITIVE_MESSAGE =
            "Stripe refund amount must be positive when provided.";

    private StripeOccConstants() {
        // utility constants
    }
}
