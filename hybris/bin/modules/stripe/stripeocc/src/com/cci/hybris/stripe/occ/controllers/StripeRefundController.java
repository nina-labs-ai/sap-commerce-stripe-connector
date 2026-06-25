
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.occ.controllers;

import com.cci.hybris.stripe.facades.refund.StripeRefundFacade;
import com.cci.hybris.stripe.occ.constants.StripeOccConstants;
import com.cci.hybris.stripe.occ.dto.StripeRefundRequestWsDTO;
import com.cci.hybris.stripe.occ.dto.StripeRefundWsDTO;
import com.cci.hybris.stripe.facades.data.StripeRefundFacadeData;

import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;

import java.math.BigDecimal;
import java.util.Objects;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCC controller for order-bound Stripe refunds.
 */
@RestController
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orders/{code}/stripe/refunds")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Tag(name = "Stripe Refunds",
        description = "OCC endpoint for creating Stripe refunds against an owned order payment reference.")
public class StripeRefundController {

    @Resource(name = "stripeRefundFacade")
    private StripeRefundFacade stripeRefundFacade;

    @Secured({"ROLE_CUSTOMERGROUP", "ROLE_GUEST", "ROLE_CUSTOMERMANAGERGROUP", "ROLE_TRUSTED_CLIENT", "ROLE_CLIENT"})
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a Stripe refund for an owned order",
            description = "Creates a Stripe refund for the specified order and payment reference. "
                    + "The order must belong to the authenticated OCC context and current base site.")
    @ApiResponse(responseCode = "201", description = "Stripe refund created",
            content = @Content(schema = @Schema(implementation = StripeRefundWsDTO.class)))
    @ApiResponse(responseCode = "400",
            description = "The order is not available in the current OCC context or the payment reference is invalid")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public StripeRefundWsDTO createRefund(
            @Parameter(description = "Order code or guest order guid", required = true)
            @PathVariable final String code,
            @RequestBody final StripeRefundRequestWsDTO request) {
        validateRequest(request);
        return toRefundWsDTO(
                getStripeRefundFacade().createRefundForOrder(code, request.getPaymentReference(), request.getAmount()));
    }

    private static void validateRequest(final StripeRefundRequestWsDTO request) {
        if (request == null || StringUtils.isBlank(request.getPaymentReference())) {
            throw new IllegalArgumentException(StripeOccConstants.REFUND_PAYMENT_REFERENCE_REQUIRED_MESSAGE);
        }

        final BigDecimal amount = request.getAmount();
        if (amount != null && amount.signum() <= 0) {
            throw new IllegalArgumentException(StripeOccConstants.REFUND_AMOUNT_POSITIVE_MESSAGE);
        }
    }

    private static StripeRefundWsDTO toRefundWsDTO(final StripeRefundFacadeData facadeData) {
        final StripeRefundFacadeData source = Objects.requireNonNull(facadeData, "facadeData");
        final StripeRefundWsDTO dto = new StripeRefundWsDTO();
        dto.setId(source.getId());
        dto.setPaymentIntentId(source.getPaymentIntentId());
        dto.setStatus(source.getStatus());
        dto.setAmount(source.getAmount());
        dto.setCurrency(source.getCurrency());
        dto.setFormattedAmount(source.getFormattedAmount());
        dto.setOrderCode(source.getOrderCode());
        dto.setPaymentReference(source.getPaymentReference());
        return dto;
    }

    /**
     * Exposes refund facade dependency for extensible subclasses.
     *
     * @return refund facade
     */
    protected StripeRefundFacade getStripeRefundFacade() {
        return stripeRefundFacade;
    }
}
