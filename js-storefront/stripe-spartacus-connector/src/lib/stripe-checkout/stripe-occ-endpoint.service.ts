import { Injectable } from '@angular/core';
import { BaseSiteService, UserIdService } from '@spartacus/core';
import { Observable } from 'rxjs';
import { filter, switchMap, take } from 'rxjs/operators';

export interface StripeOccContext {
  baseSiteId: string;
  userId: string;
}

export interface StripeOccRequestOptions {
  params?: Record<string, string>;
}

export interface StripeOccOrderRequestOptions {
  params: Record<string, string>;
}

@Injectable({
  providedIn: 'root',
})
export class StripeOccEndpointService {
  constructor(
    private readonly baseSiteService: BaseSiteService,
    private readonly userIdService: UserIdService
  ) {}

  withCurrentContext<T>(
    callback: (context: StripeOccContext) => Observable<T>
  ): Observable<T> {
    return this.baseSiteService.getActive().pipe(
      filter((baseSiteId): baseSiteId is string => Boolean(baseSiteId)),
      switchMap((baseSiteId) =>
        this.userIdService.getUserId().pipe(
          filter((userId): userId is string => Boolean(userId)),
          take(1),
          switchMap((userId) => callback({ baseSiteId, userId }))
        )
      ),
      take(1)
    );
  }

  userUrl(context: StripeOccContext, path: string): string {
    return `/occ/v2/${context.baseSiteId}/users/${context.userId}/${path}`;
  }

  cartPaymentOptionUrl(context: StripeOccContext, cartId?: string): string {
    if (context.userId === 'anonymous' && cartId) {
      return this.userUrl(context, `carts/${cartId}/paymentOption`);
    }

    return this.userUrl(context, 'carts/current/paymentOption');
  }

  cartOptions(cartId?: string): StripeOccRequestOptions {
    return this.params({ cartId });
  }

  contextOptions(
    cartId?: string,
    orderCode?: string
  ): StripeOccRequestOptions {
    return this.params({ cartId, orderCode });
  }

  orderOptions(
    cartId?: string,
    orderCode?: string
  ): StripeOccOrderRequestOptions {
    return {
      params: {
        fields: 'FULL',
        ...(cartId ? { cartId } : {}),
        ...(orderCode ? { orderCode } : {}),
      },
    };
  }

  private params(
    values: Record<string, string | undefined>
  ): StripeOccRequestOptions {
    const params = Object.entries(values).reduce<Record<string, string>>(
      (result, [key, value]) => {
        if (value) {
          result[key] = value;
        }
        return result;
      },
      {}
    );

    return Object.keys(params).length > 0 ? { params } : {};
  }
}
