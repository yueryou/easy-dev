package io.github.yueryou.easydev.plugin.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.content.Content;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import tech.lin2j.idea.plugin.file.ConsoleTransferListener;
import tech.lin2j.idea.plugin.file.filter.ConsoleFileFilter;
import tech.lin2j.idea.plugin.model.ConfigHelper;
import tech.lin2j.idea.plugin.service.ISshService;
import tech.lin2j.idea.plugin.ssh.CommandLog;
import tech.lin2j.idea.plugin.ssh.SshConnectionManager;
import tech.lin2j.idea.plugin.ssh.SshServer;
import tech.lin2j.idea.plugin.ssh.sshj.SshjConnection;
import tech.lin2j.idea.plugin.uitl.MessagesBundle;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * Dialog for selecting multiple servers to upload project files to
 *
 * @author yueryou
 * @date 2026/04/21
 */
public class SelectServersForUploadDialog extends DialogWrapper {

    private final Project project;
    private final List<VirtualFile> selectedFiles;
    private final JBTable serverTable;
    private final ServerTableModel tableModel;
    private final JTextField remotePathField;
    private final JTextArea postCommandArea;

    public SelectServersForUploadDialog(@Nullable Project project, @NotNull List<VirtualFile> selectedFiles) {
        super(project);
        this.project = project;
        this.selectedFiles = selectedFiles;

        List<SshServer> servers = ConfigHelper.sshServers();
        this.tableModel = new ServerTableModel(servers);
        this.serverTable = new JBTable(tableModel);
        this.remotePathField = new JTextField("/tmp");
        this.postCommandArea = new JTextArea(3, 40);
        this.postCommandArea.setMinimumSize(new Dimension(0, 35));
        this.postCommandArea.setPreferredSize(new Dimension(0, 35));

        setTitle(MessagesBundle.getText("dialog.uploadto.title"));
        setSize(500, 400);
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        serverTable.getColumnModel().getColumn(0).setMaxWidth(40);
        serverTable.getColumnModel().getColumn(0).setMinWidth(40);
        serverTable.getColumnModel().getColumn(0).setPreferredWidth(40);

        // Disable row selection so clicking checkbox doesn't highlight the row
        serverTable.setRowSelectionAllowed(false);

        ColoredTableCellRenderer nameRenderer = new ColoredTableCellRenderer() {
            @Override
            protected void customizeCellRenderer(
                    @NotNull JTable table,
                    Object value,
                    boolean selected,
                    boolean hasFocus,
                    int row,
                    int column) {
                if (value instanceof SshServer) {
                    SshServer server = (SshServer) value;
                    String display = server.getIp();
                    if (server.getTag() != null && !server.getTag().isEmpty()) {
                        display += " (" + server.getTag() + ")";
                    }
                    append(display, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
            }
        };
        serverTable.getColumnModel().getColumn(1).setCellRenderer(nameRenderer);

        // Single-click toggle for checkbox column
        serverTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int col = serverTable.columnAtPoint(e.getPoint());
                int row = serverTable.rowAtPoint(e.getPoint());
                if (col == 0 && row >= 0 && row < serverTable.getRowCount()) {
                    Boolean current = (Boolean) tableModel.getValueAt(row, 0);
                    tableModel.setValueAt(!Boolean.TRUE.equals(current), row, 0);
                    // Repaint only the checkbox cell
                    Rectangle rect = serverTable.getCellRect(row, 0, true);
                    serverTable.repaint(rect);
                }
            }
        });

        // Show checkbox cursor on hover
        serverTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int col = serverTable.columnAtPoint(e.getPoint());
                serverTable.setCursor(new Cursor(col == 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });

        // Post command panel with border and padding
        JPanel postCommandPanel = new JPanel(new BorderLayout());
        postCommandPanel.setBorder(JBUI.Borders.compound(
                JBUI.Borders.customLine(com.intellij.ui.JBColor.LIGHT_GRAY),
                JBUI.Borders.empty(4)
        ));
        postCommandArea.setMargin(JBUI.insets(6, 8));
        postCommandArea.setLineWrap(true);
        postCommandArea.setWrapStyleWord(true);
        postCommandPanel.add(new JScrollPane(postCommandArea), BorderLayout.CENTER);

