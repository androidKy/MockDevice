package com.vm.shadowsocks.tunnel.shadowsocks;

import android.annotation.SuppressLint;
import android.util.Base64;

import com.safframework.log.L;
import com.vm.shadowsocks.core.ProxyConfig;
import com.vm.shadowsocks.tunnel.Tunnel;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;

public class ShadowsocksTunnel extends Tunnel {

    private ICrypt m_Encryptor;
    private ShadowsocksConfig m_Config;
    private boolean m_TunnelEstablished;

    public ShadowsocksTunnel(ShadowsocksConfig config, Selector selector) throws Exception {
        super(config.ServerAddress, selector);
        m_Config = config;
        m_Encryptor = CryptFactory.get(m_Config.EncryptMethod, m_Config.Password);
    }

    @Override
    protected void onConnected(ByteBuffer buffer) throws Exception {
        String userInfo = m_Config.EncryptMethod.replaceAll("\\(", "") + ":" + m_Config.Password.replaceAll("\\)", "");
        String authData = Base64.encodeToString(userInfo.getBytes(), Base64.DEFAULT);
        L.i("userInfo:" + userInfo + " authData:" + authData);
        @SuppressLint("DefaultLocale")
        String request = String.format("CONNECT %s:%d HTTP/1.0\r\nProxy-Authorization: Basic %s\r\n%s\r\nProxy-Connection: keep-alive\r\nUser-Agent: %s\r\nX-App-Install-ID: %s\r\n\r\n",
                m_DestAddress.getHostName(),
                m_DestAddress.getPort(),
                authData,
                userInfo,
                ProxyConfig.Instance.getUserAgent(),
                ProxyConfig.AppInstallID);

        if (ProxyConfig.IS_DEBUG)
            L.i("request:" + request);

        buffer.clear();
        buffer.put(request.getBytes());
        // https://shadowsocks.org/en/spec/protocol.html

        buffer.put((byte) 0x03);//domain
        byte[] domainBytes = m_DestAddress.getHostName().getBytes();
        buffer.put((byte) domainBytes.length);//domain length;
        buffer.put(domainBytes);
        buffer.putShort((short) m_DestAddress.getPort());
        buffer.flip();
        byte[] _header = new byte[buffer.limit()];
        buffer.get(_header);

        buffer.clear();
        buffer.put(m_Encryptor.encrypt(_header));
        buffer.flip();

        if (write(buffer, true)) {
            m_TunnelEstablished = true;
            onTunnelEstablished();
        } else {
            m_TunnelEstablished = true;
            this.beginReceive();
        }
    }

    @Override
    protected boolean isTunnelEstablished() {
        return m_TunnelEstablished;
    }

    @Override
    protected void beforeSend(ByteBuffer buffer) throws Exception {

        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);

        byte[] newbytes = m_Encryptor.encrypt(bytes);

        buffer.clear();
        buffer.put(newbytes);
        buffer.flip();
    }

    @Override
    protected void afterReceived(ByteBuffer buffer) throws Exception {
        byte[] bytes = new byte[buffer.limit()];
        buffer.get(bytes);
        byte[] newbytes = m_Encryptor.decrypt(bytes);
        String s = new String(newbytes);
        buffer.clear();
        buffer.put(newbytes);
        buffer.flip();
    }

    @Override
    protected void onDispose() {
        m_Config = null;
        m_Encryptor = null;
    }

}
