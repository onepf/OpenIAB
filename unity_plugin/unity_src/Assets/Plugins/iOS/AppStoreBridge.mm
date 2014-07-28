/*******************************************************************************
 * Copyright 2012-2014 One Platform Foundation
 *
 *       Licensed under the Apache License, Version 2.0 (the "License");
 *       you may not use this file except in compliance with the License.
 *       You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *       Unless required by applicable law or agreed to in writing, software
 *       distributed under the License is distributed on an "AS IS" BASIS,
 *       WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *       See the License for the specific language governing permissions and
 *       limitations under the License.
 ******************************************************************************/

#import "AppStoreDelegate.h"

/**
 * Unity to NS String conversion
 * @param c_string original C string
 */ 
NSString* ToString(const char* c_string)
{
    return c_string == NULL ? [NSString stringWithUTF8String:""] : [NSString stringWithUTF8String:c_string];
}

extern "C"
{
    /**
     * Native 'requestProducts' wrapper
     * @param skus product IDs
     * @param skuNumber lenght of the 'skus' array
     */
    void AppStore_requestProducts(const char* skus[], int skuNumber)
    {
        NSMutableSet *skuSet = [NSMutableSet set];
        for (int i = 0; i < skuNumber; ++i)
            [skuSet addObject: ToString(skus[i])];
        [[AppStoreDelegate instance] requestSKUs:skuSet];
    }
    
    /**
     * Native 'startPurchase' wrapper
     * @param sku product ID
     */
    void AppStore_startPurchase(const char* sku)
    {
        [[AppStoreDelegate instance] startPurchase:ToString(sku)];
    }
    
    /**
     * Native 'restorePurchases' wrapper
     */
    void AppStore_restorePurchases()
    {
        [[AppStoreDelegate instance] restorePurchases];
    }
    
    /**
     * Query inventory
     * Restore purchased items
     */
    void Inventory_query()
    {
        [[AppStoreDelegate instance] queryInventory];
    }
    
    /**
     * Checks if product is purchased
     * @param sku product ID
     * @return true if product was purchased
     */
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
    
    /**
     * Delete purchase information
     * @param sku product ID
     */    
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