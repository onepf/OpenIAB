#import "AppStoreDelegate.h"

// Unity to NS String conversion
NSString* ToString(const char* c_string)
{
    return c_string == NULL ? [NSString stringWithUTF8String:""] : [NSString stringWithUTF8String:c_string];
}

extern "C"
{
    bool AppStore_canMakePayments()
    {
        return [[AppStoreDelegate instance] canMakePayments] != 0;
    }
    
    void AppStore_requestProducts(const char* skus[], int skuNumber)
    {
        NSMutableSet *skuSet = [NSMutableSet set];
        for (int i = 0; i < skuNumber; ++i)
            [skuSet addObject: ToString(skus[i])];
        [[AppStoreDelegate instance] requestProducts:skuSet];
    }
    
    void AppStore_startPurchase(const char* sku)
    {
        [[AppStoreDelegate instance] startPurchase:ToString(sku)];
    }
    
    void AppStore_restorePurchases()
    {
        [[AppStoreDelegate instance] restorePurchases];
    }
    
    void Inventory_query()
    {
        [[AppStoreDelegate instance] queryInventory];
    }
    
    bool Inventory_hasPurchase(const char* sku)
    {
        NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
        if (standardUserDefaults)
        {
            return [standardUserDefaults boolForKey:ToString(sku)];
        }
        else
        {
            NSLog(@"Couldn't access purchase storage.");
            return false;
        }
    }
    
    void Inventory_removePurchase(const char* sku)
    {
        NSUserDefaults *standardUserDefaults = [NSUserDefaults standardUserDefaults];
        if (standardUserDefaults)
        {
            [standardUserDefaults removeObjectForKey:ToString(sku)];
            [standardUserDefaults synchronize];
        }
        else
        {
            NSLog(@"Couldn't access standardUserDefaults. Purchase wasn't removed.");
        }
    }
}