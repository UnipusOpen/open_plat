package cn.unipus.plat.demo.dto;

import lombok.Data;

/**
 * 写作引擎请求
 */
@Data
public class WriteRequest {

    // 文章标题
    private String title;

    // 文章内容,不能为空
    private String content;

    // 文章关键词，多个用','分割
    private String keywords;

    // 文章字数下限
    private Integer wordLimitMin;

    // 文章字数上限
    private Integer wordLimitMax;

    // 请求方业务Id
    private String requestId;

    // 文章提示标题
    private String promptTitle;

    // 文章提示内容
    private String promptContent;

    // 默认false.是否进行base64编码，若为true，会对title,content,promptTitle参数进行base64编码
    private boolean base64Enc;

    // 回调地址，默认使用应用开通时设置的回调地址
    private String callbackUrl;
}
