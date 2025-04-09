package cn.unipus.plat.demo;

import cn.unipus.plat.demo.dto.WriteRequest;
import cn.unipus.plat.demo.utils.PCipTxtGenerator;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hushun
 * @Date: 2025/3/14
 * @Description: 异步写作测评demo
 */
public class AsyncWriteCorrectDemo {
    private String appId;
    private String appKey;
    private String asyncWriteCorrectUrl;
    private String asyncWriteQueryUrl;
    private static OkHttpClient okHttpClient = null;
    private static int queryTimeout = 120;

    public AsyncWriteCorrectDemo() {
        loadConfig();
    }

    private void loadConfig() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            properties.load(input);
            appId = properties.getProperty("plat.appId");
            appKey = properties.getProperty("plat.appKey");
            asyncWriteCorrectUrl = properties.getProperty("plat.asyncWriteCorrectUrl");
            asyncWriteQueryUrl = properties.getProperty("plat.asyncWriteQueryUrl");
        } catch (IOException e) {
            System.out.println("加载配置文件异常: ");
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
        String title = "I come from china.";
        String content = "Hooray! It's snowing! It's time to make a snowman.James runs out. He makes a big pile of snow. He puts a big snowball on top. He adds a scarf and a hat. He adds an orange for the nose. He adds coal for the eyes and buttons.In the evening, James opens the door. What does he see? The snowman is moving! James invites him in. The snowman has never been inside a house. He says hello to the cat. He plays with paper towels.A moment later, the snowman takes James's hand and goes out.They go up, up, up into the air! They are flying! What a wonderful night!The next morning, James jumps out of bed. He runs to the door.He wants to thank the snowman. But he's gone.";

        AsyncWriteCorrectDemo demo = new AsyncWriteCorrectDemo();
        String asyncSubmitResultStr = demo.submitAsyncCorrect(title, content);
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
            System.out.println("异步测评评分结果=" + asyncCorrectResultJson.getJSONObject("value"));
        }
    }

    public String queryAsyncCorrect(String correctId, String targetGroup) {
        Request okHttpRequest = new Request.Builder()
                .url(asyncWriteQueryUrl + "?correctId=" + correctId + "&targetGroup=" + targetGroup)
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
     * 提交写作异步测评
     *
     * @return
     */
    public String submitAsyncCorrect(String title, String content) {
        WriteRequest request = new WriteRequest();
        request.setTitle(title);
        request.setContent(content);
        request.setRequestId(UUID.randomUUID().toString().replaceAll("-", ""));
        request.setRequestId("rid");

        MediaType mediaType = MediaType.get("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mediaType, JSONObject.toJSONString(request));
        System.out.println("提交异步测评请求参数: " + JSONObject.toJSONString(request));

        Request okHttpRequest = new Request.Builder()
                .url(asyncWriteCorrectUrl)
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
