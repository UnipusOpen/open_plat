package main

import (
	"bytes"
	"crypto/aes"
	"crypto/cipher"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"time"
)

// AudioRequest 定义音频请求结构体
type AudioRequest struct {
	AudioPath   string `json:"audioPath"`
	RequestId   string `json:"requestId"`
	UserId      string `json:"userId"`
	QuesType    int    `json:"quesType"`
	RequestJson string `json:"requestJson"`
}

// 定义常量
const (
	appId                     = "git13tz10skijcgmlrv65yc2x"
	appKey                    = "8DACCF71669AD19676AF03F89D288085"
	asyncCorrectUrl           = "https://open-test.unipus.cn/openapi/clio/v1/correct/off/fl"
	asycCorrectResultQueryUrl = "https://open-test.unipus.cn/openapi/clio/v1/correct/query"
	queryTimeout              = 120
)

// 初始化向量
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
	padtext := bytes.Repeat([]byte{byte(padding)}, padding)
	return append(data, padtext...)
}

// sendRequest 统一发送 HTTP 请求的函数
func sendRequest(method, url string, body []byte) (string, error) {
	req, err := http.NewRequest(method, url, bytes.NewBuffer(body))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("p-app-id", appId)

	cipTxt, err := getCipTxt(appKey)
	if err != nil {
		return "", err
	}
	req.Header.Set("p-cip-txt", cipTxt)

	client := &http.Client{
		Timeout: 10 * time.Second,
	}
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	respBody, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}

	return string(respBody), nil
}

// submitAsyncCorrect 提交音频异步测评
func submitAsyncCorrect(audioPath, refText, userId string, quesType, rank int) (string, error) {
	requestId := fmt.Sprintf("%v", time.Now().UnixNano())
	requestJson := map[string]interface{}{
		"refText": refText,
		"rank":    rank,
	}
	requestJsonBytes, err := json.Marshal(requestJson)
	if err != nil {
		return "", err
	}

	request := AudioRequest{
		AudioPath:   audioPath,
		RequestId:   requestId,
		UserId:      userId,
		QuesType:    quesType,
		RequestJson: string(requestJsonBytes),
	}

	requestBytes, err := json.Marshal(request)
	if err != nil {
		return "", err
	}

	return sendRequest("POST", asyncCorrectUrl, requestBytes)
}

// queryAsyncCorrect 查询异步测评结果
func queryAsyncCorrect(correctId, targetGroup string) (string, error) {
	url := fmt.Sprintf("%s?correctId=%s&targetGroup=%s", asycCorrectResultQueryUrl, correctId, targetGroup)
	return sendRequest("GET", url, nil)
}

// parseCorrectId 从异步测评提交结果中解析 correctId
func parseCorrectId(resultStr string) (string, error) {
	var result map[string]interface{}
	err := json.Unmarshal([]byte(resultStr), &result)
	if err != nil {
		return "", err
	}

	value, ok := result["value"].(map[string]interface{})
	if !ok {
		return "", fmt.Errorf("无法获取 correctId")
	}

	correctId, ok := value["correctId"].(string)
	if !ok {
		return "", fmt.Errorf("无法获取 correctId")
	}

	return correctId, nil
}

// waitForAsyncResult 等待异步测评结果
func waitForAsyncResult(correctId string) (string, error) {
	var asyncCorrectResultStr string
	var asyncCorrectResultJson map[string]interface{}

	for i := 1; i <= queryTimeout; i++ {
		result, err := queryAsyncCorrect(correctId, "0")
		if err != nil {
			fmt.Println("查询异步结果异常:", err)
			continue
		}
		if result != "" {
			err = json.Unmarshal([]byte(result), &asyncCorrectResultJson)
			if err != nil {
				fmt.Println("解析异步测评结果异常:", err)
				continue
			}
			fmt.Printf("第 %d 次轮询结果: %v\n", i, asyncCorrectResultJson)
			if code, ok := asyncCorrectResultJson["code"].(float64); ok && code == 0 {
				asyncCorrectResultStr = result
				break
			}
		}
		time.Sleep(2 * time.Second)
	}

	return asyncCorrectResultStr, nil
}

// parseFinalResult 从异步测评最终结果中解析评分结果
func parseFinalResult(resultStr string) (string, error) {
	var resultJson map[string]interface{}
	err := json.Unmarshal([]byte(resultStr), &resultJson)
	if err != nil {
		return "", err
	}

	value, ok := resultJson["value"].(map[string]interface{})
	if !ok {
		return "", fmt.Errorf("无法获取评分结果")
	}

	result, ok := value["result"].(string)
	if !ok {
		return "", fmt.Errorf("无法获取评分结果")
	}

	return result, nil
}

func main() {
	// 测评参数
	audioPath := "https://cdn.unischool.cn/ulearning/app/audio/ai/9382_1741739868827.wav"
	refText := "I come from china."
	userId := "uid-123456"
	quesType := 5 // 假设 PARAGRRAPH 的 quesType 为 1
	rank := 100

	// 提交异步测评请求
	asyncSubmitResultStr, err := submitAsyncCorrect(audioPath, refText, userId, quesType, rank)
	if err != nil {
		fmt.Println("提交异步测评请求异常:", err)
		return
	}
	fmt.Println("异步测评提交结果=", asyncSubmitResultStr)

	// 解析 correctId
	correctId, err := parseCorrectId(asyncSubmitResultStr)
	if err != nil {
		fmt.Println(err)
		return
	}

	// 等待异步测评结果
	asyncCorrectResultStr, err := waitForAsyncResult(correctId)
	if err != nil {
		fmt.Println("等待异步测评结果异常:", err)
		return
	}
	fmt.Println("异步测评最终轮询结果=", asyncCorrectResultStr)

	// 解析最终评分结果
	resultStr, err := parseFinalResult(asyncCorrectResultStr)
	if err != nil {
		fmt.Println("解析最终评分结果异常:", err)
		return
	}
	fmt.Println("异步测评评分结果=", resultStr)
}
