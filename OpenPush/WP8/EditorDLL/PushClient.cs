using System.Collections.Generic;
using System;

namespace OnePF.OpenPush.WP8
{
    public class PushClient
    {
        public static event Action<bool, string> InitFinished;

        public static void Init(string serverUrl)
        {
            if (InitFinished != null)
                InitFinished(true, "");
        }
    }
}

