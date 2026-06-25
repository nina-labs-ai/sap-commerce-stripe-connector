import { HttpErrorResponse } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Store } from '@ngrx/store';
import { ActiveCartFacade, MultiCartFacade } from '@spartacus/cart/base/root';
import { CartActions } from '@spartacus/cart/base/core';
import { RoutingService, UserIdService } from '@spartacus/core';
import { Order, OrderFacade } from '@spartacus/order/root';
import { firstValueFrom } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { StripeCheckoutService } from './stripe-checkout.service';

type CartIdentifier = { guid?: string; code?: string };

@Injectable({
  providedIn: 'root',
})
export class StripeCheckoutFlowService {
  readonly cart$ = this.activeCartFacade.getActive();

  constructor(
    private readonly activeCartFacade: ActiveCartFacade,
    private readonly multiCartFacade: MultiCartFacade,
    private readonly userIdService: UserIdService,
    private readonly stripeCheckoutService: StripeCheckoutService,
    private readonly store: Store,
    private readonly orderFacade: OrderFacade,
    private readonly routingService: RoutingService
  ) {}

  async getActiveCartIdentifier(): Promise<string> {
    const cart = await firstValueFrom(
      this.activeCartFacade.getActive().pipe(
        filter((activeCart): activeCart is CartIdentifier =>
          Boolean(activeCart?.guid || activeCart?.code)
        ),
        take(1)
      )
    );

    return cart.guid ?? cart.code!;
  }

  async restoreCartContext(cartId?: string): Promise<void> {
    if (!cartId) {
      await firstValueFrom(this.activeCartFacade.requireLoadedCart());
      return;
    }

    const userId = await firstValueFrom(
      this.userIdService.getUserId().pipe(
        filter((value): value is string => Boolean(value)),
        take(1)
      )
    );

    this.store.dispatch(new CartActions.SetActiveCartId(cartId));
    this.multiCartFacade.loadCart({ userId, cartId });

    await firstValueFrom(
      this.multiCartFacade.getCart(cartId).pipe(
        filter((cart): cart is CartIdentifier =>
          Boolean(cart?.guid || cart?.code)
        ),
        take(1)
      )
    );
  }

  async assignPaymentOptionIfSupported(
    paymentOptionId: string,
    cartId?: string
  ): Promise<void> {
    try {
      await this.stripeCheckoutService.setCartPaymentOption(
        paymentOptionId,
        cartId
      );
    } catch (error) {
      if (!this.isIgnorablePaymentOptionError(error)) {
        throw error;
      }
    }
  }

  completeOrder(order: Order, cartId?: string): void {
    this.orderFacade.setPlacedOrder(order);
    if (cartId) {
      this.multiCartFacade.removeCart(cartId);
    }
    this.store.dispatch(new CartActions.SetActiveCartId(''));
    this.routingService.go({ cxRoute: 'orderConfirmation' });
  }

  resolveErrorMessage(error: unknown, fallbackMessage: string): string {
    return error instanceof Error && error.message
      ? error.message
      : fallbackMessage;
  }

  private isIgnorablePaymentOptionError(error: unknown): boolean {
    return (
      error instanceof HttpErrorResponse &&
      [400, 401, 403].includes(error.status)
    );
  }
}
