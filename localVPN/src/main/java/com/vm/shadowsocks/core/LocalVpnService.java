package com.vm.shadowsocks.core;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;

import com.proxy.service.R;
import com.safframework.log.L;
import com.vm.shadowsocks.core.ProxyConfig.IPAddress;
import com.vm.shadowsocks.dns.DnsPacket;
import com.vm.shadowsocks.provider.Provider;
import com.vm.shadowsocks.provider.ProviderPicker;
import com.vm.shadowsocks.tcpip.CommonMethods;
import com.vm.shadowsocks.tcpip.IPHeader;
import com.vm.shadowsocks.tcpip.TCPHeader;
import com.vm.shadowsocks.tcpip.UDPHeader;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LocalVpnService extends VpnService implements Runnable {

    public static String BROADCAST_STOP_VPN = "com.vpn.stop";
    public static WeakReference<Activity> mReferenceActivity;
    public static LocalVpnService Instance;
    public static String ProxyUrl;
    public static boolean IsRunning = false;

    private static int ID;
    public static int LOCAL_IP;
    private static ConcurrentHashMap<onStatusChangedListener, Object> m_OnStatusChangedListeners = new ConcurrentHashMap<onStatusChangedListener, Object>();

    private Thread m_VPNThread;
    private ParcelFileDescriptor m_VPNInterface;
    private TcpProxyServer m_TcpProxyServer;
    private DnsProxy m_DnsProxy;
    private FileOutputStream m_VPNOutputStream;

    private byte[] m_Packet;
    private IPHeader m_IPHeader;
    private TCPHeader m_TCPHeader;
    private UDPHeader m_UDPHeader;
    private ByteBuffer m_DNSBuffer;
    private Handler m_Handler;
    private long m_SentBytes;
    private long m_ReceivedBytes;
    public HashMap<String, String> dnsServers;
    private Provider provider;

    public LocalVpnService() {
        ID++;
        m_Handler = new Handler();
        m_Packet = new byte[20000];
        m_IPHeader = new IPHeader(m_Packet, 0);
        m_TCPHeader = new TCPHeader(m_Packet, 20);
        m_UDPHeader = new UDPHeader(m_Packet, 20);
        m_DNSBuffer = ((ByteBuffer) ByteBuffer.wrap(m_Packet).position(28)).slice();
        Instance = this;

        System.out.printf("New VPNService(%d)\n", ID);
    }


    public static void setAcitivity(Activity acitivity) {
        mReferenceActivity = new WeakReference<>(acitivity);
    }

    @Override
    public void onCreate() {
        registerReceiver(mVpnStopReceiver, new IntentFilter(BROADCAST_STOP_VPN));
        System.out.printf("VPNService(%s) created.\n", ID);
        // Start a new session by creating a new thread.
        m_VPNThread = new Thread(this, "VPNServiceThread");
        m_VPNThread.start();
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IsRunning = true;
        return super.onStartCommand(intent, flags, startId);
    }

    public interface onStatusChangedListener {
        public void onStatusChanged(String status, Boolean isRunning);

        public void onLogReceived(String logString);
    }

    public static void addOnStatusChangedListener(onStatusChangedListener listener) {
        if (!m_OnStatusChangedListeners.containsKey(listener)) {
            m_OnStatusChangedListeners.put(listener, 1);
        }
    }

    public static void removeOnStatusChangedListener(onStatusChangedListener listener) {
        if (m_OnStatusChangedListeners.containsKey(listener)) {
            m_OnStatusChangedListeners.remove(listener);
        }
    }

    private void onStatusChanged(final String status, final boolean isRunning) {
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
                    entry.getKey().onStatusChanged(status, isRunning);
                }
            }
        });
    }

    public void writeLog(final String format, Object... args) {
        final String logString = String.format(format, args);
        m_Handler.post(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<onStatusChangedListener, Object> entry : m_OnStatusChangedListeners.entrySet()) {
                    entry.getKey().onLogReceived(logString);
                }
            }
        });
    }

    String getAppInstallID() {
        SharedPreferences preferences = getSharedPreferences("SmartProxy", MODE_PRIVATE);
        String appInstallID = preferences.getString("AppInstallID", null);
        if (appInstallID == null || appInstallID.isEmpty()) {
            appInstallID = UUID.randomUUID().toString();
            Editor editor = preferences.edit();
            editor.putString("AppInstallID", appInstallID);
            editor.apply();
        }
        return appInstallID;
    }

    String getVersionName() {
        try {
            PackageManager packageManager = getPackageManager();
            // getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            String version = packInfo.versionName;
            return version;
        } catch (Throwable e) {
            return "0.0";
        }
    }

    @Override
    public synchronized void run() {
        try {
            System.out.printf("VPNService(%s) work thread is runing...\n", ID);

            ProxyConfig.AppInstallID = getAppInstallID();//获取安装ID
            ProxyConfig.AppVersion = getVersionName();//获取版本号
            System.out.printf("AppInstallID: %s\n", ProxyConfig.AppInstallID);
            writeLog("Android version: %s", Build.VERSION.RELEASE);
            writeLog("App version: %s", ProxyConfig.AppVersion);


            ChinaIpMaskManager.loadFromFile(getResources().openRawResource(R.raw.ipmask));//加载中国的IP段，用于IP分流。
            waitUntilPreapred();//检查是否准备完毕。

            writeLog("Load config from file ...");
            try {
                ProxyConfig.Instance.loadFromFile(getResources().openRawResource(R.raw.config));
                writeLog("Load done");
            } catch (Throwable e) {
                String errString = e.getMessage();
                if (errString == null || errString.isEmpty()) {
                    errString = e.toString();
                }
                writeLog("Load failed with error: %s", errString);
            }

            m_TcpProxyServer = new TcpProxyServer(0);
            m_TcpProxyServer.start();
            writeLog("LocalTcpServer started.");

            m_DnsProxy = new DnsProxy();
            m_DnsProxy.start();
            writeLog("LocalDnsProxy started.");

            while (true) {
                if (IsRunning) {
                    //加载配置文件
                    // writeLog("set shadowsocks/(http proxy)");
                    try {
                        ProxyConfig.Instance.m_ProxyList.clear();
                        ProxyConfig.Instance.addProxyToList(ProxyUrl);
                        //writeLog("Proxy is: %s", ProxyConfig.Instance.getDefaultProxy());
                        L.i("Proxy is:" + ProxyConfig.Instance.getDefaultProxy());
                    } catch (Exception e) {
                        String errString = e.getMessage();
                        if (errString == null || errString.isEmpty()) {
                            errString = e.toString();
                        }
                        IsRunning = false;
                        L.i("加载配置失败：" + e.getMessage());
                        onStatusChanged(errString, false);
                        continue;
                    }
                    String welcomeInfoString = ProxyConfig.Instance.getWelcomeInfo();
                    if (welcomeInfoString != null && !welcomeInfoString.isEmpty()) {
                        writeLog("%s", ProxyConfig.Instance.getWelcomeInfo());
                    }
                    writeLog("Global mode is " + (ProxyConfig.Instance.globalMode ? "on" : "off"));

                    runVPN();
                } else {
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e);
        } catch (Throwable e) {
            e.printStackTrace();
            writeLog("Fatal error: %s", e.toString());
        } finally {
            writeLog("App terminated.");
            dispose();
        }
    }

    private void runVPN() throws Exception {
        // ParcelFileDescriptor fileDescriptor = establishVPN();

        this.m_VPNInterface = establishVPN();
        this.m_VPNOutputStream = new FileOutputStream(m_VPNInterface.getFileDescriptor());
        FileInputStream in = new FileInputStream(m_VPNInterface.getFileDescriptor());
        int size = 0;
        while (size != -1 && IsRunning) {
            while ((size = in.read(m_Packet)) > 0 && IsRunning) {
                if (m_DnsProxy.Stopped || m_TcpProxyServer.Stopped) {
                    in.close();
                    throw new Exception("LocalServer stopped.");
                }
                onIPPacketReceived(m_IPHeader, size);
            }
            Thread.sleep(20);
        }
        in.close();
        disconnectVPN();
    }

    //接收拦截的数据，然后通过VPN转发到代理服务器
    void onIPPacketReceived(IPHeader ipHeader, int size) throws IOException {
        switch (ipHeader.getProtocol()) {
            case IPHeader.TCP:
                // L.i("转发tcp DNS数据包");
                TCPHeader tcpHeader = m_TCPHeader;
                tcpHeader.m_Offset = ipHeader.getHeaderLength();
                if (ipHeader.getSourceIP() == LOCAL_IP) {
                    if (tcpHeader.getSourcePort() == m_TcpProxyServer.Port) {// 收到本地TCP服务器数据
                        NatSession session = NatSessionManager.getSession(tcpHeader.getDestinationPort());
                        if (session != null) {
                            ipHeader.setSourceIP(ipHeader.getDestinationIP());
                            tcpHeader.setSourcePort(session.RemotePort);
                            ipHeader.setDestinationIP(LOCAL_IP);

                            CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
                            m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
                            m_ReceivedBytes += size;
                        } else {
                            System.out.printf("NoSession: %s %s\n", ipHeader.toString(), tcpHeader.toString());
                        }
                    } else {
                        // 添加端口映射
                        int portKey = tcpHeader.getSourcePort();
                        NatSession session = NatSessionManager.getSession(portKey);
                        if (session == null || session.RemoteIP != ipHeader.getDestinationIP() || session.RemotePort != tcpHeader.getDestinationPort()) {
                            session = NatSessionManager.createSession(portKey, ipHeader.getDestinationIP(), tcpHeader.getDestinationPort());
                        }

                        session.LastNanoTime = System.nanoTime();
                        session.PacketSent++;//注意顺序

                        int tcpDataSize = ipHeader.getDataLength() - tcpHeader.getHeaderLength();
                        if (session.PacketSent == 2 && tcpDataSize == 0) {
                            return;//丢弃tcp握手的第二个ACK报文。因为客户端发数据的时候也会带上ACK，这样可以在服务器Accept之前分析出HOST信息。
                        }

                        //分析数据，找到host
                        if (session.BytesSent == 0 && tcpDataSize > 10) {
                            int dataOffset = tcpHeader.m_Offset + tcpHeader.getHeaderLength();
                            String host = HttpHostHeaderParser.parseHost(tcpHeader.m_Data, dataOffset, tcpDataSize);
                            if (host != null) {
                                session.RemoteHost = host;
                            } else {
                                L.i("No host name found: RemoteIP=" + session.RemoteHost);
                                // System.out.printf("No host name found: %s", session.RemoteHost);
                            }
                        }

                        // 转发给本地TCP服务器
                        ipHeader.setSourceIP(ipHeader.getDestinationIP());
                        ipHeader.setDestinationIP(LOCAL_IP);
                        tcpHeader.setDestinationPort(m_TcpProxyServer.Port);

                        CommonMethods.ComputeTCPChecksum(ipHeader, tcpHeader);
                        m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, size);
                        session.BytesSent += tcpDataSize;//注意顺序
                        m_SentBytes += size;
                    }
                }
                break;
            case IPHeader.UDP:
                // 转发DNS数据包：
                L.i("转发udp DNS数据包");
                UDPHeader udpHeader = m_UDPHeader;
                udpHeader.m_Offset = ipHeader.getHeaderLength();


                if (ipHeader.getSourceIP() == LOCAL_IP && udpHeader.getDestinationPort() == 53) {
                    // L.i("getDestinationPort");
                    m_DNSBuffer.clear();
                    m_DNSBuffer.limit(ipHeader.getDataLength() - 8);
                    DnsPacket dnsPacket = DnsPacket.FromBytes(m_DNSBuffer);
                    if (dnsPacket != null && dnsPacket.Header.QuestionCount > 0) {
                        //L.i("开始转发UDP数据");
                        m_DnsProxy.onDnsRequestReceived(ipHeader, udpHeader, dnsPacket);
                        // m_DnsProxy.interceptDns(ipHeader,udpHeader,dnsPacket);
                    }
                }
                break;
            default:
                L.i("转发其他数据协议:" + ipHeader.getProtocol());
                break;
        }
    }

    public void sendUDPPacket(IPHeader ipHeader, UDPHeader udpHeader) {
        try {
            CommonMethods.ComputeUDPChecksum(ipHeader, udpHeader);
            this.m_VPNOutputStream.write(ipHeader.m_Data, ipHeader.m_Offset, ipHeader.getTotalLength());
        } catch (Exception e) {
            L.i("sendUDPPacket发送UDP数据：" + e.getMessage());
        }
    }

    private void waitUntilPreapred() {
        while (prepare(this) != null) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private ParcelFileDescriptor establishVPN() throws Exception {
        Builder builder = new Builder();
        builder.setMtu(ProxyConfig.Instance.getMTU());
        /*if (ProxyConfig.IS_DEBUG)
            System.out.printf("setMtu: %d\n", ProxyConfig.Instance.getMTU());*/

        //addAddress
        IPAddress ipAddress = ProxyConfig.Instance.getDefaultLocalIP();
        LOCAL_IP = CommonMethods.ipStringToInt(ipAddress.Address);
        L.i("LOCAL_IP：" + LOCAL_IP + " addAddress address: " + ipAddress.Address + " prefixLength:" + ipAddress.PrefixLength);
        if (CheckIp(ipAddress.Address)) {
            builder.addAddress(ipAddress.Address, 32);
        } else builder.addAddress(ipAddress.Address, 64);
        //add public dns
        dnsServers = new HashMap<>();
        //addDnsServer(builder,null,"");
       /* String primaryServer = "119.29.29.29";    //public dns:119.29.29.29 182.254.116.116
        String secondaryServer = "182.254.116.116";
        InetAddress aliasPrimary = InetAddress.getByName(primaryServer);
        InetAddress aliasSecondary = InetAddress.getByName(secondaryServer);
        builder.addDnsServer(aliasPrimary).addDnsServer(aliasSecondary);*/
        for (ProxyConfig.IPAddress dns : ProxyConfig.Instance.getDnsList()) {
            builder.addDnsServer(dns.Address);
        }
        //addRoute
        if (CheckIp(ipAddress.Address)) {
            builder.addRoute("0.0.0.0", 0);
        } else {
            builder.addRoute("::", 0);
        }

        /*if (ProxyConfig.Instance.getRouteList().size() > 0) {
            for (ProxyConfig.IPAddress routeAddress : ProxyConfig.Instance.getRouteList()) {
                L.i("config.xml addRoute route: " + routeAddress.Address + " prefixLength:" + routeAddress.PrefixLength);
                builder.addRoute(routeAddress.Address, routeAddress.PrefixLength);
            }
            builder.addRoute(CommonMethods.ipIntToString(ProxyConfig.FAKE_NETWORK_IP), 16);
        } else {
            builder.addRoute("0.0.0.0", 0);
        }*/

       /* Class<?> SystemProperties = Class.forName("android.os.SystemProperties");
        Method method = SystemProperties.getMethod("get", new Class[]{String.class});
        //ArrayList<String> servers = new ArrayList<String>();
        for (String name : new String[]{"net.dns1", "net.dns2", "net.dns3", "net.dns4"}) {
            String value = (String) method.invoke(null, name);

            if (!TextUtils.isEmpty(value)) {
                if (CheckIp(value)) {
                    L.i("addRoute route:" + value + " prefixLength:" + 32);
                    builder.addRoute(value, 32);
                } else {
                    L.i("addRoute route:" + value + " prefixLength:" + 64);
                    builder.addRoute(value, 128);
                }
//                String valueReplace = value.replaceAll("\\d", "");
//                System.out.printf("%s\n", valueReplace);
//                //IPV4 地址长度 32位  带有.标识
//                //IPV6地址长度 128位  带有::标识
//                if (valueReplace.length() == 3 && valueReplace.contains(".")) {//防止IPv6地址导致问题
//                    builder.addRoute(value, 32);
//                } else {
//                    builder.addRoute(value, 128);
//                }
            }
        }*/

      /*  builder.setBlocking(true);
        builder.allowFamily(OsConstants.AF_INET);
        builder.allowFamily(OsConstants.AF_INET6);*/

        //addAllowedApplication
        if (AppProxyManager.isLollipopOrAbove) {
            writeLog("Proxy App Info:" + AppProxyManager.getInstance().getProxyAppList());
            if (AppProxyManager.getInstance().getProxyAppList().size() == 0) {
                writeLog("Proxy All Apps");
            }
            for (AppInfo app : AppProxyManager.getInstance().getProxyAppList()) {
//                builder.addAllowedApplication("com.vm.shadowsocks");//需要把自己加入代理，不然会无法进行网络链接
                // builder.addAllowedApplication("com.bull.vpn");//需要把自己加入代理，不然会无法进行网络链接
                //builder.addAllowedApplication("com.net.request");//需要把自己加入代理，不然会无法进行网络链接
                try {
                    builder.addAllowedApplication(app.getPkgName());
//                    writeLog("Proxy App: " + app.getAppLabel() + " , PackName:" + app.getPkgName());
                } catch (Throwable e) {
                    e.printStackTrace();
//                    writeLog("Proxy App Fail: " + app.getAppLabel());
                }
            }
        } else {
            writeLog("No Pre-App proxy, due to low Android version.");
        }

        if (mReferenceActivity.get() != null) {
            Intent intent = new Intent(this, mReferenceActivity.get().getClass());
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
            builder.setConfigureIntent(pendingIntent);
        }

        builder.setSession(ProxyConfig.Instance.getSessionName());


        ParcelFileDescriptor pfdDescriptor = builder.establish();

       /* provider = ProviderPicker.getProvider(pfdDescriptor, this);
        provider.start();
        provider.process();*/

        onStatusChanged(ProxyConfig.Instance.getSessionName() + "已连接", true);
        return pfdDescriptor;
    }

    /**
     * 判断IP是否IPV4地址
     */
    private boolean CheckIp(String ip) {
        boolean isIpv4 = false;
        try {
            InetAddress address = InetAddress.getByName(ip);
            if (address instanceof Inet4Address) {
                isIpv4 = true;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return isIpv4;
    }

    public void disconnectVPN() {
        try {
            if (m_VPNInterface != null) {
                m_VPNInterface.close();
                m_VPNInterface = null;
            }
        } catch (Throwable e) {
            // ignore
        }
        onStatusChanged(ProxyConfig.Instance.getSessionName() + "断开连接", false);
        this.m_VPNOutputStream = null;
    }

    private void dispose() {
        // 断开VPN
        IsRunning = false;
        disconnectVPN();

        if (m_VPNInterface != null) {
            try {
                m_VPNInterface.close();
            } catch (IOException e) {
                L.e(e.getMessage(), e);
            }
        }

        // 停止TcpServer
        if (m_TcpProxyServer != null) {
            m_TcpProxyServer.stop();
            m_TcpProxyServer = null;
            writeLog("LocalTcpServer stopped.");
        }

        // 停止DNS解析器
        if (m_DnsProxy != null) {
            m_DnsProxy.stop();
            m_DnsProxy = null;
            writeLog("LocalDnsProxy stopped.");
        }

        if (provider != null) {
            provider.shutdown();
            provider.stop();
        }

        stopSelf();
        // IsRunning = false;
        // System.exit(0);
    }

    @Override
    public void onDestroy() {
        System.out.printf("VPNService(%s) destoried.\n", ID);
        if (m_VPNThread != null) {
            m_VPNThread.interrupt();
        }
        if (mVpnStopReceiver != null) {
            unregisterReceiver(mVpnStopReceiver);
        }
    }

    private VpnStateReceiver mVpnStopReceiver = new VpnStateReceiver();

    class VpnStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null)
                return;

            if (intent.getAction().equals(BROADCAST_STOP_VPN)) {
                L.i("接收到停止VPN服务的广播");
                dispose();
            }
        }
    }

    private InetAddress addDnsServer(Builder builder, String format, byte[] ipv6Template, String addr) throws UnknownHostException {
        int size = dnsServers.size();
        size++;
        if (addr.contains("/")) {//https uri
            String alias = String.format(format, size + 1);
            dnsServers.put(alias, addr);
            builder.addRoute(alias, 32);
            return InetAddress.getByName(alias);
        }
        InetAddress address = InetAddress.getByName(addr);
        if (address instanceof Inet6Address && ipv6Template == null) {
            L.i("addDnsServer: Ignoring DNS server " + address);
        } else if (address instanceof Inet4Address) {
            String alias = String.format(format, size + 1);
            dnsServers.put(alias, address.getHostAddress());
            builder.addRoute(alias, 32);
            return InetAddress.getByName(alias);
        } else if (address instanceof Inet6Address) {
            ipv6Template[ipv6Template.length - 1] = (byte) (size + 1);
            InetAddress i6addr = Inet6Address.getByAddress(ipv6Template);
            dnsServers.put(i6addr.getHostAddress(), address.getHostAddress());
            return i6addr;
        }
        return null;
    }

    public static class VpnNetworkException extends Exception {
        public VpnNetworkException(String s) {
            super(s);
        }

        public VpnNetworkException(String s, Throwable t) {
            super(s, t);
        }

    }


}
