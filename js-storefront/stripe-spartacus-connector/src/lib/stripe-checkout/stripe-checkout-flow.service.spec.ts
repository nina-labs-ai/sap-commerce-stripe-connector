import { HttpErrorResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { StripeCheckoutFlowService } from './stripe-checkout-flow.service';

describe('StripeCheckoutFlowService', () => {
  let activeCartFacade: jasmine.SpyObj<any>;
  let multiCartFacade: jasmine.SpyObj<any>;
  let userIdService: jasmine.SpyObj<any>;
  let stripeCheckoutService: jasmine.SpyObj<any>;
  let store: jasmine.SpyObj<any>;
  let orderFacade: jasmine.SpyObj<any>;
  let routingService: jasmine.SpyObj<any>;
  let service: StripeCheckoutFlowService;

  beforeEach(() => {
    activeCartFacade = jasmine.createSpyObj('ActiveCartFacade', [
      'getActive',
      'requireLoadedCart',
    ]);
    multiCartFacade = jasmine.createSpyObj('MultiCartFacade', [
      'getCart',
      'loadCart',
      'removeCart',
    ]);
    userIdService = jasmine.createSpyObj('UserIdService', ['getUserId']);
    stripeCheckoutService = jasmine.createSpyObj('StripeCheckoutService', [
      'setCartPaymentOption',
    ]);
    store = jasmine.createSpyObj('Store', ['dispatch']);
    orderFacade = jasmine.createSpyObj('OrderFacade', ['setPlacedOrder']);
    routingService = jasmine.createSpyObj('RoutingService', ['go']);

    activeCartFacade.getActive.and.returnValue(of({ guid: 'cart-guid' }));
    activeCartFacade.requireLoadedCart.and.returnValue(of({ code: 'current' }));
    multiCartFacade.getCart.and.returnValue(of({ guid: 'restored-guid' }));
    userIdService.getUserId.and.returnValue(of('anonymous'));
    stripeCheckoutService.setCartPaymentOption.and.resolveTo(undefined);

    service = new StripeCheckoutFlowService(
      activeCartFacade,
      multiCartFacade,
      userIdService,
      stripeCheckoutService,
      store,
      orderFacade,
      routingService
    );
  });

  it('resolves the active cart identifier from guid before code', async () => {
    await expectAsync(service.getActiveCartIdentifier()).toBeResolvedTo(
      'cart-guid'
    );
  });

  it('restores a returned anonymous cart before finalization lookups', async () => {
    await service.restoreCartContext('cart-guid');

    expect(store.dispatch).toHaveBeenCalled();
    expect(multiCartFacade.loadCart).toHaveBeenCalledWith({
      userId: 'anonymous',
      cartId: 'cart-guid',
    });
    expect(multiCartFacade.getCart).toHaveBeenCalledWith('cart-guid');
  });

  it('ignores unsupported payment-option assignment responses only', async () => {
    stripeCheckoutService.setCartPaymentOption.and.rejectWith(
      new HttpErrorResponse({ status: 403 })
    );

    await expectAsync(
      service.assignPaymentOptionIfSupported('stripe', 'cart-guid')
    ).toBeResolved();

    const serverError = new HttpErrorResponse({ status: 500 });
    stripeCheckoutService.setCartPaymentOption.and.rejectWith(serverError);

    await expectAsync(
      service.assignPaymentOptionIfSupported('stripe', 'cart-guid')
    ).toBeRejectedWith(serverError);
  });

  it('centralizes placed-order state cleanup and confirmation routing', () => {
    const order = { code: 'order-code' } as any;

    service.completeOrder(order, 'cart-guid');

    expect(orderFacade.setPlacedOrder).toHaveBeenCalledWith(order);
    expect(multiCartFacade.removeCart).toHaveBeenCalledWith('cart-guid');
    expect(store.dispatch).toHaveBeenCalled();
    expect(routingService.go).toHaveBeenCalledWith({
      cxRoute: 'orderConfirmation',
    });
  });
});
