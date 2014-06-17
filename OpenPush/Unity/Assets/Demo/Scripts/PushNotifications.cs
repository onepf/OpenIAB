using UnityEngine;
using OnePF.OpenPush;

public class PushNotifications : MonoBehaviour
{
    void OnEnable()
    {
        OpenPush.InitFinished += OpenPush_InitFinished;
    }

    void OnDisable()
    {
        OpenPush.InitFinished -= OpenPush_InitFinished;
    }

    void Start()
    {
        InitParams initParams = new InitParams("http://custom-server-url", "ANDROID_SENDER_ID");
        OpenPush.Init(initParams);
    }

    void OpenPush_InitFinished(bool success, string errorMessage)
    {
        Debug.Log(string.Format("Init Finished: {0}; {1}", success, errorMessage));
    }
}
