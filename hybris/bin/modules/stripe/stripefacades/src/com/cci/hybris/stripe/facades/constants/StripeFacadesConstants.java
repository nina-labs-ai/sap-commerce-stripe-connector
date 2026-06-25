package com.cci.hybris.stripe.facades.constants;

/**
 * Compatibility constants for generated sources that still use the original extgen package.
 */
public final class StripeFacadesConstants {

    public static final String EXTENSIONNAME = "stripefacades";
    public static final String PLATFORM_LOGO_CODE = "hybrisPlatformLogo";
    public static final String STRIPE_CHECKOUT_PAYMENT_STATUS_PAID = "paid";
    public static final String STRIPE_CHECKOUT_STATUS_COMPLETE = "complete";
    public static final String STRIPE_PAYMENT_INTENT_STATUS_SUCCEEDED = "succeeded";
    public static final String STRIPE_PAYMENT_INTENT_STATUS_REQUIRES_CAPTURE = "requires_capture";
    public static final String MESSAGE_NO_SESSION_CART_FOR_STRIPE_CHECKOUT = "No session cart available for Stripe Checkout.";
    public static final String MESSAGE_NO_BASE_SITE_FOR_STRIPE_CHECKOUT =
            "No current base site is available for Stripe Checkout.";
    public static final String MESSAGE_NO_SESSION_CART_FOR_PAYMENT_ELEMENTS =
            "No session cart available for Stripe Payment Elements.";
    public static final String MESSAGE_NO_BASE_SITE_FOR_PAYMENT_ELEMENTS =
            "No current base site is available for Stripe Payment Elements.";
    public static final String MESSAGE_NO_ORDER_FOR_PAID_STRIPE_CART =
            "SAP Commerce did not return an order for the paid Stripe cart.";
    public static final int DEFAULT_CURRENCY_DIGITS = 2;

    private StripeFacadesConstants() {
    }
}
