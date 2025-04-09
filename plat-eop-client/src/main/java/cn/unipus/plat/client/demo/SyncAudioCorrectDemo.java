package cn.unipus.plat.client.demo;

import cn.unipus.plat.client.dto.AudioRequest;
import cn.unipus.plat.client.em.QuesTypeEnum;
import cn.unipus.plat.client.utils.PCipTxtGenerator;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hushun
 * @Date: 2025/3/13
 * @Description: 同步音频测评demo
 */
public class SyncAudioCorrectDemo {
    private String appId = "git13tz10skijcgmlrv65yc2x";
    private String appKey = "8DACCF71669AD19676AF03F89D288085";
    private String syncCorrectUrl = "https://open-test.unipus.cn/openapi/clio/v1/correct/fl";
    private static OkHttpClient okHttpClient;

    // 静态代码块，在类加载时执行，初始化 okHttpClient
    static {
        okHttpClient = new OkHttpClient
                .Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public static void main(String[] args) {
        // 测评参数
        String audioPath = "https://itestres.unipus.cn/itest-product/1843/uanswer/eno_32558/1008424602/1740757744000.m4a.mp3";
        String refText = "I am a student.";
        String userId = "uid-123456";
        Integer quesType = QuesTypeEnum.SENTENCE_ADJUST.getQuesType();

        SyncAudioCorrectDemo demo = new SyncAudioCorrectDemo();
        String syncCorrectResultStr = demo.syncCorrect(audioPath, refText, userId, quesType);
        System.out.println("同步测评结果=" + syncCorrectResultStr);
        JSONObject syncCorrectResultJson = JSONObject.parseObject(syncCorrectResultStr);
        System.out.println("同步测评评分结果=" + syncCorrectResultJson.getJSONObject("value").getString("unifyResult"));
    }

    /**
     * 音频同步测评
     *
     * @return
     */
    public String syncCorrect(String audioPath, String refText, String userId, Integer quesType) {
        AudioRequest request = new AudioRequest();
        request.setAudioPath(audioPath);
        request.setRequestId(UUID.randomUUID().toString().replaceAll("-", ""));
        request.setUserId(userId);
        request.setQuesType(quesType);

        JSONObject requestJson = new JSONObject();
        requestJson.put("refText", refText);
        request.setRequestJson(requestJson.toJSONString());

        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, JSONObject.toJSONString(request));
        System.out.println("请求参数: " + JSONObject.toJSONString(request));

        Request okHttpRequest = new Request.Builder()
                .url(syncCorrectUrl)
                .addHeader("p-app-id", appId)
                .addHeader("p-cip-txt", PCipTxtGenerator.getCipTxt(appKey))
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            return response.body().string();
        } catch (Exception e) {
            System.out.println("同步测评异常: ");
            e.printStackTrace();
        }
        return null;
    }
}
