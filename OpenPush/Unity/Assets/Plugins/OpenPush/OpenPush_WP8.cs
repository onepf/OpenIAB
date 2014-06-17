#if UNITY_WP8
namespace OnePF.OpenPush
{
    public class OpenPush_WP8 : IOpenPush
    {
        public event OpenPush.InitFinishedDelegate InitFinished;

        public void Init(InitParams p)
        {
            WP8.PushClient.InitFinished += PushClient_InitFinished;
            WP8.PushClient.Init(p.ServerUrl);
        }

        void PushClient_InitFinished(bool success, string errorMessage)
        {
            if (InitFinished != null)
                InitFinished(success, errorMessage);
        }
    }
}
#endif