import requests
import json
import uuid
import time
from enum import Enum

# 定义问题类型枚举
class QuesTypeEnum(Enum):
    WORD_ADJUST = 2
    SENTENCE_ADJUST = 4
    PARAGRAPH = 5
    CHOICE_REC = 7

# 异步音频测评类
class AsyncAudioCorrectDemo:
    def __init__(self):
        # 应用 ID
        self.app_id = "git13tz10skijcgmlrv65yc2x"
        # 应用密钥
        self.app_key = "8DACCF71669AD19676AF03F89D288085"
        # 异步测评提交 URL
        self.async_correct_url = "https://open-test.unipus.cn/openapi/clio/v1/correct/off/fl"
        # 异步测评结果查询 URL
        self.async_correct_result_query_url = "https://open-test.unipus.cn/openapi/clio/v1/correct/query"
        # 查询超时时间（秒）
        self.query_timeout = 120

    def _get_headers(self):
        """
        生成请求头，包含应用 ID 和加密文本
        :return: 请求头字典
        """
        from crypto_utils import getCipTxt
        return {
            "p-app-id": self.app_id,
            "p-cip-txt": getCipTxt(self.app_key)
        }

    def submit_async_correct(self, audio_path, ref_text, user_id, ques_type, rank):
        """
        提交音频异步测评请求
        :param audio_path: 音频文件路径
        :param ref_text: 参考文本
        :param user_id: 用户 ID
        :param ques_type: 问题类型
        :param rank: 排名
        :return: 响应文本，若请求异常则返回 None
        """
        request_data = {
            "audioPath": audio_path,
            "requestId": str(uuid.uuid4()).replace("-", ""),
            "userId": user_id,
            "quesType": ques_type,
            "requestJson": json.dumps({"refText": ref_text, "rank": rank})
        }
        print("提交异步测评请求参数:", json.dumps(request_data))
        try:
            response = requests.post(self.async_correct_url, headers=self._get_headers(), json=request_data)
            response.raise_for_status()
            return response.text
        except requests.RequestException as e:
            print("提交异步测评请求异常:", e)
            return None

    def query_async_correct(self, correct_id, target_group):
        """
        查询异步测评结果
        :param correct_id: 测评 ID
        :param target_group: 目标组
        :return: 响应文本，若请求异常则返回 None
        """
        url = f"{self.async_correct_result_query_url}?correctId={correct_id}&targetGroup={target_group}"
        try:
            response = requests.get(url, headers=self._get_headers())
            response.raise_for_status()
            return response.text
        except requests.RequestException as e:
            print("查询异步结果异常:", e)
            return None

# 主函数
def main():
    # 测评参数
    audio_path = "https://cdn.unischool.cn/ulearning/app/audio/ai/9382_1741739868827.wav"
    ref_text = "I come from china."
    user_id = "uid-123456"
    ques_type = QuesTypeEnum.PARAGRAPH.value  # 使用枚举值
    rank = 100

    demo = AsyncAudioCorrectDemo()
    # 提交异步测评请求
    async_submit_result_str = demo.submit_async_correct(audio_path, ref_text, user_id, ques_type, rank)
    print("异步测评提交结果=", async_submit_result_str)
    try:
        # 解析提交结果
        sync_correct_result_json = json.loads(async_submit_result_str)
        correct_id = sync_correct_result_json["value"]["correctId"]
        async_correct_result_str = None
        async_correct_result_json = None

        # 查询测评结果
        for count in range(1, demo.query_timeout + 1):
            async_correct_result_str = demo.query_async_correct(correct_id, "0")
            if async_correct_result_str:
                async_correct_result_json = json.loads(async_correct_result_str)
                print(f"第{count}次轮询结果: {async_correct_result_json}")
                if async_correct_result_json["code"] == 0:
                    break
            time.sleep(2)
        print("异步测评最终轮询结果=", async_correct_result_str)
        if async_correct_result_json:
            print("异步测评评分结果=", async_correct_result_json["value"]["result"])
    except (KeyError, json.JSONDecodeError) as e:
        print("处理结果时出现错误:", e)

if __name__ == "__main__":
    main()
