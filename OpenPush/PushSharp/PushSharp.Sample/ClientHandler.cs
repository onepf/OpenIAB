using System;
using System.Collections.Generic;
using System.Linq;
using System.Net.Sockets;
using System.Text;

namespace PushSharp.Sample
{
    public class ClientHandler
    {
        public ClientHandler(TcpClient client)
        {
            string html = "<html><body><h1>It kinda works</h1></body></html>";
            string str = "HTTP/1.1 200 OK\nContent-type: text/html\nContent-Length:" + html.Length.ToString() + "\n\n" + html;
            byte[] buffer = Encoding.ASCII.GetBytes(str);
            client.GetStream().Write(buffer, 0, buffer.Length);

            string request = "";
            byte[] replyBuffer = new byte[1024];
            int count;
            while ((count = client.GetStream().Read(replyBuffer, 0, replyBuffer.Length)) > 0)
            {
                request += Encoding.ASCII.GetString(replyBuffer, 0, count);
                if (request.IndexOf("\r\n\r\n") >= 0 || request.Length > 4096)
                {
                    break;
                }
            }

            client.Close();
        }
    }
}
