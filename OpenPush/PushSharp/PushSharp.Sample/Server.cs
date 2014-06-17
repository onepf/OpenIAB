using PushSharp.Android;
using PushSharp.WindowsPhone;
using System;
using System.IO;
using System.Net;
using System.Collections.Generic;
using System.Web.Script.Serialization;
using PushSharp.Apple;

namespace PushSharp.Sample
{
    public class Server : HttpServer
    {
        readonly PushBroker _push;

        const string WP8 = "wp8";
        const string Android = "android";
        const string iOS = "ios";

        public Server(PushBroker push, int port)
            : base(port)
        {
            _push = push;
        }
        
        ~Server()
        {
            _push.StopAllServices();
        }

        public override void handleGETRequest(HttpProcessor p)
        {
            Console.WriteLine("request: {0}", p.http_url);
            p.writeSuccess();
            p.outputStream.WriteLine("<html><body><h1>Push server</h1>");
            p.outputStream.WriteLine("Current Time: " + DateTime.Now.ToString());
            p.outputStream.WriteLine("url : {0}", p.http_url);

            p.outputStream.WriteLine("<form method=post action=/form>");
            p.outputStream.WriteLine("<input type=text name=foo value=foovalue>");
            p.outputStream.WriteLine("<input type=submit name=bar value=barvalue>");
            p.outputStream.WriteLine("</form>");
        }

        public override void handlePOSTRequest(HttpProcessor p, StreamReader inputData)
        {
            string data = inputData.ReadToEnd();
            Console.WriteLine("POST request: {0}", data);
            p.writeSuccess();

            JavaScriptSerializer ser = new JavaScriptSerializer();
            var json = ser.Deserialize<Dictionary<string, string>>(data);
            string platform = json["platform"];
            string token = json["token"];

            switch (platform)
            {
                case Android:
                    _push.QueueNotification(new GcmNotification()
                        .ForDeviceRegistrationId(token)
                        .WithJson("{\"title\":\"OpenPush!\",\"message\":\"Hello there!\"}"));
                    break;
                case iOS:
                    _push.QueueNotification(new AppleNotification()
                           .ForDeviceToken(token)
                           .WithAlert("Hello there!")
                           .WithBadge(7)
                           .WithSound("sound.caf"));
                    break;
                case WP8:
                    _push.QueueNotification(new WindowsPhoneToastNotification()
                        .ForEndpointUri(new Uri(token))
                        .ForOSVersion(WindowsPhoneDeviceOSVersion.MangoSevenPointFive)
                        .WithBatchingInterval(BatchingInterval.Immediate)
                        //.WithNavigatePath("/MainPage.xaml")
                        .WithText1("OpenPush")
                        .WithText2("Hello there!"));
                    break;
            }
        }
    }
}