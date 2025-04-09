package cn.unipus.plat.demo;

import cn.unipus.plat.demo.dto.AudioRequest;
import cn.unipus.plat.demo.em.QuesTypeEnum;
import cn.unipus.plat.demo.utils.PCipTxtGenerator;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @Author: [原作者]
 * @Date: [原日期]
 * @Description: 同步音频测评demo
 */
public class SyncAudioCorrectDemo {

    private String appId;
    private String appKey;
    private String syncAudioCorrectUrl;

    private static OkHttpClient okHttpClient = null;

    public SyncAudioCorrectDemo() {
        loadConfig();
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("未找到配置文件");
                return;
            }
            properties.load(input);
            appId = properties.getProperty("plat.appId");
            appKey = properties.getProperty("plat.appKey");
            syncAudioCorrectUrl = properties.getProperty("plat.syncAudioCorrectUrl"); // 假设配置文件中有该配置项
        } catch (IOException e) {
            System.out.println("加载配置文件时出错");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        okHttpClient = new OkHttpClient
                .Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        // 测评参数
        String audioPath = "https://cdn.unischool.cn/ulearning/app/audio/ai/9382_1741739868827.wav";
        String refText = "I come from china.";
        String userId = "uid-123456";
        Integer quesType = QuesTypeEnum.PARAGRRAPH.getQuesType();
        Integer rank = 100;

        SyncAudioCorrectDemo demo = new SyncAudioCorrectDemo();
        String syncCorrectResultStr = demo.submitSyncCorrect(audioPath, refText, userId, quesType, rank);
        System.out.println("同步测评结果=" + syncCorrectResultStr);
    }

    public String submitSyncCorrect(String audioPath, String refText, String userId, Integer quesType, Integer rank) {
        AudioRequest request = new AudioRequest();
        request.setAudioPath(audioPath);
        request.setRequestId(UUID.randomUUID().toString().replaceAll("-", ""));
        request.setUserId(userId);
        request.setQuesType(quesType);

        JSONObject requestJson = new JSONObject();
        requestJson.put("refText", refText);
        requestJson.put("rank", rank);
        request.setRequestJson(requestJson.toJSONString());

        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, JSONObject.toJSONString(request));
        System.out.println("提交同步测评请求参数: " + JSONObject.toJSONString(request));

        Request okHttpRequest = new Request.Builder()
                .url(syncAudioCorrectUrl)
                .addHeader("p-app-id", appId)
                .addHeader("p-cip-txt", PCipTxtGenerator.getCipTxt(appKey))
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            return response.body().string();
        } catch (Exception e) {
            System.out.println("提交同步测评请求异常: ");
            e.printStackTrace();
        }
        return null;
    }
}
