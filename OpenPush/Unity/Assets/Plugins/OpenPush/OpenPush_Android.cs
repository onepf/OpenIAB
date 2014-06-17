#if UNITY_ANDROID
using UnityEngine;

namespace OnePF.OpenPush
{
    public class OpenPush_Android : IOpenPush
    {
        private static AndroidJavaObject _push;

        static OpenPush_Android()
        {
            if (Application.platform != RuntimePlatform.Android)
                return;

            AndroidJNI.AttachCurrentThread();

            // Get push client instance
            using (var pushClass = new AndroidJavaClass("org.onepf.openpush.PushClient"))
            {
                _push = pushClass.CallStatic<AndroidJavaObject>("instance");
            }
        }

        public void Init(InitParams p)
        {
            _push.Call("init", p.ServerUrl, p.SenderId);
        }
    }
}
#endif