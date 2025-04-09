import time
import base64
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad

# 加密函数
def encrypt(content, password, iv):
    cipher = AES.new(password.encode(), AES.MODE_CBC, iv)
    # 对内容进行填充
    padded_content = pad(content.encode(), AES.block_size)
    # 加密操作
    encrypted = cipher.encrypt(padded_content)
    # 对加密结果进行 Base64 编码
    return base64.b64encode(encrypted).decode()

# 获取加密文本
def getCipTxt(secret):
    # 检查密钥长度是否符合 AES 要求
    valid_key_lengths = [16, 24, 32]
    if len(secret) not in valid_key_lengths:
        return f"Error: Invalid key length. Key must be 16, 24, or 32 bytes long, got {len(secret)} bytes."
    # 获取当前时间戳（秒）
    timestamp_in_seconds = int(time.time())
    # 转换为字符串
    content = str(timestamp_in_seconds)
    # 初始化向量
    iv = bytes([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16])
    try:
        # 加密函数
        cip_txt = encrypt(content, secret, iv)
    except Exception as ex:
        return f"Encryption error: {ex}"
    return cip_txt
