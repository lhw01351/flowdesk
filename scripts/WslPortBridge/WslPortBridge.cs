// Windows TCP bridge: 0.0.0.0:ListenPort -> WSL Tomcat
// netsh portproxy(iphlpsvc) 대신 사용해 CloseWait 끊김을 피함
using System;
using System.Diagnostics;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

class WslPortBridge
{
    const int ListenPort = 8080;
    const int TargetPort = 8080;
    const string Distro = "Ubuntu";
    static string _cachedIp = null;
    static DateTime _ipAt = DateTime.MinValue;
    static readonly object _ipLock = new object();

    static int Main(string[] args)
    {
        try
        {
            // 기존 portproxy가 8080을 점유 중이면 안내
            var listener = new TcpListener(IPAddress.Any, ListenPort);
            listener.Server.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
            listener.Start();
            Log("LISTEN 0.0.0.0:" + ListenPort + " -> WSL:" + TargetPort);
            while (true)
            {
                var client = listener.AcceptTcpClient();
                client.NoDelay = true;
                ThreadPool.QueueUserWorkItem(_ => HandleClient(client));
            }
        }
        catch (Exception ex)
        {
            Log("FATAL " + ex.Message);
            return 1;
        }
    }

    static void HandleClient(TcpClient client)
    {
        TcpClient remote = null;
        try
        {
            string ip = GetWslIp();
            if (string.IsNullOrEmpty(ip))
            {
                Log("NO_WSL_IP");
                client.Close();
                return;
            }

            remote = new TcpClient();
            remote.NoDelay = true;
            var ar = remote.BeginConnect(ip, TargetPort, null, null);
            if (!ar.AsyncWaitHandle.WaitOne(3000) || !remote.Connected)
            {
                Log("CONNECT_FAIL " + ip);
                client.Close();
                try { remote.Close(); } catch { }
                return;
            }
            remote.EndConnect(ar);

            NetworkStream a = client.GetStream();
            NetworkStream b = remote.GetStream();
            var t1 = Task.Run(() => Pump(a, b));
            var t2 = Task.Run(() => Pump(b, a));
            Task.WaitAny(t1, t2);
        }
        catch (Exception ex)
        {
            Log("HANDLE " + ex.Message);
        }
        finally
        {
            try { client.Close(); } catch { }
            try { if (remote != null) remote.Close(); } catch { }
        }
    }

    static void Pump(NetworkStream from, NetworkStream to)
    {
        byte[] buf = new byte[8192];
        try
        {
            int n;
            while ((n = from.Read(buf, 0, buf.Length)) > 0)
            {
                to.Write(buf, 0, n);
                to.Flush();
            }
        }
        catch { }
    }

    static string GetWslIp()
    {
        lock (_ipLock)
        {
            if (_cachedIp != null && (DateTime.UtcNow - _ipAt).TotalSeconds < 15)
                return _cachedIp;
        }

        string ip = null;
        try
        {
            var psi = new ProcessStartInfo
            {
                FileName = "wsl.exe",
                Arguments = "-d " + Distro + " -- hostname -I",
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };
            using (var p = Process.Start(psi))
            {
                string output = p.StandardOutput.ReadToEnd();
                p.WaitForExit(5000);
                foreach (var part in output.Split(new[] { ' ', '\t', '\r', '\n' }, StringSplitOptions.RemoveEmptyEntries))
                {
                    IPAddress addr;
                    if (IPAddress.TryParse(part, out addr) && addr.AddressFamily == AddressFamily.InterNetwork)
                    {
                        ip = part;
                        break;
                    }
                }
            }
        }
        catch (Exception ex)
        {
            Log("WSL_IP_ERR " + ex.Message);
        }

        if (ip != null)
        {
            lock (_ipLock)
            {
                _cachedIp = ip;
                _ipAt = DateTime.UtcNow;
            }
        }
        return ip;
    }

    static void Log(string msg)
    {
        try
        {
            string line = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss") + " " + msg;
            Console.WriteLine(line);
            string path = @"d:\ispark_prj\flowdesk\scripts\WslPortBridge\bridge.log";
            System.IO.File.AppendAllText(path, line + Environment.NewLine, Encoding.UTF8);
        }
        catch { }
    }
}
