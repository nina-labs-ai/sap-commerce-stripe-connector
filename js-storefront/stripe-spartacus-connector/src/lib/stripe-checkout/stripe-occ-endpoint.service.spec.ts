import { firstValueFrom, of } from 'rxjs';
import { StripeOccEndpointService } from './stripe-occ-endpoint.service';

describe('StripeOccEndpointService', () => {
  let service: StripeOccEndpointService;

  beforeEach(() => {
    service = new StripeOccEndpointService(
      { getActive: () => of('apparel-uk-spa') } as any,
      { getUserId: () => of('anonymous') } as any
    );
  });

  it('resolves the active OCC storefront context', async () => {
    const context = await firstValueFrom(
      service.withCurrentContext((resolvedContext) => of(resolvedContext))
    );

    expect(context).toEqual({
      baseSiteId: 'apparel-uk-spa',
      userId: 'anonymous',
    });
  });

  it('builds Stripe user URLs and anonymous cart payment-option URLs', () => {
    const context = { baseSiteId: 'apparel-uk-spa', userId: 'anonymous' };

    expect(service.userUrl(context, 'stripe/checkout/session')).toBe(
      '/occ/v2/apparel-uk-spa/users/anonymous/stripe/checkout/session'
    );
    expect(service.cartPaymentOptionUrl(context, 'cart-guid')).toBe(
      '/occ/v2/apparel-uk-spa/users/anonymous/carts/cart-guid/paymentOption'
    );
  });

  it('keeps request params explicit and omits empty optional values', () => {
    expect(service.cartOptions('cart-guid')).toEqual({
      params: { cartId: 'cart-guid' },
    });
    expect(service.contextOptions(undefined, 'order-code')).toEqual({
      params: { orderCode: 'order-code' },
    });
    expect(service.orderOptions('cart-guid')).toEqual({
      params: { fields: 'FULL', cartId: 'cart-guid' },
    });
    expect(service.cartOptions()).toEqual({});
  });
});
