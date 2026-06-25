/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.service.impl;

import com.cci.hybris.stripe.fulfilmentprocess.constants.StripefulfilmentprocessConstants;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.payment.dto.TransactionStatus;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

@UnitTest
public class DefaultStripeCheckoutSessionPaymentStatusServiceTest
{
	private DefaultStripeCheckoutSessionPaymentStatusService testObj;
	private OrderModel order;

	@Before
	public void setUp()
	{
		testObj = new DefaultStripeCheckoutSessionPaymentStatusService();
		order = new OrderModel();
		order.setPaymentInfo(new PaymentInfoModel());
		order.setPaymentTransactions(new ArrayList<>());
	}

	@Test
	public void resolvePaymentTransition_nullOrder_returnsNok()
	{
		final String result = testObj.resolvePaymentTransition(null);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_NOK, result);
	}

	@Test
	public void resolvePaymentTransition_paymentCapturedStatus_returnsOk()
	{
		order.setStatus(OrderStatus.PAYMENT_CAPTURED);
		final String result = testObj.resolvePaymentTransition(order);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_OK, result);
	}

	@Test
	public void resolvePaymentTransition_acceptedCaptureEntry_returnsOk()
	{
		order.setPaymentTransactions(Collections.singletonList(createTransaction(PaymentTransactionType.CAPTURE,
				TransactionStatus.ACCEPTED.name())));
		final String result = testObj.resolvePaymentTransition(order);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_OK, result);
	}

	@Test
	public void resolvePaymentTransition_waitingAuthorizationEntry_returnsWait()
	{
		order.setPaymentTransactions(Collections.singletonList(createTransaction(PaymentTransactionType.AUTHORIZATION,
				TransactionStatus.WAITING.name())));
		final String result = testObj.resolvePaymentTransition(order);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_WAIT, result);
	}

	@Test
	public void resolvePaymentTransition_rejectedCaptureEntry_returnsNok()
	{
		order.setPaymentTransactions(Collections.singletonList(createTransaction(PaymentTransactionType.CAPTURE,
				TransactionStatus.REJECTED.name())));
		final String result = testObj.resolvePaymentTransition(order);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_NOK, result);
	}

	@Test
	public void resolvePaymentTransition_noEntriesButPaymentInfoPresent_returnsWait()
	{
		final String result = testObj.resolvePaymentTransition(order);
		assertEquals(StripefulfilmentprocessConstants.TRANSITION_WAIT, result);
	}

	protected PaymentTransactionModel createTransaction(final PaymentTransactionType type, final String transactionStatus)
	{
		final PaymentTransactionEntryModel paymentTransactionEntry = new PaymentTransactionEntryModel();
		paymentTransactionEntry.setType(type);
		paymentTransactionEntry.setTransactionStatus(transactionStatus);

		final PaymentTransactionModel paymentTransaction = new PaymentTransactionModel();
		paymentTransaction.setEntries(Collections.singletonList(paymentTransactionEntry));
		return paymentTransaction;
	}
}
