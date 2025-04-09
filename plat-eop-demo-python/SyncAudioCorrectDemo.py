import requests
import uuid
import json
from enum import Enum
import base64
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad
import time

# 定义问题类型枚举
class QuesTypeEnum(Enum):
    WORD_ADJUST = 2
    SENTENCE_ADJUST = 4
    PARAGRRAPH = 5
    CHOICE_REC = 7

# 定义常量
APP_ID = "git13tz10skijcgmlrv65yc2x"
APP_KEY = "8DACCF71669AD19676AF03F89D288085"
SYNC_CORRECT_URL = "https://open-test.unipus.cn/openapi/clio/v1/correct/fl"
VALID_KEY_LENGTHS = [16, 24, 32]
IV = bytes([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16])

def get_timestamp():
    """获取当前时间戳（秒）"""
    return int(time.time())

def encrypt(content, password, iv):
    """
    对内容进行AES加密并进行Base64编码
    :param content: 待加密的内容
    :param password: 加密密钥
    :param iv: 初始化向量
    :return: 加密并编码后的字符串
    """
    cipher = AES.new(password.encode(), AES.MODE_CBC, iv)
    padded_content = pad(content.encode(), AES.block_size)
    encrypted = cipher.encrypt(padded_content)
    return base64.b64encode(encrypted).decode()

def get_cip_txt(secret):
    """
    获取加密后的时间戳
    :param secret: 加密密钥
    :return: 加密后的时间戳
    """
    if len(secret) not in VALID_KEY_LENGTHS:
        raise ValueError(f"Invalid key length. Key must be 16, 24, or 32 bytes long, got {len(secret)} bytes.")
    timestamp = str(get_timestamp())
    return encrypt(timestamp, secret, IV)

# 同步音频测评类
class SyncAudioCorrectDemo:
    def __init__(self):
        self.app_id = APP_ID
        self.app_key = APP_KEY
        self.sync_correct_url = SYNC_CORRECT_URL

    def _build_request(self, audio_path, ref_text, user_id, ques_type):
        """
        构建同步音频测评的请求数据
        :param audio_path: 音频文件路径
        :param ref_text: 参考文本
        :param user_id: 用户ID
        :param ques_type: 问题类型
        :return: 请求数据字典
        """
        return {
            "audioPath": audio_path,
            "requestId": str(uuid.uuid4()).replace("-", ""),
            "userId": user_id,
            "quesType": ques_type,
            "requestJson": json.dumps({"refText": ref_text})
        }

    def _build_headers(self):
        """
        构建同步音频测评的请求头
        :return: 请求头字典
        """
        try:
            cip_txt = get_cip_txt(self.app_key)
        except ValueError as ve:
            print(ve)
            return None
        return {
            "p-app-id": self.app_id,
            "p-cip-txt": cip_txt,
            "Content-Type": "application/json; charset=utf-8"
        }

    def sync_correct(self, audio_path, ref_text, user_id, ques_type):
        """
        进行同步音频测评
        :param audio_path: 音频文件路径
        :param ref_text: 参考文本
        :param user_id: 用户ID
        :param ques_type: 问题类型
        :return: 测评结果
        """
        # 构建请求数据
        request = self._build_request(audio_path, ref_text, user_id, ques_type)
        # 打印请求参数
        print(f"请求参数: {json.dumps(request)}")

        # 构建请求头
        headers = self._build_headers()
        if headers is None:
            return None

        try:
            # 发送POST请求
            response = requests.post(self.sync_correct_url, headers=headers, json=request, timeout=(10, 60))
            response.raise_for_status()
            return response.text
        except requests.RequestException as e:
            print(f"同步测评异常: {e}")
            return None

if __name__ == "__main__":
    # 测评参数
    audio_path = "https://itestres.unipus.cn/itest-product/1843/uanswer/eno_32558/1008424602/1740757744000.m4a.mp3"
    ref_text = "I am a student."
    user_id = "uid-123456"
    ques_type = QuesTypeEnum.SENTENCE_ADJUST.value

    demo = SyncAudioCorrectDemo()
    sync_correct_result_str = demo.sync_correct(audio_path, ref_text, user_id, ques_type)
    print(f"同步测评结果={sync_correct_result_str}")
    if sync_correct_result_str:
        sync_correct_result_json = json.loads(sync_correct_result_str)
        print(f"同步测评评分结果={sync_correct_result_json.get('value', {}).get('unifyResult')}")
