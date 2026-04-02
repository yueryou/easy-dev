package tech.lin2j.idea.plugin.util;

import com.intellij.openapi.diagnostic.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * 系统托盘通知工具类 - 使用 Java 原生 AWT TrayIcon 发送系统通知
 * <p>
 * 适用于 Windows/macOS/Linux 系统，不需要外部依赖或 PowerShell 脚本
 *
 * @author linjinjia
 * @date 2026/04/02
 */
public class SystemNotificationUtil {

    private static final Logger LOG = Logger.getInstance(SystemNotificationUtil.class);
    private static volatile TrayIcon trayIcon;

    /**
     * 初始化系统托盘图标（应在 EDT 线程中调用）
     * 建议在插件启动时调用一次即可
     */
    public static void initTrayIcon() {
        if (!SystemTray.isSupported()) {
            LOG.warn("System tray is not supported on this platform");
            return;
        }

        if (trayIcon != null) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            if (trayIcon != null) {
                return;
            }
            try {
                SystemTray tray = SystemTray.getSystemTray();

                Image iconImage = createDefaultIcon();

                trayIcon = new TrayIcon(iconImage, "Easy Deploy");
                trayIcon.setImageAutoSize(true);

                trayIcon.addActionListener(e ->
                    trayIcon.displayMessage("Easy Deploy", "点击查看插件状态", TrayIcon.MessageType.INFO)
                );

                tray.add(trayIcon);
                LOG.info("System tray icon initialized successfully");

            } catch (AWTException e) {
                LOG.warn("Failed to initialize system tray icon: " + e.getMessage());
            }
        });
    }

    /**
     * 显示系统通知（非 EDT 线程安全）
     *
     * @param title   通知标题
     * @param message 通知内容
     */
    public static void showNotification(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.INFO);
    }

    /**
     * 显示系统通知（可指定类型）
     *
     * @param title   通知标题
     * @param message 通知内容
     * @param type    通知类型
     */
    public static void showNotification(String title, String message, TrayIcon.MessageType type) {
        // 确保在 EDT 线程中执行
        if (SwingUtilities.isEventDispatchThread()) {
            doShowNotification(title, message, type);
        } else {
            SwingUtilities.invokeLater(() -> doShowNotification(title, message, type));
        }
    }

    /**
     * 显示成功通知（绿色对勾图标）
     *
     * @param title   通知标题
     * @param message 通知内容
     */
    public static void showSuccessNotification(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.INFO);
    }

    /**
     * 显示警告通知（黄色感叹号图标）
     *
     * @param title   通知标题
     * @param message 通知内容
     */
    public static void showWarningNotification(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.WARNING);
    }

    /**
     * 显示错误通知（红色 X 图标）
     *
     * @param title   通知标题
     * @param message 通知内容
     */
    public static void showErrorNotification(String title, String message) {
        showNotification(title, message, TrayIcon.MessageType.ERROR);
    }

    /**
     * 播放系统提示音（可选的辅助通知方式）
     */
    public static void playSystemSound() {
        try {
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            LOG.debug("Failed to play system sound: " + e.getMessage());
        }
    }

    /**
     * 同时显示通知并播放提示音
     *
     * @param title   通知标题
     * @param message 通知内容
     */
    public static void notifyWithSound(String title, String message) {
        showNotification(title, message);
        playSystemSound();
    }

    private static void doShowNotification(String title, String message, TrayIcon.MessageType type) {
        if (!SystemTray.isSupported()) {
            LOG.debug("System tray not supported, skipping notification: " + title);
            return;
        }

        if (trayIcon != null) {
            // Windows 系统托盘通知的标题固定为 JVM 名称，因此将标题和消息合并显示
            String displayTitle = "Easy Deploy";
            String displayMessage = "[" + title + "] " + message;
            trayIcon.displayMessage(displayTitle, displayMessage, type);
            return;
        }

        // 如果托盘图标未初始化，先初始化再显示通知
        initTrayIcon();
        SwingUtilities.invokeLater(() -> {
            if (trayIcon != null) {
                String displayTitle = "Easy Deploy";
                String displayMessage = "[" + title + "] " + message;
                trayIcon.displayMessage(displayTitle, displayMessage, type);
            }
        });
    }

    /**
     * 创建默认的 16x16 插件图标
     */
    private static BufferedImage createDefaultIcon() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        // 启用抗锯齿
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制绿色背景圆
        g2.setColor(new Color(76, 175, 80));
        g2.fillOval(0, 0, 16, 16);

        // 绘制白色对勾
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.0f));
        g2.drawLine(4, 8, 7, 11);
        g2.drawLine(7, 11, 12, 5);

        g2.dispose();
        return image;
    }

    /**
     * 移除系统托盘图标（插件卸载时调用）
     */
    public static void removeTrayIcon() {
        if (trayIcon != null) {
            SwingUtilities.invokeLater(() -> {
                SystemTray tray = SystemTray.getSystemTray();
                tray.remove(trayIcon);
                trayIcon = null;
                LOG.info("System tray icon removed");
            });
        }
    }
}
