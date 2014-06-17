namespace OnePF.OpenPush
{
    public class InitParams
    {
        /// <summary>
        /// URL of the custom server, which accepts device registration info
        /// </summary>
        public string ServerUrl { get; private set; }

        /// <summary>
        /// Android project ID signed up at Google API console
        /// </summary>
        public string SenderId { get; private set; }

        public InitParams(string serverUrl, string senderId)
        {
            ServerUrl = serverUrl;
            SenderId = senderId;
        }
    }
}
