using System;
using Windows.ApplicationModel.Store;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using Windows.Foundation;
using System.Windows.Threading;
using System.Windows;
using Microsoft.Phone.Notification;
using System.Text;
using System.Net;
using System.Collections.Specialized;
using System.IO;

namespace OnePF.OpenPush.WP8
{
    public class PushClient
    {
        public static event Action<bool, string> InitFinished;

        static string _serverUrl = "";
        
        /// Holds the push channel that is created or found.
        static HttpNotificationChannel _pushChannel;

        public static void Init(string serverUrl)
        {
            _serverUrl = serverUrl;

            // The name of our push channel.
            string channelName = "OpenPush";

            // Try to find the push channel.
            _pushChannel = HttpNotificationChannel.Find(channelName);

            // If the channel was not found, then create a new connection to the push service.
            if (_pushChannel == null)
            {
                _pushChannel = new HttpNotificationChannel(channelName);

                // Register for all the events before attempting to open the channel.
                _pushChannel.ChannelUriUpdated += new EventHandler<NotificationChannelUriEventArgs>(PushChannel_ChannelUriUpdated);
                _pushChannel.ErrorOccurred += new EventHandler<NotificationChannelErrorEventArgs>(PushChannel_ErrorOccurred);

                // Register for this notification only if you need to receive the notifications while your application is running.
                _pushChannel.ShellToastNotificationReceived += new EventHandler<NotificationEventArgs>(PushChannel_ShellToastNotificationReceived);

                _pushChannel.Open();

                // Bind this new channel for toast events.
                _pushChannel.BindToShellToast();

                Console.WriteLine(_pushChannel.ChannelUri);
            }
            else
            {
                // The channel was already open, so just register for all the events.
                _pushChannel.ChannelUriUpdated += new EventHandler<NotificationChannelUriEventArgs>(PushChannel_ChannelUriUpdated);
                _pushChannel.ErrorOccurred += new EventHandler<NotificationChannelErrorEventArgs>(PushChannel_ErrorOccurred);

                // Register for this notification only if you need to receive the notifications while your application is running.
                _pushChannel.ShellToastNotificationReceived += new EventHandler<NotificationEventArgs>(PushChannel_ShellToastNotificationReceived);
            }
        }

        static void Register()
        {
            HttpWebRequest request = WebRequest.CreateHttp(_serverUrl);
            request.ContentType = "text/plain; charset=utf-8";
            request.Method = "POST";

            request.BeginGetRequestStream((IAsyncResult result) =>
            {
                HttpWebRequest preq = result.AsyncState as HttpWebRequest;
                if (preq != null)
                {
                    Stream postStream = preq.EndGetRequestStream(result);

                    string postData = "{\"platform\":\"wp8\",\"token\":\"" + _pushChannel.ChannelUri.ToString() + "\"}";
                    Byte[] byteArray = Encoding.UTF8.GetBytes(postData);

                    postStream.Write(byteArray, 0, byteArray.Length);
                    postStream.Close();

                    // Finalize request
                    preq.BeginGetResponse((IAsyncResult finalResult) =>
                    {
                        HttpWebRequest req = finalResult.AsyncState as HttpWebRequest;
                        if (req != null)
                        {
                            try
                            {
                                WebResponse response = req.EndGetResponse(finalResult);
                            }
                            catch (WebException e)
                            {
                                Deployment.Current.Dispatcher.BeginInvoke(() =>
                                {
                                    if (InitFinished != null)
                                        InitFinished(false, e.Message);
                                });
                                return;
                            }
                            
                            if (InitFinished != null)
                                InitFinished(true, "");
                        }
                    }, preq);
                }
            }, request);            
        }

        /// <summary>
        /// Event handler for when the push channel Uri is updated.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        static void PushChannel_ChannelUriUpdated(object sender, NotificationChannelUriEventArgs e)
        {
            Deployment.Current.Dispatcher.BeginInvoke(() => { Register(); });
        }

        /// <summary>
        /// Event handler for when a push notification error occurs.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        static void PushChannel_ErrorOccurred(object sender, NotificationChannelErrorEventArgs e)
        {
            // Error handling logic for your particular application would be here.
            Deployment.Current.Dispatcher.BeginInvoke(() =>
                MessageBox.Show(String.Format("A push notification {0} error occurred.  {1} ({2}) {3}",
                    e.ErrorType, e.Message, e.ErrorCode, e.ErrorAdditionalData))
                    );
        }

        /// <summary>
        /// Event handler for when a toast notification arrives while your application is running.  
        /// The toast will not display if your application is running so you must add this
        /// event handler if you want to do something with the toast notification.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        static void PushChannel_ShellToastNotificationReceived(object sender, NotificationEventArgs e)
        {
            StringBuilder message = new StringBuilder();
            string relativeUri = string.Empty;

            message.AppendFormat("Received Toast {0}:\n", DateTime.Now.ToShortTimeString());

            // Parse out the information that was part of the message.
            foreach (string key in e.Collection.Keys)
            {
                message.AppendFormat("{0}: {1}\n", key, e.Collection[key]);

                if (string.Compare(
                    key,
                    "wp:Param",
                    System.Globalization.CultureInfo.InvariantCulture,
                    System.Globalization.CompareOptions.IgnoreCase) == 0)
                {
                    relativeUri = e.Collection[key];
                }
            }

            // Display a dialog of all the fields in the toast.
            Deployment.Current.Dispatcher.BeginInvoke(() => MessageBox.Show(message.ToString()));

        }

    }
}
