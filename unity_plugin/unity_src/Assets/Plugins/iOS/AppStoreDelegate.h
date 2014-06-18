#import <StoreKit/StoreKit.h>
#import <Foundation/Foundation.h>

@interface AppStoreDelegate : NSObject <SKPaymentTransactionObserver, SKProductsRequestDelegate>

+ (AppStoreDelegate*)instance;

- (BOOL)canMakePayments;

- (void)requestProducts:(NSSet*)skus;

- (void)startPurchase:(NSString*)sku;

- (void)queryInventory;

- (void)restorePurchases;

@end
