
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.occ.controllers;

import com.cci.hybris.stripe.facades.checkout.StripeCheckoutFacade;
import com.cci.hybris.stripe.facades.data.StripeCheckoutSessionFacadeData;
import com.cci.hybris.stripe.facades.data.StripePublicConfigurationFacadeData;
import com.cci.hybris.stripe.occ.constants.StripeOccConstants;
import com.cci.hybris.stripe.occ.dto.StripeCheckoutSessionWsDTO;
import com.cci.hybris.stripe.occ.dto.StripePublicConfigurationWsDTO;

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
 * OCC controller for Stripe Checkout.
 */
@RestController
@RequestMapping(value = "/{baseSiteId}/users/{userId}/stripe/checkout")
@CacheControl(directive = CacheControlDirective.NO_CACHE)
@Tag(name = "Stripe Checkout", description = "OCC endpoints for Stripe Checkout Session creation, retrieval, expiration, and public frontend bootstrap configuration.")
public class StripeCheckoutController extends AbstractStripeCartContextController {

    @Resource(name = "stripeCheckoutFacade")
    private StripeCheckoutFacade stripeCheckoutFacade;
    @Resource(name = "dataMapper")
    private DataMapper dataMapper;

    @PostMapping(value = "/session")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Create a Stripe Checkout Session",
            description = "Creates a hosted Stripe Checkout Session for the current session cart. "
                    + "The authenticated user must already have a current cart with items and totals.")
    @ApiResponse(responseCode = "201", description = "Stripe Checkout Session created",
            content = @Content(schema = @Schema(implementation = StripeCheckoutSessionWsDTO.class)))
    @ApiResponse(responseCode = "400", description = "The session cart is missing or not ready for checkout")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public StripeCheckoutSessionWsDTO createCheckoutSession(
            @PathVariable final String userId,
            @Parameter(description = "Anonymous cart guid or cart code when the current cart cannot be restored from the OCC user context.")
            @RequestParam(required = false) final String cartId) {
        loadCartForContext(userId, cartId, StripeOccConstants.CHECKOUT_ANONYMOUS_CART_REQUIRED_MESSAGE);
        return toCheckoutSessionWsDTO(getStripeCheckoutFacade().createCheckoutSessionForCart());
    }

    @PostMapping(value = "/session/{sessionId}/expire")
    @ResponseStatus(HttpStatus.OK)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Expire a Stripe Checkout Session for the current cart",
            description = "Expires the current cart's Stripe Checkout Session when the shopper abandons or restarts checkout. "
                    + "The provided identifier must belong to the current cart and base site.")
    @ApiResponse(responseCode = "200", description = "Stripe Checkout Session expired",
            content = @Content(schema = @Schema(implementation = StripeCheckoutSessionWsDTO.class)))
    @ApiResponse(responseCode = "400", description = "The requested Stripe Checkout Session is not available for the current cart context")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public StripeCheckoutSessionWsDTO expireCheckoutSession(
            @PathVariable final String userId,
            @Parameter(description = "Stripe Checkout Session identifier returned by the create endpoint", required = true)
            @PathVariable final String sessionId,
            @Parameter(description = "Anonymous cart guid or cart code when the current cart cannot be restored from the OCC user context.")
            @RequestParam(required = false) final String cartId) {
        loadCartForContext(userId, cartId, StripeOccConstants.CHECKOUT_ANONYMOUS_CART_REQUIRED_MESSAGE);
        return toCheckoutSessionWsDTO(getStripeCheckoutFacade().expireCheckoutSessionForCart(sessionId));
    }

    @GetMapping(value = "/session/{sessionId}")
    @ResponseStatus(HttpStatus.OK)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Get a Stripe Checkout Session",
            description = "Returns the Stripe Checkout Session that belongs to the authenticated context. "
                    + "The session identifier must come from a session previously created by this connector.")
    @ApiResponse(responseCode = "200", description = "Stripe Checkout Session returned",
            content = @Content(schema = @Schema(implementation = StripeCheckoutSessionWsDTO.class)))
    @ApiResponse(responseCode = "400", description = "The requested Stripe Checkout Session is not available for the current context")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public StripeCheckoutSessionWsDTO getCheckoutSession(
            @PathVariable final String userId,
            @Parameter(description = "Anonymous cart guid or cart code when the current cart cannot be restored from the OCC user context.")
            @RequestParam(required = false) final String cartId,
            @Parameter(description = "Original cart or order code recorded on the Stripe Checkout Session.")
            @RequestParam(required = false) final String orderCode,
            @Parameter(description = "Stripe Checkout Session identifier returned by the create endpoint", required = true)
            @PathVariable final String sessionId) {
        loadCartForReadContext(userId, cartId, orderCode, StripeOccConstants.CHECKOUT_ANONYMOUS_CART_REQUIRED_MESSAGE);
        return toCheckoutSessionWsDTO(getStripeCheckoutFacade()
                .getCheckoutSessionForContext(sessionId, resolveReference(orderCode, cartId)));
    }

    @PostMapping(value = "/session/{sessionId}/finalize")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Finalize a paid Stripe Checkout Session",
            description = "Places or retrieves the SAP Commerce order that belongs to a paid Stripe Checkout Session. "
                    + "Hosted Stripe returns should call this endpoint instead of the generic OCC orders endpoint.")
    @ApiResponse(responseCode = "201", description = "Order created or returned",
            content = @Content(schema = @Schema(implementation = OrderWsDTO.class)))
    @ApiResponse(responseCode = "400", description = "The Stripe Checkout Session is not ready for order placement")
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public OrderWsDTO finalizeCheckoutSession(
            @PathVariable final String userId,
            @Parameter(description = "Anonymous cart guid or cart code when the storefront can still provide it.")
            @RequestParam(required = false) final String cartId,
            @Parameter(description = "Original cart or order code recorded on the Stripe Checkout Session.")
            @RequestParam(required = false) final String orderCode,
            @ApiFieldsParam
            @RequestParam(defaultValue = StripeOccConstants.DEFAULT_FIELD_SET) final String fields,
            @Parameter(description = "Stripe Checkout Session identifier returned by the create endpoint", required = true)
            @PathVariable final String sessionId) throws InvalidCartException {
        final OrderData orderData = getStripeCheckoutFacade().finalizeCheckoutSessionForContext(sessionId,
                resolveReference(orderCode, cartId));
        return getDataMapper().map(orderData, OrderWsDTO.class, fields);
    }

    @GetMapping(value = "/config")
    @ResponseStatus(HttpStatus.OK)
    @SecurePortalUnauthenticatedAccess
    @Operation(summary = "Get public Stripe Checkout configuration",
            description = "Returns frontend-safe Stripe Checkout bootstrap data for the current base site. "
                    + "This endpoint never returns secret keys.")
    @ApiResponse(responseCode = "200", description = "Public Stripe Checkout configuration returned",
            content = @Content(schema = @Schema(implementation = StripePublicConfigurationWsDTO.class)))
    @ApiResponse(responseCode = "401", description = "Authentication is required")
    public StripePublicConfigurationWsDTO getConfiguration() {
        final StripePublicConfigurationFacadeData facadeData = getStripeCheckoutFacade().getPublicConfiguration();
        return toPublicConfigurationWsDTO(facadeData);
    }

    private static StripeCheckoutSessionWsDTO toCheckoutSessionWsDTO(final StripeCheckoutSessionFacadeData facadeData) {
        final StripeCheckoutSessionFacadeData source = Objects.requireNonNull(facadeData, "facadeData");
        final StripeCheckoutSessionWsDTO dto = new StripeCheckoutSessionWsDTO();
        dto.setId(source.getId());
        dto.setUrl(source.getUrl());
        dto.setStatus(source.getStatus());
        dto.setPaymentStatus(source.getPaymentStatus());
        dto.setClientReferenceId(source.getClientReferenceId());
        return dto;
    }

    private static StripePublicConfigurationWsDTO toPublicConfigurationWsDTO(
            final StripePublicConfigurationFacadeData facadeData) {
        final StripePublicConfigurationFacadeData source = Objects.requireNonNull(facadeData, "facadeData");
        final StripePublicConfigurationWsDTO dto = new StripePublicConfigurationWsDTO();
        dto.setPublishableKey(source.getPublishableKey());
        dto.setPaymentOptionId(source.getPaymentOptionId());
        dto.setPaymentMethod(source.getPaymentMethod());
        return dto;
    }

    /**
     * Exposes checkout facade dependency for extensible subclasses.
     *
     * @return checkout facade
     */
    protected StripeCheckoutFacade getStripeCheckoutFacade() {
        return stripeCheckoutFacade;
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
