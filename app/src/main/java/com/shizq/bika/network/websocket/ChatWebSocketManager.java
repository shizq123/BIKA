package com.shizq.bika.network.websocket;

import android.util.Log;

import com.shizq.bika.network.HttpDns;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ChatWebSocketManager {

    private final static int MAX_NUM = 2;       // 最大重连数
    private final static int MILLIS = 5000;     // 重连间隔时间，毫秒
    private static ChatWebSocketManager mInstance = null;

    private OkHttpClient client;
    private Request request;
    private IReceiveMessage receiveMessage;
    private WebSocket mWebSocket;

    private boolean isConnect = false;
    private int connectNum = 0;

    private ChatWebSocketManager() {
    }

    public static ChatWebSocketManager getInstance() {
        if (null == mInstance) {
            synchronized (ChatWebSocketManager.class) {
                if (mInstance == null) {
                    mInstance = new ChatWebSocketManager();
                }
            }
        }

        return mInstance;
    }

    /**
     * 释放单例, 及其所引用的资源
     */
    public static void release() {
        try {
            if (mInstance != null) {
                mInstance = null;
            }
        } catch (Exception e) {
        }
    }

    public void init(String webSocketURL, IReceiveMessage message) {
        client = new OkHttpClient.Builder()
                .pingInterval(20, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .dns(new HttpDns())
                .build();
        request = new Request.Builder().url(webSocketURL).build();
        receiveMessage = message;
        connect();
    }

    /**
     * 连接
     */
    public void connect() {
        if (isConnect()) {
            return;
        }
        client.newWebSocket(request, createListener());
    }

    /**
     * 重连
     */
    public void reconnect() {
        if (connectNum <= MAX_NUM) {
            try {
                Thread.sleep(MILLIS);
                connect();
                connectNum++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
            //大于重连数时
            if (receiveMessage != null) {
                receiveMessage.onConnectFailed();
            }
        }
    }

    /**
     * 是否连接
     */
    public boolean isConnect() {
        return mWebSocket != null && isConnect;
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (isConnect()) {
            mWebSocket.close(1000, "");
        }

        release();
    }

    private WebSocketListener createListener() {
        return new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                Log.i("WebSocketManager--", "WebSocket 打开:" + response);
                mWebSocket = webSocket;
                isConnect = response.code() == 101;
                if (!isConnect) {
                    reconnect();
                } else {
                    Log.i("WebSocketManager--", "WebSocket 连接成功");
                    if (receiveMessage != null) {
                        receiveMessage.onConnectSuccess();
                    }

                }
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                //过滤心跳包
                if (receiveMessage != null) {
                    receiveMessage.onMessage(text);
                }

            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
                if (receiveMessage != null) {
                    receiveMessage.onMessage(bytes.base64());
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                mWebSocket = null;
                isConnect = false;

                if (receiveMessage != null) {
                    receiveMessage.onClose();
                }
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                mWebSocket = null;
                isConnect = false;

                if (receiveMessage != null) {
                    receiveMessage.onClose();
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                super.onFailure(webSocket, t, response);
                if (response != null) {
                    Log.i("WebSocketManager--", "WebSocket 连接失败：" + response.message());
                }
                Log.i("WebSocketManager--", "WebSocket 连接失败异常原因：" + t.getMessage());
                isConnect = false;

                if (t.getMessage() != null) {
                    if ((t.getMessage() != null || !t.getMessage().equals("") || !t.getMessage().equals(" ")) && !t.getMessage().equals("Socket closed")) {
                        reconnect();
                    }
                }

            }
        };
    }
}
