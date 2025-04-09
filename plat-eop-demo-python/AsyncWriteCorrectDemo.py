import requests
import time
import json
import crypto_utils

# 定义异步写作测评类
class AsyncWriteCorrectDemo:
    def __init__(self):
        self.app_id = "git13tz10skijcgmlrv65yc2x"
        self.app_key = "8DACCF71669AD19676AF03F89D288085"
        self.async_correct_url = "https://open-test.unipus.cn/openapi/flaubert/v1/correct"
        self.asyc_correct_result_query_url = "https://open-test.unipus.cn/openapi/flaubert/v1/correct/query"
        self.query_timeout = 120

    # 使用 crypto_utils 中的 getCipTxt 方法
    def get_cip_txt(self):
        # 修改此处，传入 self.app_key 作为 secret 参数
        return crypto_utils.getCipTxt(self.app_key)

    # 提交写作异步测评
    def submit_async_correct(self, title, content):
        request = {
            "title": title,
            "content": content
        }
        headers = {
            "p-app-id": self.app_id,
            "p-cip-txt": self.get_cip_txt()
        }
        try:
            response = requests.post(self.async_correct_url, headers=headers, json=request)
            return response.text
        except Exception as e:
            print("提交异步测评请求异常: ")
            print(e)
            return None

    # 查询异步测评结果
    def query_async_correct(self, correct_id, target_group):
        url = f"{self.asyc_correct_result_query_url}?correctId={correct_id}&targetGroup={target_group}"
        headers = {
            "p-app-id": self.app_id,
            "p-cip-txt": self.get_cip_txt()
        }
        try:
            response = requests.get(url, headers=headers)
            return response.text
        except Exception as e:
            print("查询异步结果异常: ")
            print(e)
            return None

if __name__ == "__main__":
    # 测评参数
    title = "I come from china."
    content = "Hooray! It's snowing! It's time to make a snowman.James runs out. He makes a big pile of snow. He puts a big snowball on top. He adds a scarf and a hat. He adds an orange for the nose. He adds coal for the eyes and buttons.In the evening, James opens the door. What does he see? The snowman is moving! James invites him in. The snowman has never been inside a house. He says hello to the cat. He plays with paper towels.A moment later, the snowman takes James's hand and goes out.They go up, up, up into the air! They are flying! What a wonderful night!The next morning, James jumps out of bed. He runs to the door.He wants to thank the snowman. But he's gone."

    demo = AsyncWriteCorrectDemo()
    async_submit_result_str = demo.submit_async_correct(title, content)
    print(f"异步测评提交结果={async_submit_result_str}")
    try:
        sync_correct_result_json = json.loads(async_submit_result_str)
        correct_id = sync_correct_result_json["value"]["correctId"]
        async_correct_result_str = None
        async_correct_result_json = None

        # 查询测评结果
        count = 1
        while True:
            async_correct_result_str = demo.query_async_correct(correct_id, "0")
            if async_correct_result_str:
                async_correct_result_json = json.loads(async_correct_result_str)
                print(f"第{count}次轮询结果: {async_correct_result_json}")
                if async_correct_result_json["code"] == 0:
                    break
            count += 1
            if count >= demo.query_timeout:
                break
            time.sleep(2)

        print(f"异步测评最终轮询结果={async_correct_result_str}")
        if async_correct_result_json:
            print(f"异步测评评分结果={async_correct_result_json['value']}")
    except KeyError as e:
        print(f"JSON 解析错误，缺少键: {e}")
    except json.JSONDecodeError as e:
        print(f"JSON 解析错误: {e}")