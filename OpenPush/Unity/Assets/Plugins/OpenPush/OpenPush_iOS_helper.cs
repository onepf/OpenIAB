using UnityEngine;
using System;
using System.Collections;

namespace OnePF.OpenPush
{
	public class OpenPush_iOS_helper : MonoBehaviour 
	{
        public event OpenPush.InitFinishedDelegate InitFinished;

		string _serverUrl = null;
		string _token = null;

		public void Init(string serverUrl)
		{
			_serverUrl = serverUrl;
			StartCoroutine(RequestToken(serverUrl));
		}

		void Register()
		{
			string postData = "{\"platform\":\"ios\",\"token\":\"" + _token + "\"}";
			Byte[] byteArray = System.Text.Encoding.UTF8.GetBytes(postData);
			StartCoroutine(WaitForRequest(new WWW(_serverUrl, byteArray)));
		}

		IEnumerator WaitForRequest(WWW www)
		{
			yield return www;
			
			// check for errors
			if (www.error == null)
			{
                if (InitFinished != null)
                    InitFinished(true, "");
			} else {
                if (InitFinished != null)
                    InitFinished(false, www.error);
			}    
		}

		IEnumerator RequestToken(string serverUrl)
		{
#if UNITY_IOS
			NotificationServices.RegisterForRemoteNotificationTypes(RemoteNotificationType.Alert |
			                                                        RemoteNotificationType.Badge |
			                                                        RemoteNotificationType.Sound);
			while (true)
			{
				byte[] token = NotificationServices.deviceToken;
				if (token != null)
				{
					_token = System.BitConverter.ToString(token).Replace("-", "");
					Debug.Log("Token received: " + _token);
					yield break;
				}
				yield return new WaitForFixedUpdate();
			}
#else
            yield return null;
#endif
		}
	}
}