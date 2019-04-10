package me.aw.uia;
import android.util.Log;
import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.view.accessibility.AccessibilityNodeInfo;
import android.graphics.Rect;
import android.os.SystemClock;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.List;
import java.util.regex.*;
import java.util.concurrent.TimeoutException;
import android.os.Bundle;
import android.content.IClipboard;
import android.os.IBinder;
import android.content.ClipData;
import android.os.ServiceManager;
//import com.android.uiautomator.core.UiAutomationShellWrapper;

public class Daemon {
    private static final String HANDLER_THREAD_NAME = "UiAutomatorHandlerThread";
    private static final HandlerThread mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
    public static UiAutomation mUiAutomation;

    public static void main(String[] args) {
        try {
            if (args.length > 1) {
                new Daemon(args);
            } else {
                showUsage();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }


    private Daemon(String[] args) {
//                        Integer.parseInt(args[0]),
//                        Integer.parseInt(args[1]),
//                        Integer.parseInt(args[2]),
//                        Integer.parseInt(args[3])
        final long startTime = SystemClock.uptimeMillis();
        String selector, action, id;
        int index = 0;
        boolean id_mode = true;
        String[] selectors;
        selector = args[0];
        action = args[1];
        if (action.equals("setClip")) {
            setClip(selector);
            System.out.print("ok");
            return;
        }
        Matcher m = Pattern.compile("^([@%/:])id\\1(.+)$").matcher(selector);
        if (m.find()) {
            id = m.group(2);
        } else {
            m = Pattern.compile("^([@%/:])id\\1(.+)\\1(\\d+)$").matcher(selector);
            if (m.find()) {
                id = m.group(2);
                index = Integer.parseInt(m.group(3));
            } else {
                m = Pattern.compile("^([@%/:])text\\1(.+)$").matcher(selector);
                if (m.find()) {
                    id = m.group(2);
                    id_mode = false;
                } else {
                    m = Pattern.compile("^([@%/:])text\\1(.+)\\1(\\d+)$").matcher(selector);
                    if (m.find()) {
                        id = m.group(2);
                        index = Integer.parseInt(m.group(3));
                        id_mode = false;
                    } else {
                        //System.err.println("Error:invalid selector");
                        id = selector;
                    }
                }
            }
        }
//        Log.e("Garri", id);
//        System.err.println("IS ID:" + (id_mode ? "1":"0"));
//        System.err.println("ID:" + id);

        if (!mHandlerThread.isAlive()) {
            mHandlerThread.start();
        }

        mUiAutomation = new UiAutomation(mHandlerThread.getLooper(),
                new UiAutomationConnection());
        mUiAutomation.connect();

        try {
            mUiAutomation.waitForIdle(200, 1000 * 10);

            AccessibilityNodeInfo node, root = mUiAutomation.getRootInActiveWindow();
            if (root == null) {
                mUiAutomation.disconnect();
                mUiAutomation.connect();
                root = mUiAutomation.getRootInActiveWindow();
                if (root == null) {
                    System.err.println("Error:UiAutomation");
                    mUiAutomation.disconnect();
                    mHandlerThread.quit();
                    return;
                }
            }

            if (id_mode) {
                node = getById(root, id, index);
            } else {
                node = getByText(root, id, index);
            }

            if (node == null) {
                System.err.println("Error:not found");
                mUiAutomation.disconnect();
                mHandlerThread.quit();
                return;
            }

            //System.out.println("node found");

            if (action.equals("click")) {
                performClick(node);
                mUiAutomation.disconnect();
                mHandlerThread.quit();
                return;
            }

            if (action.equals("pgdown")) {
                performPageDown(node);
                mUiAutomation.disconnect();
                mHandlerThread.quit();
                return;
            }

            if (action.equals("pgup")) {
                performPageUp(node);
                mUiAutomation.disconnect();
                mHandlerThread.quit();
                return;
            }
//            if (action.equals("longclick")) {
//                performClick(node);
//                return;
//            }
            if (action.equals("text")) {
                System.err.print(node.getText());
                mUiAutomation.disconnect();
                mHandlerThread.quit();
                return;
            }
            if (action.equals("xy")) {
                Rect rect = node.getBoundsInScreen();
                System.err.print(rect.top + "," + rect.right + "," + rect.bottom + "," + rect.left);
                mUiAutomation.disconnect();
                mHandlerThread.quit();
                return;
            }
            
            if (action.equals("xyxy")) {
                List<AccessibilityNodeInfo> l;
                if (id_mode) {
                    l = getAllById(root, id);
                } else {
                    l = getAllByText(root, id);
                }
                int j = 0;
                String ret = "";
                for (int i = 0; i < l.size(); i++) {
                    if (l.get(i).isVisibleToUser()) {
                        node = l.get(i);
                        Rect rect = node.getBoundsInScreen();
                        ret += rect.top + "," + rect.right + "," + rect.bottom + "," + rect.left + "|";
                        j++;
                    }
                }
                System.out.print(ret.replaceAll("\\|$",""));
                mUiAutomation.disconnect();
                mHandlerThread.quit();
                return;
            }
            if (action.equals("paste")) {
                if (args.length == 3) {
                    pasteText(node, args[2]);
                    mUiAutomation.disconnect();
                    mHandlerThread.quit();
                    return;
                }
            }
            showUsage();

        } catch (TimeoutException re) {
            System.err.println("ERROR: could not get idle state.");
            mUiAutomation.disconnect();
            mHandlerThread.quit();
            return;
        } finally {
            //mUiAutomation.disconnect();
        }
        final long endTime = SystemClock.uptimeMillis();
        Log.w("Garri", "Fetch time: " + (endTime - startTime) + "ms");
    }

    private static void showUsage() {
        System.err.println(
                "Daemon SELECTOR ACTION" + "\n"
                        + "" + "\n"
                        + "SELECTOR:" + "\n"
                        + "     @id@[S]" + " => eg: @id@my_id\n"
                        + "     @id@[S]@[N]" + " => eg: @id@new_xo@2\n"
                        + "     @text@[S]" + " => eg: @id@my home\n"
                        + "     @text@[S]@[N]" + " => eg: @id@my home@1\n"
                        + "     [S]" + " // as id selector\n"
                        + "" + "\n"
                        + "ACTION:" + "\n"
                        + "     click" + "\n"
//                        + "     longclick" + "\n"
                        + "     text" + "\n"
                        + "     xy" + "\n"
                        + "     xyxy" + "\n"
                        + "     pgdown" + "\n"
                        + "     pgup" + "\n"
                        + "     paste" + " // texts\n"
                        + "     setClip" + " // set selector as text to input\n"
                        + "" + "\n"
                        + "@ is a seperator that is one of" + "\n"
                        + "     @ % / :" + "\n"
                        + "" + "\n"
                        + "[N] means numberic which is a number" + "\n"
                        + "" + "\n"
                        + "[S] means selector that is a string" + "\n"
        );
    }

    public static void setClip(String text)
    {
        try
        {
            IClipboard clipboardManager;
            IBinder b = ServiceManager.getService("clipboard");
            clipboardManager = IClipboard.Stub.asInterface(b);

            ClipData clip = ClipData.newPlainText("text", text);
            //clipboard.setPrimaryClip(clip);
            clipboardManager.setPrimaryClip(clip, text);
        }
        catch(RemoteException e)
        {
            System.err.print(e.getMessage());
        }
    
    }

    public static void pasteText(AccessibilityNodeInfo info, String text) {
//        try {
            info.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Bundle arguments = new Bundle();
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            info.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
//        } catch (RemoteException exception) {
//
//        }
    }

    public static void performPageDown(AccessibilityNodeInfo node) {
        while (node != null && !node.isScrollable()) {
            node = node.getParent();
        }
        if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
    }
    public static void performPageUp(AccessibilityNodeInfo node) {
        while (node != null && !node.isScrollable()) {
            node = node.getParent();
        }
        if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
    }

    public static void performClick(AccessibilityNodeInfo node) {
        while (node != null && !node.isClickable()) {
            node = node.getParent();
        }
        if (node != null) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    public static AccessibilityNodeInfo getByText(AccessibilityNodeInfo root, String text, int index) {
        List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByText(text);
        int j = 0;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).isVisibleToUser()) {
                if (j == index) {
                    return l.get(i);
                }
                j++;
            }
        }
        return null;
    }

    public static AccessibilityNodeInfo getById(AccessibilityNodeInfo root, String id, int index) {
        List<AccessibilityNodeInfo> l = root.findAccessibilityNodeInfosByViewId(id);
        int j = 0;
        for (int i = 0; i < l.size(); i++) {
            if (l.get(i).isVisibleToUser()) {
                if (j == index) {
                    return l.get(i);
                }
                j++;
            }
        }
        return null;
    }

    public static List<AccessibilityNodeInfo> getAllByText(AccessibilityNodeInfo root, String text) {
        return root.findAccessibilityNodeInfosByText(text);
    }

    public static List<AccessibilityNodeInfo> getAllById(AccessibilityNodeInfo root, String id) {
        return root.findAccessibilityNodeInfosByViewId(id);
    }
}

