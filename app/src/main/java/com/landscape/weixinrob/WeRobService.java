package com.landscape.weixinrob;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

public class WeRobService extends AccessibilityService {

    private List<AccessibilityNodeInfo> openedReds = new ArrayList<>();

    public WeRobService() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        switch (eventType) {
            //第一步：监听通知栏消息
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        Log.i("demo", "text:" + content);
                        if (content.contains("[微信红包]")) {
                            //模拟打开通知栏消息
                            if (event.getParcelableData() != null
                                    &&
                                    event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event.getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pendingIntent.send();
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                break;

            case AccessibilityEvent.TYPE_VIEW_SCROLLED:
                String scrollClsName = event.getClassName().toString();
                Log.i("demo", "scrollClsName:" + scrollClsName);
                if (scrollClsName.equals("android.widget.ListView")) {
                    //开始抢红包
                    recycle(event.getSource(),false);
                }
                break;
            //第二步：监听是否进入微信红包消息界面
//            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
//                String clsName = event.getClassName().toString();
//                Log.i("demo", "clsName:" + clsName);
//                if (clsName.equals("android.widget.ListView")) {
//                    //开始抢红包
//                    delayHandler.removeCallbacks(delayOpenRun);
//                    delayHandler.postDelayed(delayOpenRun, 100);
//                }
//                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:

                String className = event.getClassName().toString();
                if (className.equals("com.tencent.mm.ui.LauncherUI")) {
                    //开始抢红包
                    delayHandler.removeCallbacks(delayOpenRun);
                    delayHandler.postDelayed(delayOpenRun, 100);
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                    //开始打开红包
                    openPacket();
                } else if (className.equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    delayHandler.removeCallbacks(delayCloseRun);
                    delayHandler.postDelayed(delayCloseRun, 500);
                }
                break;
        }
    }

    Handler delayHandler = new Handler();

    Runnable delayCloseRun = new Runnable() {
        @Override
        public void run() {
            closeDetail();
        }
    };

    Runnable delayOpenRun = new Runnable() {
        @Override
        public void run() {
            getPacket();
        }
    };


    /**
     * 查找到
     */
    @SuppressLint("NewApi")
    private void openPacket() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByViewId("com.tencent.mm:id/bi3");
            for (AccessibilityNodeInfo n : list) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }

    }

    @SuppressLint("NewApi")
    private void getPacket() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        recycle(rootNode);
    }

    @SuppressLint("NewApi")
    private void closeDetail() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> list = nodeInfo
                    .findAccessibilityNodeInfosByViewId("com.tencent.mm:id/gv");
            for (AccessibilityNodeInfo n : list) {
                n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    public boolean recycle(AccessibilityNodeInfo info) {
        return recycle(info, true);
    }

    /**
     * 打印一个节点的结构
     *
     * @param info
     */
    @SuppressLint("NewApi")
    public boolean recycle(AccessibilityNodeInfo info,boolean check) {
        if (info.getChildCount() == 0) {
            if (info.getText() != null) {
                if ("领取红包".equals(info.getText().toString()) && (!check || !isOpened(info))) {
                    //这里有一个问题需要注意，就是需要找到一个可以点击的View
                    Log.i("demo", "Click" + ",isClick:" + info.isClickable());
                    info.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    AccessibilityNodeInfo parent = info.getParent();
                    while (parent != null) {
                        Log.i("demo", "parent isClick:" + parent.isClickable());
                        if (parent.isClickable()) {
                            parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            if (!openedReds.contains(info)) {
                                openedReds.add(info);
                            }
                            return true;
                        }
                        parent = parent.getParent();
                    }

                }
            }
            return false;
        } else {
            if (!check && info.isScrollable()) {
                // listView
                if (info.getChild(info.getChildCount() - 1) != null) {
                    if (recycle(info.getChild(info.getChildCount() - 1), check)) {
                        return true;
                    }
                }
            } else {
                for (int i = info.getChildCount()-1; i >= 0; i--) {
                    if (info.getChild(i) != null) {
                        if (recycle(info.getChild(i),check)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private boolean isOpened(AccessibilityNodeInfo info) {
        Log.i("demo", "isOpened:" + openedReds.contains(info));
        return openedReds.contains(info);
    }

    @Override
    public void onInterrupt() {

    }

}
