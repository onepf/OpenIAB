using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System;
using OpenIabPlugin;

#if UNITY_ANDROID
namespace OpenIabPlugin {
    public class OpenIAB {
        private static AndroidJavaObject _plugin;

        public static readonly string STORE_GOOGLE;
        public static readonly string STORE_AMAZON;
        public static readonly string STORE_TSTORE;
        public static readonly string STORE_SAMSUNG;
        public static readonly string STORE_YANDEX;

        private static GameObject OpenIABEventManager { get { return GameObject.Find("OpenIABEventManager"); } }

        static OpenIAB() {
            if (Application.platform != RuntimePlatform.Android) {
                STORE_GOOGLE = "STORE_GOOGLE";
                STORE_AMAZON = "STORE_AMAZON";
                STORE_TSTORE = "STORE_TSTORE";
                STORE_SAMSUNG = "STORE_SAMSUNG";
                STORE_YANDEX = "STORE_YANDEX";
                return;
            }

            AndroidJNI.AttachCurrentThread();

            // Find the plugin instance
            using (var pluginClass = new AndroidJavaClass("com.openiab.OpenIAB")) {
                _plugin = pluginClass.CallStatic<AndroidJavaObject>("instance");
                STORE_GOOGLE = pluginClass.GetStatic<string>("STORE_GOOGLE");
                STORE_AMAZON = pluginClass.GetStatic<string>("STORE_AMAZON");
                STORE_TSTORE = pluginClass.GetStatic<string>("STORE_TSTORE");
                STORE_SAMSUNG = pluginClass.GetStatic<string>("STORE_SAMSUNG");
                STORE_YANDEX = pluginClass.GetStatic<string>("STORE_YANDEX");
                Debug.Log("********** OpenIAB plugin initialized **********");
            }
        }

        // Starts up the billing service. This will also check to see if in app billing is supported and fire the appropriate event
        public static void init(Dictionary<string, string> storeKeys) {
            if (Application.platform != RuntimePlatform.Android) {
                OpenIABEventManager.SendMessage("OnBillingNotSupported", "editor mode");
                return;
            }

            using (AndroidJavaObject obj_HashMap = new AndroidJavaObject("java.util.HashMap")) {
                IntPtr method_Put = AndroidJNIHelper.GetMethodID(obj_HashMap.GetRawClass(), "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

                object[] args = new object[2];
                foreach (KeyValuePair<string, string> kvp in storeKeys) {
                    using (AndroidJavaObject k = new AndroidJavaObject("java.lang.String", kvp.Key)) {
                        using (AndroidJavaObject v = new AndroidJavaObject("java.lang.String", kvp.Value)) {
                            args[0] = k;
                            args[1] = v;
                            AndroidJNI.CallObjectMethod(obj_HashMap.GetRawObject(),
                                method_Put, AndroidJNIHelper.CreateJNIArgArray(args));
                        }
                    }
                }
                _plugin.Call("init", obj_HashMap);
            }
        }

        public static void mapSku(string sku, string storeName, string storeSku) {
            if (Application.platform != RuntimePlatform.Android) {
                OpenIABEventManager.SendMessage("OnBillingNotSupported", "editor mode");
                return;
            }

            _plugin.Call("mapSku", sku, storeName, storeSku);
        }

        // Unbinds and shuts down the billing service
        public static void unbindService() {
            if (Application.platform != RuntimePlatform.Android) {
                return;
            }
            _plugin.Call("unbindService");
        }

        public static bool areSubscriptionsSupported() {
            return _plugin.Call<bool>("areSubscriptionsSupported");
        }

        // Sends a request to get all completed purchases and product information
        public static void queryInventory() {
            _plugin.Call("queryInventory");
        }

        // TODO: implement on java side. does nothing for now
        // Sends a request to get all completed purchases and product information
        public static void queryInventory(string[] skus) {
            if (Application.platform != RuntimePlatform.Android) {
                // TODO: implement editor purchase simulation
                return;
            }
            IntPtr jArrayPtr = AndroidJNIHelper.ConvertToJNIArray(skus);
            jvalue[] jArray = new jvalue[1];
            jArray[0].l = jArrayPtr;
            IntPtr methodId = AndroidJNIHelper.GetMethodID(_plugin.GetRawClass(), "queryInventory");
            AndroidJNI.CallVoidMethod(_plugin.GetRawObject(), methodId, jArray);
        }

        // Purchases the product with the given sku and developerPayload
        public static void purchaseProduct(string sku, string developerPayload="") {
            if (Application.platform != RuntimePlatform.Android) {
                OpenIABEventManager.SendMessage("OnPurchaseSucceeded", Purchase.CreateFromSku(sku, developerPayload).Serialize());
                return;
            }
            _plugin.Call("purchaseProduct", sku, developerPayload);
        }

        // Purchases the subscription with the given sku and developerPayload
        public static void purchaseSubscription(string sku, string developerPayload="") {
            if (Application.platform != RuntimePlatform.Android) {
                OpenIABEventManager.SendMessage("OnPurchaseSucceeded", Purchase.CreateFromSku(sku, developerPayload).Serialize());
                return;
            }
            _plugin.Call("purchaseSubscription", sku, developerPayload);
        }

        // Sends out a request to consume the product
        public static void consumeProduct(Purchase purchase) {
            if (Application.platform != RuntimePlatform.Android) {
                OpenIABEventManager.SendMessage("OnConsumePurchaseSucceeded", purchase.Serialize());
                return;
            }
            _plugin.Call("consumeProduct", purchase.Serialize());
        }
    }
}
#endif // UNITY_ANDROID
