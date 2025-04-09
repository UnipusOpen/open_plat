package cn.unipus.plat.client;

import cn.unipus.plat.client.demo.SyncAudioCorrectDemo;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hushun
 * @Date: 2025/3/13
 * @Description: 平台 EOP 客户端应用
 */
public class PlatEopClientApp extends JFrame {
    private static OkHttpClient okHttpClient = null;
    private String appId = "git13tz10skijcgmlrv65yc2x";
    private String syncCorrectUrl = "https://open-test.unipus.cn/openapi/clio/v1/correct/fl";
    private JTextArea resultTextArea; // 用于展示接口返回结果

    public PlatEopClientApp() {
        // 初始化 OkHttpClient
        okHttpClient = new OkHttpClient.Builder()
               .connectTimeout(10, TimeUnit.SECONDS)
               .writeTimeout(10, TimeUnit.SECONDS)
               .readTimeout(60, TimeUnit.SECONDS)
               .build();

        // 创建 JTabbedPane
        JTabbedPane tabbedPane = new JTabbedPane();

        // 添加同步语音测评选项卡
        tabbedPane.addTab("同步语音测评", createSyncAudioCorrectPanel());
        // 暂时添加空的异步语音测评、智能写作测评选项卡
        tabbedPane.addTab("异步语音测评", new JPanel());
        tabbedPane.addTab("智能写作测评", new JPanel());

        // 新增：添加结果展示选项卡
        resultTextArea = new JTextArea();
        resultTextArea.setEditable(false);
        JScrollPane resultScrollPane = new JScrollPane(resultTextArea);
        tabbedPane.addTab("接口返回结果", resultScrollPane);

        // 将 JTabbedPane 添加到窗口
        add(tabbedPane);

        // 设置窗口属性
        setTitle("PlatEopClientApp");
        // 修改窗口大小为原来的两倍
        setSize(1200, 800); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 调整字体大小为原来的两倍
        adjustFontSize(tabbedPane);

        setVisible(true);
    }

    private JPanel createSyncAudioCorrectPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // App ID 标签和文本框
        JLabel appIdLabel = new JLabel("App ID:");
        JTextField appIdField = new JTextField(appId);
        appIdField.setEditable(false);

        // 接口地址标签和文本框
        JLabel urlLabel = new JLabel("接口地址:");
        JTextField urlField = new JTextField(syncCorrectUrl);
        urlField.setEditable(false);

        // 音频地址标签和文本框
        JLabel audioPathLabel = new JLabel("音频地址:");
        JTextField audioPathField = new JTextField("https://itestres.unipus.cn/itest-product/1843/uanswer/eno_32558/1008424602/1740757744000.m4a.mp3");

        // 文本内容标签和文本框
        JLabel refTextLabel = new JLabel("文本内容:");
        JTextField refTextField = new JTextField("I am a student.");

        // 题型标签和下拉框
        JLabel quesTypeLabel = new JLabel("题型:");
        String[] quesTypes = {"2-单词", "4-句子", "5-段落", "7-有限分支"};
        JComboBox<String> quesTypeComboBox = new JComboBox<>(quesTypes);
        // 设置默认选中段落
        quesTypeComboBox.setSelectedItem("5-段落");

        // 提交按钮
        JButton submitButton = new JButton("提交");
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String audioPath = audioPathField.getText();
                String refText = refTextField.getText();
                String userId = "uid-123456";
                Integer quesType = getQuesTypeFromComboBox(quesTypeComboBox);

                SyncAudioCorrectDemo demo = new SyncAudioCorrectDemo();
                String syncCorrectResultStr = demo.syncCorrect(audioPath, refText, userId, quesType);
                System.out.println("同步测评结果=" + syncCorrectResultStr);
                JSONObject syncCorrectResultJson = JSONObject.parseObject(syncCorrectResultStr);
                System.out.println("同步测评评分结果=" + syncCorrectResultJson.getJSONObject("value").getString("unifyResult"));

                // 新增：将结果显示在结果文本区域
                resultTextArea.setText("同步测评结果:\n" + syncCorrectResultStr + "\n\n同步测评评分结果:\n" + syncCorrectResultJson.getJSONObject("value").getString("unifyResult"));
            }
        });

        // 设置参数名宽度为原来的 1/3
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 添加标签
        gbc.gridy = 0;
        panel.add(appIdLabel, gbc);
        gbc.gridy = 1;
        panel.add(urlLabel, gbc);
        gbc.gridy = 2;
        panel.add(audioPathLabel, gbc);
        gbc.gridy = 3;
        panel.add(refTextLabel, gbc);
        gbc.gridy = 4;
        panel.add(quesTypeLabel, gbc);

        // 设置参数值区域宽度为剩下的部分
        gbc.gridx = 1;
        gbc.weightx = 0.7;

        // 添加文本框和下拉框
        gbc.gridy = 0;
        panel.add(appIdField, gbc);
        gbc.gridy = 1;
        panel.add(urlField, gbc);
        gbc.gridy = 2;
        panel.add(audioPathField, gbc);
        gbc.gridy = 3;
        panel.add(refTextField, gbc);
        gbc.gridy = 4;
        panel.add(quesTypeComboBox, gbc);

        // 添加提交按钮
        gbc.gridx = 1;
        gbc.gridy = 5;
        panel.add(submitButton, gbc);

        return panel;
    }

    private Integer getQuesTypeFromComboBox(JComboBox<String> comboBox) {
        String selectedItem = (String) comboBox.getSelectedItem();
        return Integer.parseInt(selectedItem.split("-")[0]);
    }

    // 新增：调整字体大小为原来的两倍
    private void adjustFontSize(Component component) {
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                adjustFontSize(child);
            }
        }
        Font font = component.getFont();
        if (font != null) {
            component.setFont(font.deriveFont(font.getSize() * 2f));
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PlatEopClientApp());
    }
}