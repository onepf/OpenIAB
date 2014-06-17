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
            if (!IsDevice())
            {
                Helper_InitFinished(true, "");
                return;
            }

			_helper = new GameObject(typeof(OpenPush_iOS_helper).Name).AddComponent<OpenPush_iOS_helper>();
            _helper.InitFinished += Helper_InitFinished;
			_helper.Init(p.ServerUrl);
        }

        private void Helper_InitFinished(bool success, string errorMessage)
        {
            if (InitFinished != null)
                InitFinished(success, errorMessage);
        }

        private static bool IsDevice()
        {
            return Application.platform == RuntimePlatform.IPhonePlayer;
        }
    }
}
#endif