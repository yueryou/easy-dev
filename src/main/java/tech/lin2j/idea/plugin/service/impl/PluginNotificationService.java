package tech.lin2j.idea.plugin.service.impl;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import tech.lin2j.idea.plugin.enums.Constant;
import tech.lin2j.idea.plugin.util.SystemNotificationUtil;

/**
 * 插件通知服务 - 支持 IntelliJ 通知和系统托盘通知
 *
 * @author linjinjia
 * @date 2024/11/17 19:31
 */
@Service
public final class PluginNotificationService {

    private final NotificationGroup notificationGroup;

    public PluginNotificationService() {
        notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup(Constant.EASY_DEPLOY);
        // 初始化系统托盘图标
        SystemNotificationUtil.initTrayIcon();
    }

    /**
     * 显示通知（同时发送到 IntelliJ）
     */
    public void showNotification(Project project, String title, String message) {
        Notification notification = notificationGroup.createNotification(title, message, NotificationType.INFORMATION);
        notification.setTitle(title);
        notification.setContent(message);
        notification.notify(project);
    }

    /**
     * 显示通知并带有操作按钮
     */
    public void showNotification(Project project, String title, String message, AnAction action) {
        Notification notification = notificationGroup.createNotification(title, message, NotificationType.INFORMATION);
        notification.setTitle(title);
        notification.setContent(message);
        notification.addAction(action);
        notification.notify(project);
    }

    /**
     * 仅显示系统托盘通知（不显示 IntelliJ 通知）
     */
    public void showSystemTrayNotification(String title, String message) {
        SystemNotificationUtil.showNotification(title, message);
    }

    /**
     * 显示系统托盘成功通知
     */
    public void showSystemTraySuccessNotification(String title, String message) {
        SystemNotificationUtil.showSuccessNotification(title, message);
    }

    /**
     * 显示系统托盘警告通知
     */
    public void showSystemTrayWarningNotification(String title, String message) {
        SystemNotificationUtil.showWarningNotification(title, message);
    }

    /**
     * 显示系统托盘错误通知
     */
    public void showSystemTrayErrorNotification(String title, String message) {
        SystemNotificationUtil.showErrorNotification(title, message);
    }

    /**
     * 显示通知并播放提示音
     */
    public void showNotificationWithSound(String title, String message) {
        SystemNotificationUtil.notifyWithSound(title, message);
    }

    /**
     * 显示通知并播放提示音（同时发送 IntelliJ 和系统托盘通知）
     */
    public void showNotificationWithSound(Project project, String title, String message) {
        showNotification(project, title, message);
        SystemNotificationUtil.notifyWithSound(title, message);
    }

    /**
     * 显示系统托盘错误通知（同时发送 IntelliJ 和系统托盘通知）
     */
    public void showSystemTrayErrorNotification(Project project, String title, String message) {
        showNotification(project, title, message);
        SystemNotificationUtil.showErrorNotification(title, message);
    }
}
