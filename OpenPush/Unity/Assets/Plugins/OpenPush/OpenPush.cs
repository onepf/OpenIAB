using UnityEngine;
using System.Collections;

namespace OnePF.OpenPush
{
    public static class OpenPush
    {
        public delegate void InitFinishedDelegate(bool success, string errorMessage);
        public static event InitFinishedDelegate InitFinished;

        static IOpenPush _push = null;

        static EventReceiver _eventReceiver = null;

        static OpenPush()
        {
#if UNITY_ANDROID
			_push = new OpenPush_Android();
#elif UNITY_IOS
            var push = new OpenPush_iOS();
            push.InitFinished += delegate(bool success, string errorMessage) { if (InitFinished != null) InitFinished(success, errorMessage); };
            _push = push;
#elif UNITY_WP8
            var push = new OpenPush_WP8();
            push.InitFinished += delegate(bool success, string errorMessage) { if (InitFinished != null) InitFinished(success, errorMessage); };
            _push = push;            
#else
			Debug.LogError("OpenPush currently not supported on this platform. Sorry.");
            return;
#endif
        }

        public static void Init(InitParams initParams)
        {
            if (_eventReceiver == null)
            {
                _eventReceiver = new GameObject("OpenPush").AddComponent<EventReceiver>();
                
                _eventReceiver.InitSucceded += delegate() 
                {
                    if (InitFinished != null)
                        InitFinished(true, "");
                };
                
                _eventReceiver.InitFailed += delegate(string error)
                {
                    if (InitFinished != null)
                        InitFinished(false, error);
                };
            }

            _push.Init(initParams);
        }
    }
}