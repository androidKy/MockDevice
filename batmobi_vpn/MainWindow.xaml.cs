using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Navigation;
using System.Windows.Shapes;
using System.Diagnostics;

using System.Net.Sockets;
using System.Net;
using System.Threading;

namespace socket
{
    /// <summary>
    /// MainWindow.xaml 的交互逻辑
    /// </summary>
    public partial class MainWindow : Window
    {
        public MainWindow()
        {
            InitializeComponent();
        }
        Socket ServerSocket = null;
        Socket clientSocket = null;

        private void btnStart_Click(object sender, RoutedEventArgs e)
        {
            IPEndPoint IPE = new IPEndPoint(IPAddress.Parse(tboxIP.Text), Int32.Parse(tboxPort.Text));
            ServerSocket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
            ServerSocket.Bind(IPE);
            ServerSocket.Listen(10);
            showmsg("服务器已启动，监听中...");

            Thread thread = new Thread(ListenClientConnect);
            thread.IsBackground=true;
            thread.Start();

        }
       // Dictionary<string, Socket> dic = new Dictionary<string, Socket>();
        private void ListenClientConnect(object obj)
        {
            while (true)
            {
                clientSocket = ServerSocket.Accept() ;
                string RemoteIP = clientSocket.RemoteEndPoint.ToString();
               // dic.Add(RemoteIP, socketClient);
                Dispatcher.Invoke(()=>lstboxIP.Items.Add(RemoteIP));
                showmsg(RemoteIP + "已连接");

                Thread recieveThread = new Thread(recievemsg);
                recieveThread.IsBackground = true;
                recieveThread.Start(clientSocket);
            }
        }

        private void recievemsg(object soc)
        {
            Socket socketClient = (Socket)soc;
            while (true)
            {
                byte[] buffer = new byte[512];
                int n = socketClient.Receive(buffer);
                //string msg = Encoding.Default.GetString(buffer, 0, n);
                if(n > 0)
                {
                    string msg = Encoding.UTF8.GetString(buffer, 0, n);
                    //可在这里指定接受数据格式
                     string[] sArray = msg.Split('|');
                    string country = sArray[0];
                    string port = sArray[1];
                    
                    showmsg(socketClient.RemoteEndPoint.ToString()+":"+msg);
                    sendCMD(country,port);                                                      
                }
               
            }
        }

        private void showmsg(string p)
        {
            Dispatcher.BeginInvoke(new Action(() =>
                {
                    rtbx.AppendText(p + "\r\n");
                }));
            
           
           // Console.WriteLine(i.ToString());
           // Console.WriteLine(strs[0]);
            //sendCMD(p);
           
        }

        private void sendCMD(string country,string port)
        {   

            Process p = new Process();
            //设置要启动的应用程序
            p.StartInfo.FileName = "cmd.exe";
            //是否使用操作系统shell启动
            p.StartInfo.UseShellExecute = false;
            // 接受来自调用程序的输入信息
            p.StartInfo.RedirectStandardInput = true;
            //输出信息
            p.StartInfo.RedirectStandardOutput = true;
            // 输出错误
            p.StartInfo.RedirectStandardError = true;
            //不显示程序窗口
            p.StartInfo.CreateNoWindow = true;
            //启动程序
            p.Start();
            //切换指定国家的IP: "Autoproxytool.exe -changeproxy/"+country+" -proxyport="+port;
          
           // string portSwitch = "Autoproxytool.exe -freeport="+port;
           //  p.StandardInput.WriteLine(portSwitch+"&exit");
             string cmd = "Autoproxytool.exe -changeproxy/"+country+" -proxyport="+port;
     
            //向cmd窗口发送输入信息
            p.StandardInput.WriteLine(cmd+"&exit");
            p.StandardInput.AutoFlush=true;
             //获取输出信息
             string strOuput = p.StandardOutput.ReadToEnd();
            //等待程序执行完退出进程
            p.WaitForExit();
            p.Close();
        }

        private void btnStop_Click(object sender, RoutedEventArgs e)
        {
            ServerSocket.Close();
        }

        private void btnSend_Click(object sender, RoutedEventArgs e)
        {
            showmsg(tboxMsg.Text);
            //string ip = "192.168.2.115";
            //string ip = lstboxIP.SelectedItem.ToString();
            byte[] by = Encoding.UTF8.GetBytes(tboxMsg.Text);

           // showmsg("客户端的IP：" + ip);
           // byte[] by = Encoding.Default.GetBytes(tboxMsg.Text);
           // dic[ip].Send(by,0);
           
            clientSocket.Send(by,0);

            tboxMsg.Text = "";
        }
    }
}
