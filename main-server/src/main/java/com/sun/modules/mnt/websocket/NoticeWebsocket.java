package com.sun.modules.mnt.websocket;

import cn.hutool.core.lang.UUID;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ServerEndpoint 注解是一个类层次的注解，它的功能主要是将目前的类定义成一个websocket服务器端,
 * 注解的值将被用于监听用户连接的终端访问URL地址,客户端可以通过这个URL来连接到WebSocket服务器端
 */
@ServerEndpoint("/notice/{userId}")
@Component
@Slf4j
public class NoticeWebsocket {
    //记录连接的客户端
    public static Map<String, Session> clients = new ConcurrentHashMap<>();

    /**
     * userId关联sid（解决同一用户id，在多个web端连接的问题）
     */
    public static Map<String, Set<String>> conns = new ConcurrentHashMap<>();

    private String sid = null;

    private String userId;


    /**
     * 连接成功后调用的方法
     *
     * @param session
     * @param userId
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.sid = UUID.randomUUID().toString();
        this.userId = userId;

        clients.put(this.sid, session);
        Set<String> clientSet = conns.computeIfAbsent(userId, k -> new HashSet<>());
        clientSet.add(this.sid);
        log.info(this.sid + "连接开启！");
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        log.info(this.sid + "连接断开！");
        clients.remove(this.sid);
    }

    /**
     * 判断是否连接的方法
     *
     * @return
     */
    public static boolean isServerClose() {
        if (NoticeWebsocket.clients.values().size() == 0) {
            log.info("已断开");
            return true;
        } else {
            log.info("已连接");
            return false;
        }
    }

    /**
     * 发送给所有用户
     *
     * @param noticeType
     * @param noticeInfo
     */
    public static void sendMessage(String noticeType, String userId, Object noticeInfo) {
        NoticeWebsocketResp noticeWebsocketResp = new NoticeWebsocketResp();
        noticeWebsocketResp.setNoticeType(noticeType);
        noticeWebsocketResp.setNoticePeople(userId);
        noticeWebsocketResp.setNoticeInfo(noticeInfo);
        sendMessage(noticeWebsocketResp);
    }


    /**
     * 发送给所有用户
     *
     * @param noticeWebsocketResp
     */
    public static void sendMessage(NoticeWebsocketResp noticeWebsocketResp) {
        String message = JSONObject.toJSONString(noticeWebsocketResp);
        for (Session session1 : NoticeWebsocket.clients.values()) {
            try {
                session1.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据用户id发送给某一个用户
     **/
    public static void sendMessageByUserId(String userId, NoticeWebsocketResp noticeWebsocketResp) {
        if (!StringUtils.isEmpty(userId)) {
            String message = JSONObject.toJSONString(noticeWebsocketResp);
            Set<String> clientSet = conns.get(userId);
            if (clientSet != null) {
                Iterator<String> iterator = clientSet.iterator();
                synchronized (clientSet) {
                    while (iterator.hasNext()) {
                        String sid = iterator.next();
                        Session session = clients.get(sid);
                        if (session != null) {
                            try {
                                session.getAsyncRemote().sendText(message);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message
     * @param session
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("收到来自窗口" + this.userId + "的信息:" + message);
    }

    /**
     * 发生错误时的回调函数
     *
     * @param error
     */
    @OnError
    public void onError(Throwable error) {
        log.info("错误");
        error.printStackTrace();
    }
}
