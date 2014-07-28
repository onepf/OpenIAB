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

using System.Collections.Generic;

namespace OnePF
{
    /**
     * All options of OpenIAB can be found here
     */
    public class Options
    {
        /**
         * Default timeout (in milliseconds) for discover all OpenStores on device.
         */
        private const int DISCOVER_TIMEOUT_MS = 5000;

        /** 
         * For generic stores it takes 1.5 - 3sec
         * SamsungApps initialization is very time consuming (from 4 to 12 seconds). 
         */
        private const int INVENTORY_CHECK_TIMEOUT_MS = 10000;

        /**
         * Wait specified amount of ms to find all OpenStores on device
         */
        public int discoveryTimeoutMs = DISCOVER_TIMEOUT_MS;

        /** 
         * Check user inventory in every store to select proper store
         * <p>
         * Will try to connect to each billingService and extract user's purchases.
         * If purchases have been found in the only store that store will be used for further purchases. 
         * If purchases have been found in multiple stores only such stores will be used for further elections    
         */
        public bool checkInventory = true;

        /**
         * Wait specified amount of ms to check inventory in all stores
         */
        public int checkInventoryTimeoutMs = INVENTORY_CHECK_TIMEOUT_MS;

        /** 
         * OpenIAB could skip receipt verification by publicKey for GooglePlay and OpenStores 
         * <p>
         * Receipt could be verified in {@link OnIabPurchaseFinishedListener#onIabPurchaseFinished()}
         * using {@link Purchase#getOriginalJson()} and {@link Purchase#getSignature()}
         */
        public OptionsVerifyMode verifyMode = OptionsVerifyMode.VERIFY_EVERYTHING;

        /** 
         * storeKeys is map of [ appstore name -> publicKeyBase64 ] 
         * Put keys for all stores you support in this Map and pass it to instantiate {@link OpenIabHelper} 
         * <p>
         * <b>publicKey</b> key is used to verify receipt is created by genuine Appstore using 
         * provided signature. It can be found in Developer Console of particular store
         * <p>
         * <b>name</b> of particular store can be provided by local_store tool if you run it on device.
         * For Google Play OpenIAB uses {@link OpenIabHelper#NAME_GOOGLE}.
         * <p>
         * <p>Note:
         * AmazonApps and SamsungApps doesn't use RSA keys for receipt verification, so you don't need 
         * to specify it
         */
        public Dictionary<string, string> storeKeys = new Dictionary<string, string>();

        /**
         * Used as priority list if store that installed app is not found and there are 
         * multiple stores installed on device that supports billing.
         */
        public string[] prefferedStoreNames = new string[] { };
    }
}