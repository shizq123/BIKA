package com.shizq.bika.network.base;

public class BaseResponse<T> {

    private int code;   // 服务器返回的状态码
    private String message; // 服务器返回的状态信息
    private String error; // 服务器返回的异常code信息
    private T data; // 服务器返回的数据封装
    //val detail: String

    /*
     * 不同公司是否代表成功的code不同, 也许变量名也不同,这里用isOK来封装,根据自己公司情况进行判断
     */
    private boolean isOk;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * 网络返回
     *
     * @return 自己公司服务器代表成功返回的唯一字段, 我们用isOk来封装
     */
    public boolean isOk() {
        return code == 200&&data!=null;
    }

}
