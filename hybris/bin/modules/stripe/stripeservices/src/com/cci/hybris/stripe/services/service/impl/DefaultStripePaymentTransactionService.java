
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;
import com.cci.hybris.stripe.services.data.StripeCheckoutSessionData;
import com.cci.hybris.stripe.services.data.StripePaymentIntentData;
import com.cci.hybris.stripe.services.data.StripeRefundData;
import com.cci.hybris.stripe.services.service.StripePaymentTransactionService;
import com.cci.hybris.stripe.services.util.StripeAmountUtils;
import com.cci.hybris.stripe.services.util.StripePaymentReferenceUtils;
import com.cci.hybris.stripe.services.util.StripePaymentTransactionUtils;
import com.cci.hybris.stripe.services.util.StripeStatusUtils;

import de.hybris.platform.core.model.order.AbstractOrderModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.time.TimeService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.CollectionUtils;

/**
 * Default transaction persistence service for Stripe Checkout.
 */
public class DefaultStripePaymentTransactionService implements StripePaymentTransactionService {

    private static final String QUERY_PARAM_CODE = "code";
    private static final String QUERY_PARAM_REQUEST_ID = "requestId";

    private static final String ORDER_QUERY = "SELECT {pk} FROM {Order} WHERE {code}=?code";
    private static final String CART_QUERY = "SELECT {pk} FROM {Cart} WHERE {code}=?code";
    private static final String CART_GUID_QUERY = "SELECT {pk} FROM {Cart} WHERE {guid}=?code";
    private static final String ORDER_BY_REQUEST_ID_QUERY = "SELECT {o.pk} "
            + "FROM {Order AS o JOIN PaymentTransaction AS t ON {t.order}={o.pk} "
            + "JOIN PaymentTransactionEntry AS e ON {e.paymentTransaction}={t.pk}} "
            + "WHERE {e.requestId}=?requestId ORDER BY {e.time} DESC";
    private static final String CART_BY_REQUEST_ID_QUERY = "SELECT {c.pk} "
            + "FROM {Cart AS c JOIN PaymentTransaction AS t ON {t.order}={c.pk} "
            + "JOIN PaymentTransactionEntry AS e ON {e.paymentTransaction}={t.pk}} "
            + "WHERE {e.requestId}=?requestId ORDER BY {e.time} DESC";
    private static final String ENTRY_BY_REQUEST_ID_QUERY = "SELECT {pk} "
            + "FROM {PaymentTransactionEntry} WHERE {requestId}=?requestId";

    private final ModelService modelService;
    private final FlexibleSearchService flexibleSearchService;
    private final TimeService timeService;

    /**
     * Creates the service with the collaborators required for Stripe transaction persistence.
     *
     * @param modelService model service
     * @param flexibleSearchService flexible search service
     * @param timeService time service
     */
    public DefaultStripePaymentTransactionService(final ModelService modelService,
                                                  final FlexibleSearchService flexibleSearchService,
                                                  final TimeService timeService) {
        this.modelService = modelService;
        this.flexibleSearchService = flexibleSearchService;
        this.timeService = timeService;
    }

