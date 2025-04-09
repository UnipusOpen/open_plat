package cn.unipus.plat.client.dto;

import lombok.Data;

/**
 * 打分参数
 **/
@Data
public class AudioRequest {

    // 音频路径
    private String audioPath;

    // 请求id
    private String requestId;

    // 请求json
    private String requestJson;

    // 用户id
    private String userId;

    // 题型
    private Integer quesType;

    // 适用人群: 0-成人, 1-少儿, 默认0
    private String targetGroup = "0";
}
