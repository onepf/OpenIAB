using UnityEngine;
using System.Collections;
using System.Collections.Generic;
using System;

namespace OnePF {
	public class OpenIAB_Android 
#if UNITY_ANDROID	
	: IOpenIAB 
#endif
	{
		public static readonly string STORE_GOOGLE;
		public static readonly string STORE_AMAZON;
		public static readonly string STORE_TSTORE;
		public static readonly string STORE_SAMSUNG;
		public static readonly string STORE_YANDEX;

#if UNITY_ANDROID
		private static AndroidJavaObject _plugin;
		
		static OpenIAB_Android() {
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
	        }
		}
		
		private bool IsDevice() {
			if (Application.platform != RuntimePlatform.Android) {
	            //OpenIAB.EventManager.SendMessage("OnBillingNotSupported", "editor mode");
	            return false;
	        }
			return true;
		}
		
		public void init(Dictionary<string, string> storeKeys=null) {
			if (!IsDevice()) return;
			
	        using (AndroidJavaObject obj_HashMap = new AndroidJavaObject("java.util.HashMap")) {
	            IntPtr method_Put = AndroidJNIHelper.GetMethodID(obj_HashMap.GetRawClass(), "put",
	                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
	
				if (storeKeys != null) {
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
				}
	            _plugin.Call("init", obj_HashMap);
	        }
		}
		
        public void mapSku(string sku, string storeName, string storeSku) {
            if (IsDevice()) {
	            _plugin.Call("mapSku", sku, storeName, storeSku);
			}
        }
		
        public void unbindService() {
            if (IsDevice()) {
            	_plugin.Call("unbindService");
			}
        }
		
        public bool areSubscriptionsSupported() {
            if (!IsDevice()) {
				// Fake result for editor mode
                return true;
            }
            return _plugin.Call<bool>("areSubscriptionsSupported");
        }
		
        public void queryInventory() {
            if (!IsDevice()) {
				return;
			}
            _plugin.Call("queryInventory");
        }
		
        // TODO: implement on java side. does nothing for now
        // Sends a request to get all completed purchases and product information
        public void queryInventory(string[] skus) {
            if (!IsDevice()) {              
                return;
            }
            IntPtr jArrayPtr = AndroidJNIHelper.ConvertToJNIArray(skus);
            jvalue[] jArray = new jvalue[1];
            jArray[0].l = jArrayPtr;
            IntPtr methodId = AndroidJNIHelper.GetMethodID(_plugin.GetRawClass(), "queryInventory");
            AndroidJNI.CallVoidMethod(_plugin.GetRawObject(), methodId, jArray);
        }
		
        public void purchaseProduct(string sku, string developerPayload="") {
            if (!IsDevice()) {
				// Fake purchase in editor mode
                OpenIAB.EventManager.SendMessage("OnPurchaseSucceeded", Purchase.CreateFromSku(sku, developerPayload).Serialize());
                return;
            }
            _plugin.Call("purchaseProduct", sku, developerPayload);
        }
		
        public void purchaseSubscription(string sku, string developerPayload="") {
            if (!IsDevice()) {
				// Fake purchase in editor mode
                OpenIAB.EventManager.SendMessage("OnPurchaseSucceeded", Purchase.CreateFromSku(sku, developerPayload).Serialize());
                return;
            }
            _plugin.Call("purchaseSubscription", sku, developerPayload);
        }
		
        public void consumeProduct(Purchase purchase) {
            if (!IsDevice()) {
				// Fake consume in editor mode
                OpenIAB.EventManager.SendMessage("OnConsumePurchaseSucceeded", purchase.Serialize());
                return;
            }
            _plugin.Call("consumeProduct", purchase.Serialize());
        }
		
		public void restoreTransactions() {
		}
#else
		static OpenIAB_Android() {
            STORE_GOOGLE = "STORE_GOOGLE";
            STORE_AMAZON = "STORE_AMAZON";
            STORE_TSTORE = "STORE_TSTORE";
            STORE_SAMSUNG = "STORE_SAMSUNG";
            STORE_YANDEX = "STORE_YANDEX";
		}
#endif
	}
}