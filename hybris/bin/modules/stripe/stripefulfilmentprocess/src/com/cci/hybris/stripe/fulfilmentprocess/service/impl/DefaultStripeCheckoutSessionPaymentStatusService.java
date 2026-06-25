
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.service.impl;

import com.cci.hybris.stripe.fulfilmentprocess.constants.StripefulfilmentprocessConstants;
import com.cci.hybris.stripe.fulfilmentprocess.service.StripeCheckoutSessionPaymentStatusService;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.dto.TransactionStatus;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Default Stripe Checkout Session payment-state resolver used by fulfilment-process actions.
 */
public class DefaultStripeCheckoutSessionPaymentStatusService implements StripeCheckoutSessionPaymentStatusService
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String resolvePaymentTransition(final OrderModel order)
	{
		final String transition;
		if (order == null || order.getPaymentInfo() == null)
		{
			transition = StripefulfilmentprocessConstants.TRANSITION_NOK;
		}
		else if (isPaidOrderStatus(order) || hasAcceptedPaymentEntry(order))
		{
			transition = StripefulfilmentprocessConstants.TRANSITION_OK;
		}
		else if (hasFailedPaymentEntry(order) || isPaymentFailureStatus(order))
		{
			transition = StripefulfilmentprocessConstants.TRANSITION_NOK;
		}
		else
		{
			transition = StripefulfilmentprocessConstants.TRANSITION_WAIT;
		}
		return transition;
	}
	protected boolean isPaidOrderStatus(final OrderModel order)
	{
		final OrderStatus status = order.getStatus();
		return OrderStatus.PAYMENT_CAPTURED.equals(status)
				|| OrderStatus.PAYMENT_AUTHORIZED.equals(status)
				|| OrderStatus.COMPLETED.equals(status);
	}
	protected boolean isPaymentFailureStatus(final OrderModel order)
	{
		return OrderStatus.PAYMENT_NOT_CAPTURED.equals(order.getStatus());
	}

	/**
	 * Returns whether a successful payment entry is already present.
	 *
	 * @param order
	 *           the order to inspect
	 * @return {@code true} when a capture or authorization succeeded
	 */
	protected boolean hasAcceptedPaymentEntry(final OrderModel order)
	{
		return getPaymentEntries(order)
				.anyMatch(this::isAcceptedPaymentEntry);
	}

	/**
	 * Returns whether a failed payment entry is already present.
	 *
	 * @param order
	 *           the order to inspect
	 * @return {@code true} when a relevant entry failed
	 */
	protected boolean hasFailedPaymentEntry(final OrderModel order)
	{
		return getPaymentEntries(order)
				.anyMatch(this::isFailedPaymentEntry);
	}
	protected boolean isAcceptedPaymentEntry(final PaymentTransactionEntryModel paymentTransactionEntry)
	{
		return isRelevantPaymentType(paymentTransactionEntry)
				&& TransactionStatus.ACCEPTED.name().equalsIgnoreCase(paymentTransactionEntry.getTransactionStatus());
	}
	protected boolean isFailedPaymentEntry(final PaymentTransactionEntryModel paymentTransactionEntry)
	{
		if (!isRelevantPaymentType(paymentTransactionEntry))
		{
			return false;
		}

		final String transactionStatus = paymentTransactionEntry.getTransactionStatus();
		if (transactionStatus == null)
		{
			return false;
		}

		final String normalizedStatus = transactionStatus.toUpperCase(Locale.ROOT);
		return !isNonFailureStatus(normalizedStatus);
	}
	protected boolean isNonFailureStatus(final String normalizedStatus)
	{
		return TransactionStatus.ACCEPTED.name().equals(normalizedStatus)
				|| TransactionStatus.WAITING.name().equals(normalizedStatus)
				|| TransactionStatus.REQUESTED.name().equals(normalizedStatus)
				|| TransactionStatus.REVIEW.name().equals(normalizedStatus);
	}
	protected boolean isRelevantPaymentType(final PaymentTransactionEntryModel paymentTransactionEntry)
	{
		return paymentTransactionEntry.getType() == PaymentTransactionType.CAPTURE
				|| paymentTransactionEntry.getType() == PaymentTransactionType.AUTHORIZATION;
	}
	protected Collection<PaymentTransactionModel> getPaymentTransactions(final OrderModel order)
	{
		return order.getPaymentTransactions() == null ? Collections.emptyList() : order.getPaymentTransactions();
	}
	protected Stream<PaymentTransactionEntryModel> getPaymentEntries(final OrderModel order)
	{
		return getPaymentTransactions(order).stream()
				.filter(Objects::nonNull)
				.map(PaymentTransactionModel::getEntries)
				.filter(Objects::nonNull)
				.flatMap(Collection::stream)
				.filter(Objects::nonNull);
	}
}
