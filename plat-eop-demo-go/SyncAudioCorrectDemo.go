package main

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"time"

	"github.com/google/uuid"
)

// AudioRequest 定义音频请求结构体
type AudioRequest struct {
	AudioPath   string `json:"audioPath"`
	RequestId   string `json:"requestId"`
	UserId      string `json:"userId"`
	QuesType    int    `json:"quesType"`
	RequestJson string `json:"requestJson"`
}

// QuesTypeEnum 定义问题类型枚举
type QuesTypeEnum int

const (
	SENTENCE_ADJUST QuesTypeEnum = 4
)

const (
	appId          = "git13tz10skijcgmlrv65yc2x"
	appKey         = "8DACCF71669AD19676AF03F89D288085"
	syncCorrectUrl = "https://open-test.unipus.cn/openapi/clio/v1/correct/fl"
)

var iv = []byte{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}

// getCipTxt 获取加密后的时间戳
func getCipTxt(secret string) (string, error) {
	timestamp := fmt.Sprintf("%d", time.Now().Unix())
	return encrypt(timestamp, secret)
}

// encrypt 加密函数
func encrypt(content, password string) (string, error) {
	block, err := aes.NewCipher([]byte(password))
	if err != nil {
		return "", err
	}

	paddedContent := pad([]byte(content), aes.BlockSize)
	mode := cipher.NewCBCEncrypter(block, iv)
	ciphertext := make([]byte, len(paddedContent))
	mode.CryptBlocks(ciphertext, paddedContent)

	return base64.StdEncoding.EncodeToString(ciphertext), nil
}

// pad 添加填充字节
func pad(data []byte, blockSize int) []byte {
	padding := blockSize - len(data)%blockSize
	return append(data, bytes.Repeat([]byte{byte(padding)}, padding)...)
}

// sendRequest 发送 HTTP 请求
func sendRequest(url string, request interface{}) (string, error) {
	requestBytes, err := json.Marshal(request)
	if err != nil {
		return "", err
	}

	client := &http.Client{Timeout: 60 * time.Second}
	req, err := http.NewRequest("POST", url, bytes.NewBuffer(requestBytes))
	if err != nil {
		return "", err
	}

	req.Header.Set("Content-Type", "application/json; charset=utf-8")
	req.Header.Set("p-app-id", appId)

	// 获取加密后的时间戳
	cipTxt, err := getCipTxt(appKey)
	if err != nil {
		return "", err
	}
	req.Header.Set("p-cip-txt", cipTxt)

	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	return string(body), nil
}

// syncCorrect 音频同步测评函数
func syncCorrect(audioPath, refText, userId string, quesType QuesTypeEnum) (string, error) {
	requestID := uuid.New().String()
	// 修正：将 bytes.ReplaceAll 的结果转换为 string 类型
	requestID = string(bytes.ReplaceAll([]byte(requestID), []byte("-"), []byte("")))

	requestJson := map[string]string{"refText": refText}
	requestJsonBytes, err := json.Marshal(requestJson)
	if err != nil {
		return "", err
	}

	request := AudioRequest{
		AudioPath:   audioPath,
		RequestId:   requestID,
		UserId:      userId,
		QuesType:    int(quesType),
		RequestJson: string(requestJsonBytes),
	}

	return sendRequest(syncCorrectUrl, request)
}

func main() {
	audioPath := "https://itestres.unipus.cn/itest-product/1843/uanswer/eno_32558/1008424602/1740757744000.m4a.mp3"
	refText := "I am a student."
	userId := "uid-123456"
	quesType := SENTENCE_ADJUST

	result, err := syncCorrect(audioPath, refText, userId, quesType)
	if err != nil {
		fmt.Println("同步测评异常:", err)
		return
	}

	fmt.Println("同步测评结果=", result)

	var parsedResult map[string]interface{}
	if err := json.Unmarshal([]byte(result), &parsedResult); err != nil {
		fmt.Println("解析 JSON 结果异常:", err)
		return
	}

	if value, ok := parsedResult["value"].(map[string]interface{}); ok {
		if unifyResult, ok := value["unifyResult"].(string); ok {
			fmt.Println("同步测评评分结果=", unifyResult)
		}
	}
}
