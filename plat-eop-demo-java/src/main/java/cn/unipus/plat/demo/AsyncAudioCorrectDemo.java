package cn.unipus.plat.demo;

import cn.unipus.plat.demo.dto.AudioRequest;
import cn.unipus.plat.demo.em.QuesTypeEnum;
import cn.unipus.plat.demo.utils.PCipTxtGenerator;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hushun
 * @Date: 2025/3/13
 * @Description: 异步音频测评demo
 */
public class AsyncAudioCorrectDemo {
    private String appId = "git13tz10skijcgmlrv65yc2x";
    private String appKey = "8DACCF71669AD19676AF03F89D288085";
    private String asyncCorrectUrl = "https://open-test.unipus.cn/openapi/clio/v1/correct/off/fl";
    private String asycCorrectResultQueryUrl = "https://open-test.unipus.cn/openapi/clio/v1/correct/query";
    private static OkHttpClient okHttpClient = null;
    private static int queryTimeout = 120;


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

        AsyncAudioCorrectDemo demo = new AsyncAudioCorrectDemo();
        String asyncSubmitResultStr = demo.submitAsyncCorrect(audioPath, refText, userId, quesType, rank);
        System.out.println("异步测评提交结果=" + asyncSubmitResultStr);
        JSONObject syncCorrectResultJson = JSONObject.parseObject(asyncSubmitResultStr);
        String correctId = syncCorrectResultJson.getJSONObject("value").getString("correctId");
        String asyncCorrectResultStr = null;
        JSONObject asyncCorrectResultJson = null;

        // 查询测评结果
        int count = 1;
        while (true) {
            asyncCorrectResultStr = demo.queryAsyncCorrect(correctId, "0");
            if (StringUtils.isNotEmpty(asyncCorrectResultStr)) {
                asyncCorrectResultJson = JSONObject.parseObject(asyncCorrectResultStr);
                System.out.println("第" + count + "次轮询结果:" + asyncCorrectResultJson);
                if (asyncCorrectResultJson.getInteger("code") == 0) {
                    break;
                }
            }
            count++;
            if (count >= queryTimeout) {
                break;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("异步测评最终轮询结果=" + asyncCorrectResultStr);
        if (null != asyncCorrectResultJson) {
            System.out.println("异步测评评分结果=" + asyncCorrectResultJson.getJSONObject("value").getString("result"));
        }
    }

    public String queryAsyncCorrect(String correctId, String targetGroup) {
        Request okHttpRequest = new Request.Builder()
                .url(asycCorrectResultQueryUrl + "?correctId=" + correctId + "&targetGroup=" + targetGroup)
                .addHeader("p-app-id", appId)
                .addHeader("p-cip-txt", PCipTxtGenerator.getCipTxt(appKey))
                .get()
                .build();

        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            return response.body().string();
        } catch (Exception e) {
            System.out.println("查询异步结果异常: ");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 提交音频异步测评
     *
     * @return
     */
    public String submitAsyncCorrect(String audioPath, String refText, String userId, Integer quesType, Integer rank) {
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
        System.out.println("提交异步测评请求参数: " + JSONObject.toJSONString(request));

        Request okHttpRequest = new Request.Builder()
                .url(asyncCorrectUrl)
                .addHeader("p-app-id", appId)
                .addHeader("p-cip-txt", PCipTxtGenerator.getCipTxt(appKey))
                .post(body)
                .build();

        try (Response response = okHttpClient.newCall(okHttpRequest).execute()) {
            return response.body().string();
        } catch (Exception e) {
            System.out.println("提交异步测评请求异常: ");
            e.printStackTrace();
        }
        return null;
    }
}
