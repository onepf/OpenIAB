using System.Collections.Generic;

namespace OnePF {

    /**
     * All options of OpenIAB can be found here
     * 
     * TODO: consider to use cloned instance of Options in OpenIABHelper   
     */
    public class Options {
        
        /** */
        private const int DISCOVER_TIMEOUT_MS = 5000;
    
        /** 
         * for generic stores it takes 1.5 - 3sec
         * <p>
         * SamsungApps initialization is very time consuming (from 4 to 12 seconds). 
         * TODO: Optimize: ~1sec is consumed for check account certification via account activity + ~3sec for actual setup
         */
        private const int INVENTORY_CHECK_TIMEOUT_MS = 10000;

        /** 
         * List of stores to be used for store elections. By default GooglePlay, Amazon, SamsungApps and 
         * all installed OpenStores are used.
         * <p>
         * To specify your own list, you need to instantiate Appstore object manually.
         * GooglePlay, Amazon and SamsungApps could be instantiated directly. OpenStore can be discovered 
         * using {@link OpenIabHelper#discoverOpenStores(Context, List, Options)}
         * <p>
         * If you put only your instance of Appstore in this list OpenIAB will use it
         */
        // TODO: it can be configured only on the java side
        //public List<Appstore> availableStores;
        
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
        public string[] prefferedStoreNames = new string[]{};
        
        /** Used for SamsungApps setup. Specify your own value if default one interfere your code.
         * <p>default value is {@link SamsungAppsBillingService#REQUEST_CODE_IS_ACCOUNT_CERTIFICATION} */
        // TODO: not needed on the Unity side
        //public int samsungCertificationRequestCode = SamsungAppsBillingService.REQUEST_CODE_IS_ACCOUNT_CERTIFICATION;
    }
}