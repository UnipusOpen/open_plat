package main

import (
	"fmt"
	"io/ioutil"
	"net/http"
)

func main() {
	// 定义请求的 URL
	url := "https://www.baidu.com"
	// 发送 GET 请求
	resp, err := http.Get(url)
	if err != nil {
		fmt.Println("请求出错:", err)
		return
	}
	// 确保在函数结束时关闭响应体
	defer resp.Body.Close()

	// 读取响应体
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		fmt.Println("读取响应出错:", err)
		return
	}

	// 打印响应状态码和响应体
	fmt.Println("状态码:", resp.StatusCode)
	fmt.Println("响应内容:", string(body))
}
