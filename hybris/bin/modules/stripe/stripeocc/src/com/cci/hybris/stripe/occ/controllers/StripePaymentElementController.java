
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.occ.controllers;

import com.cci.hybris.stripe.facades.elements.StripePaymentElementFacade;
import com.cci.hybris.stripe.facades.data.StripePaymentElementFacadeData;
import com.cci.hybris.stripe.occ.constants.StripeOccConstants;
import com.cci.hybris.stripe.occ.dto.StripePaymentElementWsDTO;

import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercewebservicescommons.annotation.SecurePortalUnauthenticatedAccess;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import java.util.Objects;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * OCC controller for Stripe Payment Elements.
 */
@RestController
@RequestMapping(value = "/{baseSiteId}/users/{userId}/stripe/elements")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Tag(name = "Stripe Payment Elements",
        description = "OCC endpoints for Stripe Payment Elements bootstrap, current-cart PaymentIntent lookup, and PaymentIntent cancellation.")
public class StripePaymentElementController extends AbstractStripeCartContextController {

    @Resource(name = "stripePaymentElementFacade")
    private StripePaymentElementFacade stripePaymentElementFacade;
    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @PostMapping(value = "/intent")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Create a Stripe PaymentIntent for Payment Elements",
            description = "Creates or returns the current-cart PaymentIntent bootstrap payload for Stripe Payment Elements. "
                    + "The response is frontend-safe and contains the publishable key and return URL, but never the Stripe secret key.")
    @ApiResponse(responseCode = "201", description = "Stripe PaymentIntent bootstrap data returned",
            content = @Content(schema = @Schema(implementation = StripePaymentElementWsDTO.class)))
    @ApiResponse(responseCode = "400", description = "The session cart is missing or not ready for payment")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public StripePaymentElementWsDTO createPaymentIntent(
            @PathVariable final String userId,
            @Parameter(description = "Anonymous cart guid or cart code when the current cart cannot be restored from the OCC user context.")
            @RequestParam(required = false) final String cartId) {
        loadCartForContext(userId, cartId, StripeOccConstants.ELEMENTS_ANONYMOUS_CART_REQUIRED_MESSAGE);
        return toPaymentElementWsDTO(getStripePaymentElementFacade().createPaymentIntentForCart());
    }

    @PostMapping(value = "/intent/{paymentIntentId}/cancel")
    @ResponseStatus(HttpStatus.OK)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Cancel the current cart Stripe PaymentIntent",
            description = "Cancels the current cart's Stripe PaymentIntent when the shopper abandons or resets Payment Elements. "
                    + "The provided identifier must belong to the current cart and base site.")
    @ApiResponse(responseCode = "200", description = "Stripe PaymentIntent canceled",
            content = @Content(schema = @Schema(implementation = StripePaymentElementWsDTO.class)))
    @ApiResponse(responseCode = "400", description = "The requested Stripe PaymentIntent is not available for the current cart context")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public StripePaymentElementWsDTO cancelPaymentIntent(
            @PathVariable final String userId,
            @Parameter(description = "Anonymous cart guid or cart code when the current cart cannot be restored from the OCC user context.")
            @RequestParam(required = false) final String cartId,
            @Parameter(description = "Stripe PaymentIntent identifier returned by the create endpoint", required = true)
            @PathVariable final String paymentIntentId) {
        loadCartForContext(userId, cartId, StripeOccConstants.ELEMENTS_ANONYMOUS_CART_REQUIRED_MESSAGE);
        return toPaymentElementWsDTO(getStripePaymentElementFacade().cancelPaymentIntentForCart(paymentIntentId));
    }

    @GetMapping(value = "/intent/{paymentIntentId}")
    @ResponseStatus(HttpStatus.OK)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Get a Stripe PaymentIntent bootstrap payload",
            description = "Returns the frontend bootstrap payload for the current cart's Stripe PaymentIntent. "
                    + "The provided identifier must belong to the current cart and base site.")
    @ApiResponse(responseCode = "200", description = "Stripe PaymentIntent bootstrap data returned",
            content = @Content(schema = @Schema(implementation = StripePaymentElementWsDTO.class)))
    @ApiResponse(responseCode = "400", description = "The requested Stripe PaymentIntent is not available for the current context")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public StripePaymentElementWsDTO getPaymentIntent(
            @PathVariable final String userId,
            @Parameter(description = "Anonymous cart guid or cart code when the current cart cannot be restored from the OCC user context.")
            @RequestParam(required = false) final String cartId,
            @Parameter(description = "Stripe PaymentIntent identifier returned by the create endpoint", required = true)
            @PathVariable final String paymentIntentId) {
        loadCartForContext(userId, cartId, StripeOccConstants.ELEMENTS_ANONYMOUS_CART_REQUIRED_MESSAGE);
        return toPaymentElementWsDTO(getStripePaymentElementFacade().getPaymentIntent(paymentIntentId));
    }

    @PostMapping(value = "/intent/{paymentIntentId}/finalize")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Finalize a paid Stripe PaymentIntent",
            description = "Places or retrieves the SAP Commerce order that belongs to a paid Stripe PaymentIntent. "
                    + "Payment Elements returns should call this endpoint instead of the generic OCC orders endpoint.")
    @ApiResponse(responseCode = "201", description = "Order created or returned",
            content = @Content(schema = @Schema(implementation = OrderWsDTO.class)))
    @ApiResponse(responseCode = "400", description = "The Stripe PaymentIntent is not ready for order placement")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public OrderWsDTO finalizePaymentIntent(
            @PathVariable final String userId,
            @Parameter(description = "Anonymous cart guid or cart code when the storefront can still provide it.")
            @RequestParam(required = false) final String cartId,
            @ApiFieldsParam
            @RequestParam(defaultValue = StripeOccConstants.DEFAULT_FIELD_SET) final String fields,
            @Parameter(description = "Stripe PaymentIntent identifier returned by the create endpoint", required = true)
            @PathVariable final String paymentIntentId) throws InvalidCartException {
        if ((cartId != null && !cartId.isBlank()) || !isAnonymous(userId)) {
            loadCartForFinalizeContextIfPossible(userId, cartId, StripeOccConstants.ELEMENTS_ANONYMOUS_CART_REQUIRED_MESSAGE);
        }
        final OrderData orderData = getStripePaymentElementFacade().finalizePaymentIntentForContext(paymentIntentId, cartId);
        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }

    private static StripePaymentElementWsDTO toPaymentElementWsDTO(final StripePaymentElementFacadeData facadeData) {
        final StripePaymentElementFacadeData source = Objects.requireNonNull(facadeData, "facadeData");
        final StripePaymentElementWsDTO dto = new StripePaymentElementWsDTO();
        dto.setId(source.getId());
        dto.setClientSecret(source.getClientSecret());
        dto.setStatus(source.getStatus());
        dto.setAmount(source.getAmount());
        dto.setCurrency(source.getCurrency());
        dto.setClientReferenceId(source.getClientReferenceId());
        dto.setPublishableKey(source.getPublishableKey());
        dto.setPaymentOptionId(source.getPaymentOptionId());
        dto.setPaymentMethod(source.getPaymentMethod());
        dto.setFormattedAmount(source.getFormattedAmount());
        dto.setReturnUrl(source.getReturnUrl());
        return dto;
    }

    /**
     * Exposes payment-element facade dependency for extensible subclasses.
     *
     * @return payment-element facade
     */
    protected StripePaymentElementFacade getStripePaymentElementFacade() {
        return stripePaymentElementFacade;
    }

    /**
     * Exposes data mapper dependency for extensible subclasses.
     *
     * @return OCC data mapper
     */
    protected DataMapper getDataMapper() {
        return dataMapper;
    }

}
