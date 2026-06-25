import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Order } from '@spartacus/order/root';
import { firstValueFrom } from 'rxjs';
import { StripeOccEndpointService } from './stripe-occ-endpoint.service';

export interface StripeCheckoutConfiguration {
  publishableKey: string;
  paymentOptionId: string;
  paymentMethod: string;
}

export interface StripeCheckoutSession {
  id: string;
  url: string;
  status?: string;
  paymentStatus?: string;
  clientReferenceId?: string;
}

export interface StripeCheckoutSessionPollingOptions {
  attempts?: number;
  intervalMs?: number;
  cartId?: string;
  orderCode?: string;
}

interface SapPaymentOptionRequest {
  sapPaymentOptionId: string;
}

@Injectable({
  providedIn: 'root',
})
export class StripeCheckoutService {
  constructor(
    private readonly http: HttpClient,
    private readonly stripeOccEndpointService: StripeOccEndpointService
  ) {}

  getConfiguration() {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.get<StripeCheckoutConfiguration>(
        this.stripeOccEndpointService.userUrl(
          context,
          'stripe/checkout/config'
        )
      )
    );
  }

  createCheckoutSession(cartId?: string) {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.post<StripeCheckoutSession>(
        this.stripeOccEndpointService.userUrl(
          context,
          'stripe/checkout/session'
        ),
        {},
        this.stripeOccEndpointService.cartOptions(cartId)
      )
    );
  }

  getCheckoutSession(sessionId: string, cartId?: string, orderCode?: string) {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.get<StripeCheckoutSession>(
        this.stripeOccEndpointService.userUrl(
          context,
          `stripe/checkout/session/${sessionId}`
        ),
        this.stripeOccEndpointService.contextOptions(cartId, orderCode)
      )
    );
  }

  expireCheckoutSession(sessionId: string, cartId?: string) {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.post<StripeCheckoutSession>(
        this.stripeOccEndpointService.userUrl(
          context,
          `stripe/checkout/session/${sessionId}/expire`
        ),
        {},
        this.stripeOccEndpointService.cartOptions(cartId)
      )
    );
  }

  finalizeCheckoutSession(
    sessionId: string,
    cartId?: string,
    orderCode?: string
  ) {
    return this.stripeOccEndpointService.withCurrentContext((context) =>
      this.http.post<Order>(
        this.stripeOccEndpointService.userUrl(
          context,
          `stripe/checkout/session/${sessionId}/finalize`
        ),
        {},
        this.stripeOccEndpointService.orderOptions(cartId, orderCode)
      )
    );
  }

  async setCartPaymentOption(
    paymentOptionId: string,
    cartId?: string
  ): Promise<void> {
    await firstValueFrom(
      this.stripeOccEndpointService.withCurrentContext((context) =>
        this.http.put(
          this.stripeOccEndpointService.cartPaymentOptionUrl(context, cartId),
          <SapPaymentOptionRequest>{
            sapPaymentOptionId: paymentOptionId,
          }
        )
      )
    );
  }

  async pollCheckoutSession(
    sessionId: string,
    options: StripeCheckoutSessionPollingOptions = {}
  ): Promise<StripeCheckoutSession> {
    const {
      attempts = 5,
      intervalMs = 1500,
      cartId,
      orderCode,
    } = options;
    let latestSession: StripeCheckoutSession | undefined;

    for (let attempt = 0; attempt < attempts; attempt += 1) {
      latestSession = await firstValueFrom(
        this.getCheckoutSession(sessionId, cartId, orderCode)
      );
      if (this.isTerminalState(latestSession)) {
        return latestSession;
      }

      if (attempt < attempts - 1) {
        await this.sleep(intervalMs);
      }
    }

    if (!latestSession) {
      throw new Error('Stripe Checkout Session could not be retrieved.');
    }

    return latestSession;
  }

  protected isTerminalState(session: StripeCheckoutSession): boolean {
    return (
      session.paymentStatus === 'paid' ||
      session.paymentStatus === 'unpaid' ||
      session.status === 'complete' ||
      session.status === 'expired'
    );
  }

  protected sleep(intervalMs: number): Promise<void> {
    return new Promise((resolve) => {
      window.setTimeout(resolve, intervalMs);
    });
  }
}