        JPanel panel = FormBuilder.createFormBuilder()
                .addLabeledComponent(MessagesBundle.getText("dialog.uploadto.servers"), new JScrollPane(serverTable), true)
                .addVerticalGap(8)
                .addLabeledComponent(MessagesBundle.getText("dialog.uploadto.remote.path"), remotePathField)
                .addVerticalGap(8)
                .addLabeledComponent(MessagesBundle.getText("dialog.uploadto.post.command"), postCommandPanel, true)
                .getPanel();
        panel.setBorder(JBUI.Borders.empty(8));
        return panel;
    }

    @Override
    protected void doOKAction() {
        Set<SshServer> selectedServers = tableModel.getSelectedServers();
        if (selectedServers.isEmpty()) {
            return;
        }

        String remotePath = remotePathField.getText().trim();
        if (remotePath.isEmpty()) {
            return;
        }

        String postCommand = postCommandArea.getText().trim();

        close(this);
        showToolWindow();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            CommandLog commandLog = project.getUserData(CommandLog.COMMAND_LOG_KEY);
            assert commandLog != null;
            commandLog.getConsole().clear();
            commandLog.info("========================================");
            commandLog.info("开始上传 " + selectedFiles.size() + " 个文件/目录 -> " + selectedServers.size() + " 台服务器");
            commandLog.info("========================================");

            CountDownLatch latch = new CountDownLatch(selectedServers.size());
            for (SshServer server : selectedServers) {
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    try {
                        uploadToServer(server, remotePath, postCommand, commandLog);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                commandLog.error("上传被中断");
            }

            commandLog.info("上传结束 at: " + LocalDateTime.now());
        });
    }

    private void close(DialogWrapper dialog) {
        dialog.close(OK_EXIT_CODE);
    }

    private void showToolWindow() {
        ToolWindow deployToolWindow = ToolWindowManager.getInstance(project).getToolWindow("Easy Dev");
        assert deployToolWindow != null;
        deployToolWindow.activate(null);
        Content messages = deployToolWindow.getContentManager().findContent("Console");
        deployToolWindow.getContentManager().setSelectedContent(messages);
    }

    private void uploadToServer(SshServer server, String remotePath, String postCommand, CommandLog commandLog) {
        String serverLabel = server.getIp() + ":" + server.getPort();
        commandLog.info("=== 开始上传到 " + serverLabel + " ===");

        SshjConnection sshjConnection = null;
        try {
            sshjConnection = SshConnectionManager.makeSshjConnection(server);
            ISshService sshService = ApplicationManager.getApplication().getService(ISshService.class);

            ConsoleFileFilter filter = new ConsoleFileFilter(commandLog);

            for (VirtualFile file : selectedFiles) {
                String localPath = file.getPath();
                commandLog.info("Upload [" + file.getName() + "] to [" + remotePath + "]");

                sshjConnection.setTransferListener(new ConsoleTransferListener(remotePath, commandLog));

                boolean success = sshService.upload(filter, sshjConnection, localPath, remotePath, commandLog, true);
                if (!success) {
                    commandLog.error("Upload failed: " + file.getName());
                } else {
                    commandLog.info("  -> Done: " + file.getName());
                }
            }

            // Execute post-upload command in remote path directory
            if (postCommand != null && !postCommand.isEmpty()) {
                commandLog.info("=== 执行上传后命令 ===");
                String cmd = "cd " + escapeShellArg(remotePath) + " && " + escapeShellArg(postCommand);
                sshjConnection.execute(cmd, commandLog);
                commandLog.info("=== 命令执行完成 ===");
            }

            sshjConnection.close();
            commandLog.info("=== " + serverLabel + " 上传完成 ===");
        } catch (Exception e) {
            commandLog.error(serverLabel + " 上传失败: " + e.getMessage());
            if (sshjConnection != null) {
                sshjConnection.close();
            }
        }
    }

    /**
     * Escapes a string for safe use as a shell argument by wrapping it in single quotes
     * and escaping any embedded single quotes.
     */
    private static String escapeShellArg(String arg) {
        if (arg == null) {
            return "''";
        }
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private static class ServerTableModel extends AbstractTableModel {
        private final List<SshServer> servers;
        private final Set<Integer> selectedIndices = new HashSet<>();

        public ServerTableModel(List<SshServer> servers) {
            this.servers = servers;
        }

        @Override
        public int getRowCount() {
            return servers.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public @NotNull Class<?> getColumnClass(int columnIndex) {
            return columnIndex == 0 ? Boolean.class : SshServer.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return selectedIndices.contains(rowIndex);
            }
            return servers.get(rowIndex);
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            if (columnIndex == 0 && rowIndex >= 0 && rowIndex < servers.size()) {
                if (Boolean.TRUE.equals(aValue)) {
                    selectedIndices.add(rowIndex);
                } else {
                    selectedIndices.remove(rowIndex);
                }
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Set<SshServer> getSelectedServers() {
            return selectedIndices.stream()
                    .filter(i -> i >= 0 && i < servers.size())
                    .map(servers::get)
                    .collect(Collectors.toSet());
        }
    }
}
