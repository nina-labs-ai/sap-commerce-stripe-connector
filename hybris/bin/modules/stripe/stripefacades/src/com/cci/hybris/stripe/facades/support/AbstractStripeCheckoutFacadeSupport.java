
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.support;

import com.cci.hybris.stripe.facades.constants.StripeFacadesConstants;
import com.cci.hybris.stripe.services.exception.StripeIntegrationException;

import de.hybris.platform.basecommerce.model.site.BaseSiteModel;
import de.hybris.platform.commercefacades.order.CheckoutFacade;
import de.hybris.platform.commercefacades.order.OrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.site.BaseSiteService;

/**
 * Common support for Stripe cart-based facades that share checkout context resolution.
 */
public abstract class AbstractStripeCheckoutFacadeSupport {

    private CartService cartService;
    private BaseSiteService baseSiteService;
    private CheckoutFacade checkoutFacade;
    private OrderFacade orderFacade;

    public void setCartService(final CartService cartService) {
        this.cartService = cartService;
    }

    public void setBaseSiteService(final BaseSiteService baseSiteService) {
        this.baseSiteService = baseSiteService;
    }

    public void setCheckoutFacade(final CheckoutFacade checkoutFacade) {
        this.checkoutFacade = checkoutFacade;
    }

    public void setOrderFacade(final OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }
    protected CartModel getSessionCart() {
        if (!getCartService().hasSessionCart()) {
            throw new StripeIntegrationException(getNoSessionCartMessage());
        }

        return getCartService().getSessionCart();
    }
    protected CartModel getSessionCartIfAvailable() {
        return getCartService().hasSessionCart() ? getCartService().getSessionCart() : null;
    }
    protected String getCurrentSiteId() {
        final BaseSiteModel currentBaseSite = getBaseSiteService().getCurrentBaseSite();
        if (currentBaseSite == null) {
            throw new StripeIntegrationException(getNoCurrentBaseSiteMessage());
        }
        return currentBaseSite.getUid();
    }
    protected String getSessionCartCode() {
        return getCartService().hasSessionCart() ? getCartService().getSessionCart().getCode() : null;
    }

    /**
     * Places an order from the resolved Stripe cart by restoring it into the current session checkout context first.
     *
     * @param cart Stripe-owned checkout cart
     * @return placed order data
     * @throws InvalidCartException when the checkout facade rejects the cart
     */
    protected OrderData placeOrder(final CartModel cart) throws InvalidCartException {
        getCartService().setSessionCart(cart);
        final OrderData orderData = getCheckoutFacade().placeOrder();
        if (orderData == null) {
            throw new StripeIntegrationException(StripeFacadesConstants.MESSAGE_NO_ORDER_FOR_PAID_STRIPE_CART);
        }
        return orderData;
    }
    protected OrderData getOrderData(final OrderModel orderModel) {
        return getOrderFacade().getOrderDetailsForCodeWithoutUser(orderModel.getCode());
    }
    protected abstract String getNoSessionCartMessage();
    protected abstract String getNoCurrentBaseSiteMessage();

    protected CartService getCartService() {
        return cartService;
    }

    protected BaseSiteService getBaseSiteService() {
        return baseSiteService;
    }

    protected CheckoutFacade getCheckoutFacade() {
        return checkoutFacade;
    }

    protected OrderFacade getOrderFacade() {
        return orderFacade;
    }
}
