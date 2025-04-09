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
	"strconv"
	"time"
)

// WriteRequest 定义请求结构体
type WriteRequest struct {
	Title   string `json:"title"`
	Content string `json:"content"`
}

// 常量定义
const (
	appId                     = "git13tz10skijcgmlrv65yc2x"
	appKey                    = "8DACCF71669AD19676AF03F89D288085"
	asyncCorrectUrl           = "https://open-test.unipus.cn/openapi/flaubert/v1/correct"
	asycCorrectResultQueryUrl = "https://open-test.unipus.cn/openapi/flaubert/v1/correct/query"
	queryTimeout              = 120
	requestTimeout            = 10 * time.Second
	pollingInterval           = 2 * time.Second
)

var iv = []byte{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}

// getCipTxt 获取加密后的时间戳
func getCipTxt(secret string) (string, error) {
	timestampInSeconds := time.Now().Unix()
	return encrypt(fmt.Sprintf("%d", timestampInSeconds), secret)
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

// submitAsyncCorrect 提交写作异步测评
func submitAsyncCorrect(title, content string) (string, error) {
	request := WriteRequest{
		Title:   title,
		Content: content,
	}
	requestBody, err := json.Marshal(request)
	if err != nil {
		return "", err
	}

	req, err := http.NewRequest("POST", asyncCorrectUrl, bytes.NewBuffer(requestBody))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/json; charset=utf-8")
	req.Header.Set("p-app-id", appId)
	// 修改：接收两个返回值并处理错误
	cipTxt, err := getCipTxt(appKey)
	if err != nil {
		return "", err
	}
	req.Header.Set("p-cip-txt", cipTxt)

	client := &http.Client{Timeout: requestTimeout}
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}
	return string(body), nil
}

// queryAsyncCorrect 查询异步测评结果
func queryAsyncCorrect(correctId, targetGroup string) (string, error) {
	url := fmt.Sprintf("%s?correctId=%s&targetGroup=%s", asycCorrectResultQueryUrl, correctId, targetGroup)
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return "", err
	}
	req.Header.Set("p-app-id", appId)
	// 修改：接收两个返回值并处理错误
	cipTxt, err := getCipTxt(appKey)
	if err != nil {
		return "", err
	}
	req.Header.Set("p-cip-txt", cipTxt)

	client := &http.Client{Timeout: requestTimeout}
	resp, err := client.Do(req)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()

	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return "", err
	}
	return string(body), nil
}

func main() {
	// 测评参数
	title := "I come from china."
	content := "Hooray! It's snowing! It's time to make a snowman.James runs out. He makes a big pile of snow. He puts a big snowball on top. He adds a scarf and a hat. He adds an orange for the nose. He adds coal for the eyes and buttons.In the evening, James opens the door. What does he see? The snowman is moving! James invites him in. The snowman has never been inside a house. He says hello to the cat. He plays with paper towels.A moment later, the snowman takes James's hand and goes out.They go up, up, up into the air! They are flying! What a wonderful night!The next morning, James jumps out of bed. He runs to the door.He wants to thank the snowman. But he's gone."

	// 提交异步测评
	asyncSubmitResultStr, err := submitAsyncCorrect(title, content)
	if err != nil {
		fmt.Println("提交异步测评请求异常:", err)
		return
	}
	fmt.Println("异步测评提交结果=", asyncSubmitResultStr)

	var syncCorrectResult map[string]interface{}
	err = json.Unmarshal([]byte(asyncSubmitResultStr), &syncCorrectResult)
	if err != nil {
		fmt.Println("解析异步测评提交结果异常:", err)
		return
	}
	value, ok := syncCorrectResult["value"].(map[string]interface{})
	if !ok {
		fmt.Println("未找到 value 字段")
		return
	}
	correctId, ok := value["correctId"].(string)
	if !ok {
		fmt.Println("未找到 correctId 字段")
		return
	}

	var asyncCorrectResultStr string
	var asyncCorrectResult map[string]interface{}

	// 查询测评结果
	count := 1
	for {
		asyncCorrectResultStr, err = queryAsyncCorrect(correctId, "0")
		if err != nil {
			fmt.Println("查询异步结果异常:", err)
		}
		if asyncCorrectResultStr != "" {
			err = json.Unmarshal([]byte(asyncCorrectResultStr), &asyncCorrectResult)
			if err != nil {
				fmt.Println("解析异步测评查询结果异常:", err)
			}
			fmt.Println("第"+strconv.Itoa(count)+"次轮询结果:", asyncCorrectResult)
			if code, ok := asyncCorrectResult["code"].(float64); ok && code == 0 {
				break
			}
		}
		count++
		if count >= queryTimeout {
			break
		}
		time.Sleep(pollingInterval)
	}
	fmt.Println("异步测评最终轮询结果=", asyncCorrectResultStr)
	if asyncCorrectResult != nil {
		if value, ok := asyncCorrectResult["value"].(map[string]interface{}); ok {
			fmt.Println("异步测评评分结果=", value)
		}
	}
}
