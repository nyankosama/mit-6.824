package com.nyankosama.base;

import java.io.Serializable;

/**
 * @created: 2015/3/28
 * @author: nyankosama
 * @description: 替代掉checked-exception，本项目尽量不使用checked-exception
 */
public class ResultSet<T, E> implements Serializable{
    private static final long serialVersionUID = 5617363210683577733L;
    private T result;
    private boolean isSucceed;
    private E error;


    public ResultSet(T result) {
        this.result = result;
        this.isSucceed = true;
    }

    public ResultSet(T result, boolean isSucceed, E error) {
        this.result = result;
        this.isSucceed = isSucceed;
        this.error = error;
    }

    public T get() {
        return result;
    }

    public boolean isSucceed() {
        return isSucceed;
    }

    public E error() {
        return error;
    }

    /**
     * 传入lambda函数处理错误
     * @param proc
     * @return 返回是否运行错误处理，如果未运行则代表上一次调用成功
     */
    public boolean handleError(ErrorProcess<E> proc) {
        if (!isSucceed) proc.process(error);
        return !isSucceed;
    }
}
