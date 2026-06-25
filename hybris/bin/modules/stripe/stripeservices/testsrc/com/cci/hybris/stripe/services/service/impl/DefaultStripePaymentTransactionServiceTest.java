package com.cci.hybris.stripe.services.service.impl;

import com.cci.hybris.stripe.services.constants.StripeServicesConstants;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.payment.enums.PaymentTransactionType;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import de.hybris.platform.servicelayer.search.SearchResult;
import de.hybris.platform.servicelayer.time.TimeService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultStripePaymentTransactionServiceTest {

    @InjectMocks
    private DefaultStripePaymentTransactionService service;

    @Mock
    private ModelService modelService;
    @Mock
    private FlexibleSearchService flexibleSearchService;
    @Mock
    private TimeService timeService;
    @Mock
    private SearchResult<OrderModel> orderByCodeSearchResult;
    @Mock
    private SearchResult<OrderModel> orderByRequestIdSearchResult;
    @Mock
    private SearchResult<CartModel> cartByRequestIdSearchResult;
    @Mock
    private SearchResult<CartModel> cartByCodeSearchResult;
    @Mock
    private SearchResult<CartModel> cartByGuidSearchResult;

    @Before
    public void setUp() {
        when(orderByCodeSearchResult.getResult()).thenReturn(Collections.emptyList());
        when(orderByRequestIdSearchResult.getResult()).thenReturn(Collections.emptyList());
        when(cartByRequestIdSearchResult.getResult()).thenReturn(Collections.emptyList());
        when(cartByCodeSearchResult.getResult()).thenReturn(Collections.emptyList());
        when(cartByGuidSearchResult.getResult()).thenReturn(Collections.emptyList());
    }

    @Test
    public void findOrderByPaymentReference_orderCodeWithoutReferenceFallsBackToRequestIdMatch() {
        // Arrange
        final PaymentTransactionEntryModel matchingEntry = new PaymentTransactionEntryModel();
        matchingEntry.setRequestId("pi_test_123");
        final PaymentTransactionModel matchingTransaction = new PaymentTransactionModel();
        matchingTransaction.setEntries(Collections.singletonList(matchingEntry));

        final OrderModel orderByRequestId = new OrderModel();
        orderByRequestId.setCode("order-002");
        orderByRequestId.setPaymentTransactions(Collections.singletonList(matchingTransaction));

        when(orderByRequestIdSearchResult.getResult()).thenReturn(Collections.singletonList(orderByRequestId));
        when(flexibleSearchService.search(any(FlexibleSearchQuery.class))).thenReturn((SearchResult) orderByCodeSearchResult,
                (SearchResult) cartByCodeSearchResult, (SearchResult) cartByGuidSearchResult,
                (SearchResult) orderByRequestIdSearchResult);

        // Act
        final OrderModel result = (OrderModel) service.findOrderByPaymentReference("order-001", "pi_test_123").orElse(null);

        // Assert
        assertSame(orderByRequestId, result);
    }

    @Test
    public void findOrderByPaymentReference_requestIdFallsBackToCartMatch() {
        // Arrange
        final PaymentTransactionEntryModel matchingEntry = new PaymentTransactionEntryModel();
        matchingEntry.setRequestId("pi_test_123");
        final PaymentTransactionModel matchingTransaction = new PaymentTransactionModel();
        matchingTransaction.setEntries(Collections.singletonList(matchingEntry));

        final CartModel cartByRequestId = new CartModel();
        cartByRequestId.setCode("cart-002");
        cartByRequestId.setPaymentTransactions(Collections.singletonList(matchingTransaction));

        when(flexibleSearchService.search(argThat(queryWith("FROM {Order AS o JOIN PaymentTransaction AS t"))))
                .thenReturn((SearchResult) orderByRequestIdSearchResult);
        when(flexibleSearchService.search(argThat(queryWith("FROM {Cart AS c JOIN PaymentTransaction AS t"))))
                .thenReturn((SearchResult) cartByRequestIdSearchResult);
        when(cartByRequestIdSearchResult.getResult()).thenReturn(Collections.singletonList(cartByRequestId));

        // Act
        final CartModel result = (CartModel) service.findOrderByPaymentReference("pi_test_123").orElse(null);

        // Assert
        assertSame(cartByRequestId, result);
    }

    @Test
    public void findOrderByPaymentReference_cartCodeMatchWinsOverGlobalRequestIdMatch() {
        // Arrange
        final PaymentTransactionEntryModel matchingEntry = new PaymentTransactionEntryModel();
        matchingEntry.setRequestId("pi_test_123");
        final PaymentTransactionModel matchingTransaction = new PaymentTransactionModel();
        matchingTransaction.setEntries(Collections.singletonList(matchingEntry));

        final CartModel cartByCode = new CartModel();
        cartByCode.setCode("cart-001");
        cartByCode.setPaymentTransactions(Collections.singletonList(matchingTransaction));

        when(cartByCodeSearchResult.getResult()).thenReturn(Collections.singletonList(cartByCode));
        when(flexibleSearchService.search(argThat(queryWith("SELECT {pk} FROM {Order} WHERE {code}=?code"))))
                .thenReturn((SearchResult) orderByCodeSearchResult);
        when(flexibleSearchService.search(argThat(queryWith("SELECT {pk} FROM {Cart} WHERE {code}=?code"))))
                .thenReturn((SearchResult) cartByCodeSearchResult);

        // Act
        final CartModel result = (CartModel) service.findOrderByPaymentReference("cart-001", "pi_test_123").orElse(null);

        // Assert
        assertSame(cartByCode, result);
    }

    @Test
    public void findOrderByPaymentReference_cartGuidFallsBackToMatchingCart() {
        // Arrange
        final PaymentTransactionEntryModel matchingEntry = new PaymentTransactionEntryModel();
        matchingEntry.setRequestId("pi_test_123");
        final PaymentTransactionModel matchingTransaction = new PaymentTransactionModel();
        matchingTransaction.setEntries(Collections.singletonList(matchingEntry));

        final CartModel cartByGuid = new CartModel();
        cartByGuid.setCode("cart-002");
        cartByGuid.setPaymentTransactions(Collections.singletonList(matchingTransaction));

        when(flexibleSearchService.search(argThat(queryWith("SELECT {pk} FROM {Order} WHERE {code}=?code"))))
                .thenReturn((SearchResult) orderByCodeSearchResult);
        when(flexibleSearchService.search(argThat(queryWith("SELECT {pk} FROM {Cart} WHERE {code}=?code"))))
                .thenReturn((SearchResult) cartByCodeSearchResult);
        when(flexibleSearchService.search(argThat(queryWith("SELECT {pk} FROM {Cart} WHERE {guid}=?code"))))
                .thenReturn((SearchResult) cartByGuidSearchResult);
        when(cartByGuidSearchResult.getResult()).thenReturn(Collections.singletonList(cartByGuid));

        // Act
        final CartModel result = (CartModel) service.findOrderByPaymentReference("guid-123", "pi_test_123").orElse(null);

        // Assert
        assertSame(cartByGuid, result);
    }

    @Test
    public void findOrderByCode_cartGuidFallsBackToMatchingCart() {
        // Arrange
        final CartModel cartByGuid = new CartModel();
        cartByGuid.setCode("cart-002");

        when(flexibleSearchService.search(argThat(queryWith("SELECT {pk} FROM {Order} WHERE {code}=?code"))))
                .thenReturn((SearchResult) orderByCodeSearchResult);
        when(flexibleSearchService.search(argThat(queryWith("SELECT {pk} FROM {Cart} WHERE {code}=?code"))))
                .thenReturn((SearchResult) cartByCodeSearchResult);
        when(flexibleSearchService.search(argThat(queryWith("SELECT {pk} FROM {Cart} WHERE {guid}=?code"))))
                .thenReturn((SearchResult) cartByGuidSearchResult);
        when(cartByGuidSearchResult.getResult()).thenReturn(Collections.singletonList(cartByGuid));

        // Act
        final CartModel result = (CartModel) service.findOrderByCode("guid-123").orElse(null);

        // Assert
        assertSame(cartByGuid, result);
    }

    @Test
    public void synchronizeStripePaymentsToOrder_capturedCartEntries_marksOrderCaptured() {
        // Arrange
        final CartModel cart = new CartModel();
        cart.setPaymentTransactions(Collections.singletonList(createSourceTransaction()));

        final OrderModel order = new OrderModel();
        order.setCode("order-001");
        order.setPaymentTransactions(Collections.emptyList());
        order.setCurrency(createCurrency());
        order.setTotalPrice(Double.valueOf(10D));
        order.setNet(Boolean.FALSE);

        final PaymentTransactionModel createdTransaction = new PaymentTransactionModel();
        final PaymentTransactionEntryModel createdAuthorization = new PaymentTransactionEntryModel();
        final PaymentTransactionEntryModel createdCapture = new PaymentTransactionEntryModel();

        when(modelService.create(PaymentTransactionModel.class)).thenReturn(createdTransaction);
        when(modelService.create(PaymentTransactionEntryModel.class)).thenReturn(createdAuthorization, createdCapture);
        when(timeService.getCurrentTime()).thenReturn(new Date(1L), new Date(2L));

        // Act
        service.synchronizeStripePaymentsToOrder(cart, order);

        // Assert
        assertEquals(OrderStatus.PAYMENT_CAPTURED, order.getStatus());
        assertEquals(1, order.getPaymentTransactions().size());
        assertEquals(2, createdTransaction.getEntries().size());
        assertEquals("cs_test_123", createdTransaction.getEntries().get(0).getRequestId());
        assertEquals(PaymentTransactionType.CAPTURE, createdTransaction.getEntries().get(1).getType());
    }

    protected PaymentTransactionModel createSourceTransaction() {
        final PaymentTransactionEntryModel authorization = new PaymentTransactionEntryModel();
        authorization.setRequestId("cs_test_123");
        authorization.setType(PaymentTransactionType.AUTHORIZATION);
        authorization.setTransactionStatus(StripeServicesConstants.STATUS_ACCEPTED);
        authorization.setTransactionStatusDetails("succeeded");
        authorization.setAmount(BigDecimal.TEN);
        authorization.setCurrency(createCurrency());
        authorization.setTime(new Date(1L));

        final PaymentTransactionEntryModel capture = new PaymentTransactionEntryModel();
        capture.setRequestId("cs_test_123");
        capture.setType(PaymentTransactionType.CAPTURE);
        capture.setTransactionStatus(StripeServicesConstants.STATUS_ACCEPTED);
        capture.setTransactionStatusDetails("succeeded");
        capture.setAmount(BigDecimal.TEN);
        capture.setCurrency(createCurrency());
        capture.setTime(new Date(2L));

        final PaymentTransactionModel transaction = new PaymentTransactionModel();
        transaction.setCode("cart-001-stripe");
        transaction.setPlannedAmount(BigDecimal.TEN);
        transaction.setCurrency(createCurrency());
        transaction.setEntries(List.of(authorization, capture));
        return transaction;
    }

    protected CurrencyModel createCurrency() {
        final CurrencyModel currency = new CurrencyModel();
        currency.setIsocode("USD");
        currency.setDigits(Integer.valueOf(2));
        return currency;
    }

    protected ArgumentMatcher<FlexibleSearchQuery> queryWith(final String snippet) {
        return query -> query != null && query.getQuery() != null && query.getQuery().contains(snippet);
    }
}
