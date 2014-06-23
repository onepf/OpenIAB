using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System;

namespace OnePF
{
    public class OpenIAB_Android
#if UNITY_ANDROID
 : IOpenIAB
#endif
    {
        public static readonly string STORE_GOOGLE;
        public static readonly string STORE_AMAZON;
        public static readonly string STORE_SAMSUNG;
        public static readonly string STORE_NOKIA;
        public static readonly string STORE_YANDEX;

#if UNITY_ANDROID
        private static AndroidJavaObject _plugin;

        static OpenIAB_Android()
        {
            if (Application.platform != RuntimePlatform.Android)
            {
                STORE_GOOGLE = "STORE_GOOGLE";
                STORE_AMAZON = "STORE_AMAZON";
                STORE_SAMSUNG = "STORE_SAMSUNG";
                STORE_NOKIA = "STORE_NOKIA";
                STORE_YANDEX = "STORE_YANDEX";
                return;
            }

            AndroidJNI.AttachCurrentThread();

            // Find the plugin instance
            using (var pluginClass = new AndroidJavaClass("org.onepf.openiab.UnityPlugin"))
            {
                _plugin = pluginClass.CallStatic<AndroidJavaObject>("instance");
                STORE_GOOGLE = pluginClass.GetStatic<string>("STORE_GOOGLE");
                STORE_AMAZON = pluginClass.GetStatic<string>("STORE_AMAZON");
                STORE_SAMSUNG = pluginClass.GetStatic<string>("STORE_SAMSUNG");
                STORE_NOKIA = pluginClass.GetStatic<string>("STORE_NOKIA");
                STORE_YANDEX = pluginClass.GetStatic<string>("STORE_YANDEX");
            }
        }

        private bool IsDevice()
        {
            if (Application.platform != RuntimePlatform.Android)
            {
                //OpenIAB.EventManager.SendMessage("OnBillingNotSupported", "editor mode");
                return false;
            }
            return true;
        }

        private AndroidJavaObject CreateJavaHashMap(Dictionary<string, string> storeKeys)
        {
            var j_HashMap = new AndroidJavaObject("java.util.HashMap");
            IntPtr method_Put = AndroidJNIHelper.GetMethodID(j_HashMap.GetRawClass(), "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");

            if (storeKeys != null)
            {
                object[] args = new object[2];
                foreach (KeyValuePair<string, string> kvp in storeKeys)
                {
                    using (AndroidJavaObject k = new AndroidJavaObject("java.lang.String", kvp.Key))
                    {
                        using (AndroidJavaObject v = new AndroidJavaObject("java.lang.String", kvp.Value))
                        {
                            args[0] = k;
                            args[1] = v;
                            AndroidJNI.CallObjectMethod(j_HashMap.GetRawObject(),
                                method_Put, AndroidJNIHelper.CreateJNIArgArray(args));
                        }
                    }
                }
            }
            return j_HashMap;
        }

        public void init(Options options)
        {
            if (!IsDevice())
            {
                // Fake init process in the editor. For test purposes
                OpenIAB.EventManager.SendMessage("OnBillingSupported", "");
                return;
            }

            using (var j_options = new AndroidJavaObject("org.onepf.oms.OpenIabHelper$Options"))
            {
                j_options.Set<int>("discoveryTimeoutMs", options.discoveryTimeoutMs);
                j_options.Set<bool>("checkInventory", options.checkInventory);
                j_options.Set<int>("checkInventoryTimeoutMs", options.checkInventoryTimeoutMs);
                j_options.Set<int>("verifyMode", (int) options.verifyMode);

                AndroidJavaObject j_storeKeys = CreateJavaHashMap(options.storeKeys);
                j_options.Set("storeKeys", j_storeKeys);
                j_storeKeys.Dispose();

                j_options.Set("prefferedStoreNames", AndroidJNIHelper.ConvertToJNIArray(options.prefferedStoreNames));

                _plugin.Call("initWithOptions", j_options);
            }
        }

        public void init(Dictionary<string, string> storeKeys=null)
        {
            if (!IsDevice()) return;

            if (storeKeys != null)
            {
                AndroidJavaObject j_storeKeys = CreateJavaHashMap(storeKeys);
                _plugin.Call("init", j_storeKeys);
                j_storeKeys.Dispose();
            }
        }

        public void mapSku(string sku, string storeName, string storeSku)
        {
            if (IsDevice())
            {
                _plugin.Call("mapSku", sku, storeName, storeSku);
            }
        }

        public void unbindService()
        {
            if (IsDevice())
            {
                _plugin.Call("unbindService");
            }
        }

        public bool areSubscriptionsSupported()
        {
            if (!IsDevice())
            {
                // Fake result for editor mode
                return true;
            }
            return _plugin.Call<bool>("areSubscriptionsSupported");
        }

        public void queryInventory()
        {
            if (!IsDevice())
            {
                return;
            }
            IntPtr methodId = AndroidJNI.GetMethodID(_plugin.GetRawClass(), "queryInventory", "()V");
            AndroidJNI.CallVoidMethod(_plugin.GetRawObject(), methodId, new jvalue[] { });
        }

        public void queryInventory(string[] skus)
        {
            queryInventory(skus, skus);
        }

        private void queryInventory(string[] inAppSkus, string[] subsSkus)
        {
            if (!IsDevice())
            {
                return;
            }
            jvalue[] args = AndroidJNIHelper.CreateJNIArgArray(new object[] { inAppSkus, subsSkus });
            IntPtr methodId = AndroidJNI.GetMethodID(_plugin.GetRawClass(), "queryInventory", "([Ljava/lang/String;[Ljava/lang/String;)V");
            AndroidJNI.CallVoidMethod(_plugin.GetRawObject(), methodId, args);
        }

        public void purchaseProduct(string sku, string developerPayload="")
        {
            if (!IsDevice())
            {
                // Fake purchase in editor mode
                OpenIAB.EventManager.SendMessage("OnPurchaseSucceeded", Purchase.CreateFromSku(sku, developerPayload).Serialize());
                return;
            }
            _plugin.Call("purchaseProduct", sku, developerPayload);
        }

        public void purchaseSubscription(string sku, string developerPayload="")
        {
            if (!IsDevice())
            {
                // Fake purchase in editor mode
                OpenIAB.EventManager.SendMessage("OnPurchaseSucceeded", Purchase.CreateFromSku(sku, developerPayload).Serialize());
                return;
            }
            _plugin.Call("purchaseSubscription", sku, developerPayload);
        }

        public void consumeProduct(Purchase purchase)
        {
            if (!IsDevice())
            {
                // Fake consume in editor mode
                OpenIAB.EventManager.SendMessage("OnConsumePurchaseSucceeded", purchase.Serialize());
                return;
            }
            _plugin.Call("consumeProduct", purchase.Serialize());
        }

        public void restoreTransactions()
        {
        }

        public bool isDebugLog()
        {
            return _plugin.Call<bool>("isDebugLog");
        }

        public void enableDebugLogging(bool enabled)
        {
            _plugin.Call("enableDebugLogging", enabled);
        }

        public void enableDebugLogging(bool enabled, string tag)
        {
            _plugin.Call("enableDebugLogging", enabled, tag);
        }
#else
		static OpenIAB_Android() {
            STORE_GOOGLE = "STORE_GOOGLE";
            STORE_AMAZON = "STORE_AMAZON";
            STORE_SAMSUNG = "STORE_SAMSUNG";
            STORE_NOKIA = "STORE_NOKIA";
            STORE_YANDEX = "STORE_YANDEX";
		}
#endif
    }
}