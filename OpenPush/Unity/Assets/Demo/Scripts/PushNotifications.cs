using UnityEngine;
using System.Collections;
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
        InitParams initParams = new InitParams("http://192.168.2.139:8080", "363551033620");
        OpenPush.Init(initParams);
    }

    void OpenPush_InitFinished(bool success, string errorMessage)
    {
        Debug.Log(string.Format("Init Finished: {0}; {1}", success, errorMessage));
    }
}
