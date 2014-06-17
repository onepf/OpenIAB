#if UNITY_IOS
using UnityEngine;

namespace OnePF.OpenPush
{
    public class OpenPush_iOS : IOpenPush
    {
        public event OpenPush.InitFinishedDelegate InitFinished;

		OpenPush_iOS_helper _helper;

        public void Init(InitParams p)
        {
			_helper = new GameObject(typeof(OpenPush_iOS_helper).Name).AddComponent<OpenPush_iOS_helper>();
            _helper.InitFinished += Helper_InitFinished;
			_helper.Init(p.ServerUrl);
        }

        void Helper_InitFinished(bool success, string errorMessage)
        {
            if (InitFinished != null)
                InitFinished(success, errorMessage);
        }
    }
}
#endif