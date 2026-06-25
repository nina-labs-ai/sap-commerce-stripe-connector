
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.facades.refund.impl;

import com.cci.hybris.stripe.facades.constants.StripeFacadesConstants;
import com.cci.hybris.stripe.facades.data.StripeRefundFacadeData;
import com.cci.hybris.stripe.facades.refund.StripeRefundFacade;
import com.cci.hybris.stripe.facades.util.StripeFacadeAmountUtils;
import com.cci.hybris.stripe.facades.util.StripeRefundFacadeUtils;
import com.cci.hybris.stripe.services.data.StripeRefundData;
import com.cci.hybris.stripe.services.service.StripePaymentLifecycleService;

import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commerceservices.customer.CustomerAccountService;
import de.hybris.platform.commerceservices.order.CommerceOrderService;
import de.hybris.platform.commerceservices.strategies.CheckoutCustomerStrategy;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.opffacades.calculators.CalculatorStrategy;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.servicelayer.user.UserService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.store.services.BaseStoreService;

import java.math.BigDecimal;

/**
 * Default facade for order-bound Stripe refunds.
 */
public class DefaultStripeRefundFacade implements StripeRefundFacade {

    private static final String ORDER_NOT_FOUND_FOR_USER_AND_BASE_STORE =
            "Order with guid %s not found for current user in current BaseStore";

    private final BaseStoreService baseStoreService;
    private final UserService userService;
    private final CheckoutCustomerStrategy checkoutCustomerStrategy;
    private final CustomerAccountService customerAccountService;
    private final CommerceOrderService commerceOrderService;
    private final StripePaymentLifecycleService stripePaymentLifecycleService;
    private final CalculatorStrategy calculatorStrategy;

    /**
     * Creates the facade with the services required to resolve customer orders and create Stripe refunds.
     *
     * @param baseStoreService base-store service
     * @param userService user service
     * @param checkoutCustomerStrategy checkout-customer strategy
     * @param customerAccountService customer account service
     * @param commerceOrderService commerce order service
     * @param stripePaymentLifecycleService Stripe lifecycle service
     * @param calculatorStrategy price formatter
     */
    public DefaultStripeRefundFacade(final BaseStoreService baseStoreService,
                                     final UserService userService,
                                     final CheckoutCustomerStrategy checkoutCustomerStrategy,
                                     final CustomerAccountService customerAccountService,
                                     final CommerceOrderService commerceOrderService,
                                     final StripePaymentLifecycleService stripePaymentLifecycleService,
                                     final CalculatorStrategy calculatorStrategy) {
        this.baseStoreService = baseStoreService;
        this.userService = userService;
        this.checkoutCustomerStrategy = checkoutCustomerStrategy;
        this.customerAccountService = customerAccountService;
        this.commerceOrderService = commerceOrderService;
        this.stripePaymentLifecycleService = stripePaymentLifecycleService;
        this.calculatorStrategy = calculatorStrategy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StripeRefundFacadeData createRefundForOrder(final String orderCode,
                                                       final String paymentReference,
                                                       final BigDecimal amount) {
        final OrderModel order = resolveOrder(orderCode);
        final StripeRefundData refundData = getStripePaymentLifecycleService().createRefund(order, paymentReference, amount);
        return convert(order, paymentReference, refundData);
    }

    /**
     * Converts Stripe refund service data into facade data.
     *
     * @param order refunded order
     * @param paymentReference refunded Stripe reference
     * @param source Stripe refund service data
     * @return refund facade data
     */
    protected StripeRefundFacadeData convert(final OrderModel order,
                                             final String paymentReference,
                                             final StripeRefundData source) {
        return StripeRefundFacadeUtils.toFacadeData(source, order.getCode(), paymentReference,
                formatAmount(order, source.getAmount()));
    }

    /**
     * Formats the refund amount for storefront presentation.
     *
     * @param order refunded order
     * @param amountMinor refund amount in minor units
     * @return formatted refund amount
     */
    protected String formatAmount(final OrderModel order, final Long amountMinor) {
        if (order == null || order.getCurrency() == null || amountMinor == null) {
            return null;
        }

        final int digits = order.getCurrency().getDigits() == null
                ? StripeFacadesConstants.DEFAULT_CURRENCY_DIGITS
                : order.getCurrency().getDigits();
        final double majorAmount = StripeFacadeAmountUtils.toMajorUnits(amountMinor, digits);
        final PriceData priceData = getCalculatorStrategy().createPrice(order, majorAmount);
        return priceData == null ? null : priceData.getFormattedValue();
    }

    /**
     * Resolves an order that belongs to the current user in the current base store.
     *
     * @param orderCode order code or guid
     * @return resolved order
     */
    protected OrderModel resolveOrder(final String orderCode) {
        final BaseStoreModel currentBaseStore = getBaseStoreService().getCurrentBaseStore();
        final OrderModel orderModel;
        if (getCheckoutCustomerStrategy().isAnonymousCheckout()) {
            orderModel = getCustomerAccountService().getOrderDetailsForGUID(orderCode, currentBaseStore);
        } else {
            final UserModel currentUser = getUserService().getCurrentUser();
            if (!(currentUser instanceof CustomerModel customer)) {
                throw new UnknownIdentifierException(String.format(ORDER_NOT_FOUND_FOR_USER_AND_BASE_STORE, orderCode));
            }
            try {
                orderModel = getCommerceOrderService().getOrderForPotentialId(customer,
                        orderCode, currentBaseStore);
            } catch (final ModelNotFoundException ignored) {
                throw new UnknownIdentifierException(String.format(ORDER_NOT_FOUND_FOR_USER_AND_BASE_STORE, orderCode));
            }
        }

        if (orderModel == null) {
            throw new UnknownIdentifierException(String.format(ORDER_NOT_FOUND_FOR_USER_AND_BASE_STORE, orderCode));
        }

        return orderModel;
    }

    protected BaseStoreService getBaseStoreService() {
        return baseStoreService;
    }

    protected UserService getUserService() {
        return userService;
    }

    protected CheckoutCustomerStrategy getCheckoutCustomerStrategy() {
        return checkoutCustomerStrategy;
    }

    protected CustomerAccountService getCustomerAccountService() {
        return customerAccountService;
    }

    protected CommerceOrderService getCommerceOrderService() {
        return commerceOrderService;
    }

    protected StripePaymentLifecycleService getStripePaymentLifecycleService() {
        return stripePaymentLifecycleService;
    }

    protected CalculatorStrategy getCalculatorStrategy() {
        return calculatorStrategy;
    }
}