    /**
     * Registers a Checkout Session authorization entry on the order payment transaction.
     *
     * @param order order that owns the Checkout Session
     * @param sessionData Checkout Session data
     */
    @Override
    public void registerCheckoutSession(final AbstractOrderModel order, final StripeCheckoutSessionData sessionData) {
        final PaymentTransactionModel transaction = getOrCreateTransaction(order);
        if (StripePaymentTransactionUtils.findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId())
                .isEmpty()) {
            createEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId(),
                    StripeServicesConstants.STATUS_PENDING, StripeServicesConstants.STATUS_PENDING);
        }
    }

    /**
     * Marks the Checkout Session as completed and adds the corresponding capture entry when needed.
     *
     * @param order order that owns the Checkout Session
     * @param sessionData Checkout Session data
     */
    @Override
    public void markCheckoutSessionCompleted(final AbstractOrderModel order, final StripeCheckoutSessionData sessionData) {
        final PaymentTransactionModel transaction = getOrCreateTransaction(order);
        if (StripePaymentTransactionUtils.isCaptured(transaction, sessionData.getId())) {
            return;
        }

        final PaymentTransactionEntryModel authorization = StripePaymentTransactionUtils
                .findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId())
                .orElseGet(() -> createEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId(),
                        StripeServicesConstants.STATUS_ACCEPTED, StripeServicesConstants.STATUS_SUCCESSFUL));
        authorization.setTransactionStatus(StripeServicesConstants.STATUS_ACCEPTED);
        authorization.setTransactionStatusDetails(StripeServicesConstants.STATUS_SUCCESSFUL);
        getModelService().save(authorization);

        if (StripePaymentTransactionUtils.findEntry(transaction, PaymentTransactionType.CAPTURE, sessionData.getId())
                .isEmpty()) {
            createEntry(transaction, PaymentTransactionType.CAPTURE, sessionData.getId(),
                    StripeServicesConstants.STATUS_ACCEPTED, StripeServicesConstants.STATUS_SUCCESSFUL);
        }

        if (order instanceof OrderModel orderModel) {
            orderModel.setStatus(OrderStatus.PAYMENT_CAPTURED);
            getModelService().save(orderModel);
        }
    }

    /**
     * Marks the Checkout Session as expired and records a rejected authorization state when needed.
     *
     * @param order order that owns the Checkout Session
     * @param sessionData Checkout Session data
     */
    @Override
    public void markCheckoutSessionExpired(final AbstractOrderModel order, final StripeCheckoutSessionData sessionData) {
        final PaymentTransactionModel transaction = getOrCreateTransaction(order);
        if (StripePaymentTransactionUtils.isCaptured(transaction, sessionData.getId())
                || StripePaymentTransactionUtils.isRejected(transaction, sessionData.getId())) {
            return;
        }

        final PaymentTransactionEntryModel authorization = StripePaymentTransactionUtils
                .findEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId())
                .orElseGet(() -> createEntry(transaction, PaymentTransactionType.AUTHORIZATION, sessionData.getId(),
                        StripeServicesConstants.STATUS_REJECTED, sessionData.getStatus()));
        authorization.setTransactionStatus(StripeServicesConstants.STATUS_REJECTED);
        authorization.setTransactionStatusDetails(sessionData.getStatus());
        getModelService().save(authorization);

        if (order instanceof OrderModel orderModel) {
            orderModel.setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
            getModelService().save(orderModel);
        }
    }

    /**
     * Registers a PaymentIntent authorization entry on the order payment transaction.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentData PaymentIntent data
     */
    @Override
    public void registerPaymentIntent(final AbstractOrderModel order, final StripePaymentIntentData paymentIntentData) {
        final PaymentTransactionModel transaction = getOrCreateTransaction(order);
        if (StripePaymentTransactionUtils.findEntry(transaction, PaymentTransactionType.AUTHORIZATION, paymentIntentData.getId())
                .isEmpty()) {
            createEntry(transaction, PaymentTransactionType.AUTHORIZATION, paymentIntentData.getId(),
                    StripeServicesConstants.STATUS_PENDING, paymentIntentData.getStatus());
        }
    }

    /**
     * Marks the PaymentIntent as succeeded and adds the corresponding capture entry when needed.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentData PaymentIntent data
     */
    @Override
    public void markPaymentIntentSucceeded(final AbstractOrderModel order, final StripePaymentIntentData paymentIntentData) {
        final PaymentTransactionModel transaction = getOrCreateTransaction(order);
        if (StripePaymentTransactionUtils.isCaptured(transaction, paymentIntentData.getId())) {
            return;
        }

        final PaymentTransactionEntryModel authorization = StripePaymentTransactionUtils.findEntry(transaction,
                PaymentTransactionType.AUTHORIZATION,
                paymentIntentData.getId())
                        .orElseGet(() -> createEntry(transaction, PaymentTransactionType.AUTHORIZATION,
                                paymentIntentData.getId(), StripeServicesConstants.STATUS_ACCEPTED,
                                paymentIntentData.getStatus()));
        authorization.setTransactionStatus(StripeServicesConstants.STATUS_ACCEPTED);
        authorization.setTransactionStatusDetails(paymentIntentData.getStatus());
        getModelService().save(authorization);

        if (StripePaymentTransactionUtils.findEntry(transaction, PaymentTransactionType.CAPTURE, paymentIntentData.getId())
                .isEmpty()) {
            createEntry(transaction, PaymentTransactionType.CAPTURE, paymentIntentData.getId(),
                    StripeServicesConstants.STATUS_ACCEPTED, paymentIntentData.getStatus());
        }

        if (order instanceof OrderModel orderModel) {
            orderModel.setStatus(OrderStatus.PAYMENT_CAPTURED);
            getModelService().save(orderModel);
        }
    }

    /**
     * Marks the PaymentIntent as failed and records a rejected authorization state when needed.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentData PaymentIntent data
     */
    @Override
    public void markPaymentIntentFailed(final AbstractOrderModel order, final StripePaymentIntentData paymentIntentData) {
        final PaymentTransactionModel transaction = getOrCreateTransaction(order);
        if (StripePaymentTransactionUtils.isCaptured(transaction, paymentIntentData.getId())
                || StripePaymentTransactionUtils.isRejected(transaction, paymentIntentData.getId())) {
            return;
        }

        final PaymentTransactionEntryModel authorization = StripePaymentTransactionUtils.findEntry(transaction,
                PaymentTransactionType.AUTHORIZATION,
                paymentIntentData.getId())
                        .orElseGet(() -> createEntry(transaction, PaymentTransactionType.AUTHORIZATION,
                                paymentIntentData.getId(), StripeServicesConstants.STATUS_REJECTED,
                                paymentIntentData.getStatus()));
        authorization.setTransactionStatus(StripeServicesConstants.STATUS_REJECTED);
        authorization.setTransactionStatusDetails(paymentIntentData.getStatus());
        getModelService().save(authorization);

        if (order instanceof OrderModel orderModel) {
            orderModel.setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
            getModelService().save(orderModel);
        }
    }

    /**
     * Marks the PaymentIntent as cancelled and records the cancellation state when needed.
     *
     * @param order order that owns the PaymentIntent
     * @param paymentIntentData PaymentIntent data
     */
    @Override
    public void markPaymentIntentCancelled(final AbstractOrderModel order, final StripePaymentIntentData paymentIntentData) {
        final PaymentTransactionModel transaction = getOrCreateTransaction(order);
        if (StripePaymentTransactionUtils.isCaptured(transaction, paymentIntentData.getId())) {
            return;
        }

        final PaymentTransactionEntryModel authorization = StripePaymentTransactionUtils.findEntry(transaction,
                PaymentTransactionType.AUTHORIZATION,
                paymentIntentData.getId())
                        .orElseGet(() -> createEntry(transaction, PaymentTransactionType.AUTHORIZATION,
                                paymentIntentData.getId(), StripeServicesConstants.STATUS_REJECTED,
                                paymentIntentData.getStatus()));
        authorization.setTransactionStatus(StripeServicesConstants.STATUS_REJECTED);
        authorization.setTransactionStatusDetails(paymentIntentData.getStatus());
        getModelService().save(authorization);

        if (StripePaymentTransactionUtils.findEntry(transaction, PaymentTransactionType.CANCEL, paymentIntentData.getId())
                .isEmpty()) {
            createEntry(transaction, PaymentTransactionType.CANCEL, paymentIntentData.getId(),
                    StripeServicesConstants.STATUS_ACCEPTED, paymentIntentData.getStatus());
        }

        if (order instanceof OrderModel orderModel) {
            orderModel.setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
            getModelService().save(orderModel);
        }
    }

    /**
     * Registers a Stripe refund entry on the order payment transaction.
     *
     * @param order refunded order
     * @param paymentReference refunded Stripe reference
     * @param refundData Stripe refund data
     */
    @Override
    public void registerRefund(final AbstractOrderModel order,
                               final String paymentReference,
                               final StripeRefundData refundData) {
        final PaymentTransactionModel transaction = getOrCreateTransaction(order);
        if (!StripePaymentTransactionUtils.findEntry(transaction, PaymentTransactionType.REFUND_FOLLOW_ON, refundData.getId())
                .isEmpty()) {
            return;
        }

        createEntry(transaction, PaymentTransactionType.REFUND_FOLLOW_ON, refundData.getId(),
                StripeStatusUtils.mapRefundStatus(refundData.getStatus()), refundData.getStatus(),
                StripeAmountUtils.toMajorAmount(refundData.getAmount(),
                        StripePaymentTransactionUtils.resolveCurrencyDigits(transaction)), paymentReference);
    }

    /**
     * Returns the latest open PaymentIntent request identifier registered for the supplied order.
     *
     * @param order order to inspect
     * @return latest open PaymentIntent identifier
     */
    @Override
    public Optional<String> findLatestOpenPaymentIntentId(final AbstractOrderModel order) {
        if (CollectionUtils.isEmpty(order.getPaymentTransactions())) {
            return Optional.empty();
        }

        return order.getPaymentTransactions().stream()
                .filter(transaction -> CollectionUtils.isNotEmpty(transaction.getEntries()))
                .flatMap(transaction -> transaction.getEntries().stream())
                .filter(entry -> entry.getType() == PaymentTransactionType.AUTHORIZATION)
                .filter(entry -> StripePaymentReferenceUtils.isPaymentIntentReference(entry.getRequestId()))
                .max(Comparator.comparing(PaymentTransactionEntryModel::getTime))
                .map(PaymentTransactionEntryModel::getRequestId);
    }

    /**
     * Returns whether the supplied order already contains an entry for the Stripe request identifier.
     *
     * @param order order to inspect
     * @param requestId Stripe request identifier
     * @return {@code true} when the entry exists
     */
    @Override
    public boolean hasPaymentTransactionEntry(final AbstractOrderModel order, final String requestId) {
        if (order == null || requestId == null) {
            return false;
        }

        if (CollectionUtils.isNotEmpty(order.getPaymentTransactions()) && order.getPaymentTransactions().stream()
                .filter(transaction -> CollectionUtils.isNotEmpty(transaction.getEntries()))
                .flatMap(transaction -> transaction.getEntries().stream())
                .anyMatch(entry -> requestId.equals(entry.getRequestId()))) {
            return true;
        }

        final FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(ENTRY_BY_REQUEST_ID_QUERY);
        flexibleSearchQuery.addQueryParameter(QUERY_PARAM_REQUEST_ID, requestId);
        final SearchResult<PaymentTransactionEntryModel> searchResult = getFlexibleSearchService().search(flexibleSearchQuery);
        return searchResult.getResult().stream()
                .map(PaymentTransactionEntryModel::getPaymentTransaction)
                .map(PaymentTransactionModel::getOrder)
                .anyMatch(candidate -> candidate != null && candidate.getPk() != null && candidate.getPk().equals(order.getPk()));
    }

    /**
     * Finds an order or cart by code.
     *
     * @param code order or cart code
     * @return matching order model
     */
    @Override
    public Optional<AbstractOrderModel> findOrderByCode(final String code) {
        return findSingle(ORDER_QUERY, code)
                .or(() -> findSingle(CART_QUERY, code))
                .or(() -> findSingle(CART_GUID_QUERY, code));
    }

    /**
     * Finds an order or cart by order code and Stripe request identifier.
     *
     * @param code order or cart code
     * @param requestId Stripe request identifier
     * @return matching order model
     */
    @Override
    public Optional<AbstractOrderModel> findOrderByPaymentReference(final String code, final String requestId) {
        return findSingle(ORDER_QUERY, code)
                .filter(order -> hasPaymentTransactionEntry(order, requestId))
                .or(() -> findSingle(CART_QUERY, code).filter(order -> hasPaymentTransactionEntry(order, requestId)))
                .or(() -> findSingle(CART_GUID_QUERY, code).filter(order -> hasPaymentTransactionEntry(order, requestId)))
                .or(() -> findOrderByPaymentReference(requestId));
    }

    /**
     * Finds the latest order or cart associated with the given Stripe request identifier.
     *
     * @param requestId Stripe request identifier
     * @return matching order or cart
     */
    @Override
    public Optional<AbstractOrderModel> findOrderByPaymentReference(final String requestId) {
        return findByRequestId(ORDER_BY_REQUEST_ID_QUERY, requestId)
                .or(() -> findByRequestId(CART_BY_REQUEST_ID_QUERY, requestId));
    }

    /**
     * Copies Stripe Checkout Session and PaymentIntent entries from the source cart or order into the target order transaction.
     *
     * @param source source cart or order
     * @param target target order
     */
    @Override
    public void synchronizeStripePaymentsToOrder(final AbstractOrderModel source, final OrderModel target) {
        if (source == null || target == null || CollectionUtils.isEmpty(source.getPaymentTransactions())) {
            return;
        }

        final PaymentTransactionModel targetTransaction = getOrCreateTransaction(target);
        source.getPaymentTransactions().stream()
                .filter(transaction -> CollectionUtils.isNotEmpty(transaction.getEntries()))
                .flatMap(transaction -> transaction.getEntries().stream())
                .filter(StripePaymentTransactionUtils::isStripePaymentEntry)
                .sorted(Comparator.comparing(PaymentTransactionEntryModel::getTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(entry -> copyEntry(targetTransaction, entry));

        updateOrderStatus(target, targetTransaction);
    }

    /**
     * Returns the first order or cart matching the given code query.
     *
     * @param query the flexible search query
     * @param code the code to resolve
     * @return the first matching order or cart
     */
    protected Optional<AbstractOrderModel> findSingle(final String query, final String code) {
        final FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(query);
        flexibleSearchQuery.addQueryParameter(QUERY_PARAM_CODE, code);
        final SearchResult<AbstractOrderModel> searchResult = getFlexibleSearchService().search(flexibleSearchQuery);
        final List<AbstractOrderModel> result = searchResult.getResult();
        return CollectionUtils.isEmpty(result) ? Optional.empty() : Optional.of(result.get(0));
    }

    /**
     * Returns the latest order associated with the given Stripe request identifier.
     *
     * @param requestId the Stripe request identifier
     * @return the matching order, when present
     */
    protected Optional<AbstractOrderModel> findByRequestId(final String query, final String requestId) {
        final FlexibleSearchQuery flexibleSearchQuery = new FlexibleSearchQuery(query);
        flexibleSearchQuery.addQueryParameter(QUERY_PARAM_REQUEST_ID, requestId);
        final SearchResult<AbstractOrderModel> searchResult = getFlexibleSearchService().search(flexibleSearchQuery);
        final List<AbstractOrderModel> result = searchResult.getResult();
        return CollectionUtils.isEmpty(result) ? Optional.empty() : Optional.of(result.get(0));
    }
    protected PaymentTransactionModel getOrCreateTransaction(final AbstractOrderModel order) {
        if (CollectionUtils.isNotEmpty(order.getPaymentTransactions())) {
            return order.getPaymentTransactions().get(0);
        }

        final PaymentTransactionModel transaction = getModelService().create(PaymentTransactionModel.class);
        transaction.setCode(order.getCode() + "-stripe");
        transaction.setRequestId(order.getCode());
        transaction.setOrder(order);
        transaction.setCurrency(order.getCurrency());
        transaction.setPaymentProvider(StripeServicesConstants.PAYMENT_PROVIDER);
        transaction.setPlannedAmount(StripeAmountUtils.calculateOrderTotal(order));
        transaction.setInfo(order.getPaymentInfo());
        transaction.setEntries(new ArrayList<>());
        StripePaymentTransactionUtils.attachTransaction(order, transaction);
        getModelService().save(order);
        getModelService().save(transaction);
        getModelService().refresh(order);
        return transaction;
    }

    /**
     * Creates a transaction entry using the transaction planned amount.
     *
     * @param transaction the payment transaction
     * @param type the entry type
     * @param requestId the Stripe request identifier
     * @param transactionStatus the transaction status
     * @param details the transaction status details
     * @return the created entry
     */
    protected PaymentTransactionEntryModel createEntry(final PaymentTransactionModel transaction,
                                                       final PaymentTransactionType type,
                                                       final String requestId,
                                                       final String transactionStatus,
                                                       final String details) {
        return createEntry(transaction, type, requestId, transactionStatus, details, transaction.getPlannedAmount(), null);
    }

    /**
     * Creates a transaction entry with an explicit amount and optional request token.
     *
     * @param transaction the payment transaction
     * @param type the entry type
     * @param requestId the Stripe request identifier
     * @param transactionStatus the transaction status
     * @param details the transaction status details
     * @param amount the entry amount
     * @param requestToken the optional request token
     * @return the created entry
     */
    protected PaymentTransactionEntryModel createEntry(final PaymentTransactionModel transaction,
                                                       final PaymentTransactionType type,
                                                       final String requestId,
                                                       final String transactionStatus,
                                                       final String details,
                                                       final BigDecimal amount,
                                                       final String requestToken) {
        final PaymentTransactionEntryModel entry = getModelService().create(PaymentTransactionEntryModel.class);
        final int entryIndex = CollectionUtils.isEmpty(transaction.getEntries()) ? 1 : (transaction.getEntries().size() + 1);
        entry.setCode(transaction.getCode() + "-" + type.getCode() + "-" + entryIndex);
        entry.setPaymentTransaction(transaction);
        entry.setType(type);
        entry.setRequestId(requestId);
        if (requestToken != null) {
            entry.setRequestToken(requestToken);
        }
        entry.setTime(getTimeService().getCurrentTime());
        entry.setAmount(amount);
        entry.setCurrency(transaction.getCurrency());
        entry.setTransactionStatus(transactionStatus);
        entry.setTransactionStatusDetails(details);
        StripePaymentTransactionUtils.attachEntry(transaction, entry);
        getModelService().save(transaction);
        getModelService().save(entry);
        getModelService().refresh(transaction);
        return entry;
    }

    /**
     * Copies a Stripe transaction entry into the target transaction.
     *
     * @param transaction the target transaction
     * @param sourceEntry the source entry to copy
     */
    protected void copyEntry(final PaymentTransactionModel transaction, final PaymentTransactionEntryModel sourceEntry) {
        final PaymentTransactionEntryModel targetEntry = StripePaymentTransactionUtils
                .findEntry(transaction, sourceEntry.getType(), sourceEntry.getRequestId())
                .orElseGet(() -> createEntry(transaction, sourceEntry.getType(), sourceEntry.getRequestId(),
                        sourceEntry.getTransactionStatus(), sourceEntry.getTransactionStatusDetails()));
        targetEntry.setAmount(sourceEntry.getAmount());
        targetEntry.setCurrency(sourceEntry.getCurrency());
        targetEntry.setTransactionStatus(sourceEntry.getTransactionStatus());
        targetEntry.setTransactionStatusDetails(sourceEntry.getTransactionStatusDetails());
        if (sourceEntry.getTime() != null) {
            targetEntry.setTime(sourceEntry.getTime());
        }
        getModelService().save(targetEntry);
    }

    /**
     * Updates the order payment status from the target Stripe transaction state.
     *
     * @param order the order to update
     * @param transaction the Stripe payment transaction
     */
    protected void updateOrderStatus(final OrderModel order, final PaymentTransactionModel transaction) {
        if (StripePaymentTransactionUtils.hasEntryWithStatus(transaction, PaymentTransactionType.CAPTURE,
                StripeServicesConstants.STATUS_ACCEPTED)) {
            order.setStatus(OrderStatus.PAYMENT_CAPTURED);
            getModelService().save(order);
            return;
        }

        if (StripePaymentTransactionUtils.hasEntryWithStatus(transaction, PaymentTransactionType.AUTHORIZATION,
                StripeServicesConstants.STATUS_REJECTED)) {
            order.setStatus(OrderStatus.PAYMENT_NOT_CAPTURED);
            getModelService().save(order);
        }
    }

    protected ModelService getModelService() {
        return modelService;
    }

    protected FlexibleSearchService getFlexibleSearchService() {
        return flexibleSearchService;
    }

    protected TimeService getTimeService() {
        return timeService;
    }
}
