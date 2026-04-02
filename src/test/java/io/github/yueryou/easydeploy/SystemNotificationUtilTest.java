package io.github.yueryou.easydeploy;

import org.junit.jupiter.api.Test;
import tech.lin2j.idea.plugin.util.SystemNotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统托盘通知工具类测试
 */
public class SystemNotificationUtilTest {

    private static final int NOTIFICATION_DELAY_MS = 500;
    private static final int PIPELINE_DELAY_MS = 1000;

    @Test
    public void testSystemTraySupport() {
        boolean isSupported = SystemTray.isSupported();
        System.out.println("System tray supported: " + isSupported);
        if (!isSupported) {
            System.out.println("Warning: System tray not available on this platform");
        }
    }

    @Test
    public void testInitTrayIcon() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS), "托盘图标应该在 2 秒内初始化完成");
        System.out.println("托盘图标初始化完成");
    }

    @Test
    public void testShowNotification() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();
                SystemNotificationUtil.showNotification("测试通知", "这是一条普通通知");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS), "通知应该在 3 秒内显示");
        System.out.println("普通通知测试完成");
    }

    @Test
    public void testShowSuccessNotification() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();
                SystemNotificationUtil.showSuccessNotification("成功", "操作执行成功！");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS), "成功通知应该在 3 秒内显示");
        System.out.println("成功通知测试完成");
    }

    @Test
    public void testShowWarningNotification() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();
                SystemNotificationUtil.showWarningNotification("警告", "这是一个警告提示");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS), "警告通知应该在 3 秒内显示");
        System.out.println("警告通知测试完成");
    }

    @Test
    public void testShowErrorNotification() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();
                SystemNotificationUtil.showErrorNotification("错误", "发生了一个错误");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS), "错误通知应该在 3 秒内显示");
        System.out.println("错误通知测试完成");
    }

    @Test
    public void testNotifyWithSound() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();
                SystemNotificationUtil.notifyWithSound("带声音的通知", "提示：任务已完成");
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS), "带声音的通知应该在 3 秒内显示");
        System.out.println("带声音的通知测试完成");
    }

    @Test
    public void testPlaySystemSound() {
        assertDoesNotThrow(() -> {
            SystemNotificationUtil.playSystemSound();
            System.out.println("系统声音播放完成");
        });
    }

    @Test
    public void testMultipleNotifications() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);

        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();

                SystemNotificationUtil.showNotification("通知 1", "第一条通知");
                latch.countDown();
                sleep(NOTIFICATION_DELAY_MS);

                SystemNotificationUtil.showSuccessNotification("通知 2", "第二条通知 - 成功");
                latch.countDown();
                sleep(NOTIFICATION_DELAY_MS);

                SystemNotificationUtil.showWarningNotification("通知 3", "第三条通知 - 警告");
                latch.countDown();
                sleep(NOTIFICATION_DELAY_MS);

                SystemNotificationUtil.showErrorNotification("通知 4", "第四条通知 - 错误");
                latch.countDown();
                sleep(NOTIFICATION_DELAY_MS);

                SystemNotificationUtil.notifyWithSound("通知 5", "第五条通知 - 带声音");
                latch.countDown();
            } finally {
                while (latch.getCount() > 0) {
                    latch.countDown();
                }
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "所有通知应该在 10 秒内完成");
        System.out.println("多条通知测试完成");
    }

    @Test
    public void testRemoveTrayIcon() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();
                SystemNotificationUtil.removeTrayIcon();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(2, TimeUnit.SECONDS), "托盘图标应该在 2 秒内移除");
        System.out.println("托盘图标移除测试完成");
    }

    @Test
    public void testPipelineCompleteNotification() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            try {
                SystemNotificationUtil.initTrayIcon();

                SystemNotificationUtil.showNotification(
                    "Running Pipeline",
                    "正在执行流水线：Deploy-Task"
                );

                sleep(PIPELINE_DELAY_MS);

                SystemNotificationUtil.showSuccessNotification(
                    "Pipeline Execution Successful",
                    "任务流水线执行完成！"
                );
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "流水线通知应该在 5 秒内完成");
        System.out.println("流水线完成通知测试完成");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
