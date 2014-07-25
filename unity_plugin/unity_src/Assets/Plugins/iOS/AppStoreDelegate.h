#import <StoreKit/StoreKit.h>
#import <Foundation/Foundation.h>

@interface AppStoreDelegate : NSObject <SKPaymentTransactionObserver, SKProductsRequestDelegate>

+ (AppStoreDelegate*)instance;

- (void)requestSKUs:(NSSet*)skus;

- (void)startPurchase:(NSString*)sku;

- (void)queryInventory;

- (void)restorePurchases;

@end
