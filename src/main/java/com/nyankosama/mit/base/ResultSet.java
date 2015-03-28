package com.nyankosama.mit.base;

/**
 * @created: 2015/3/28
 * @author: nyankosama
 * @description: 替代掉checked-exception，本项目尽量不使用checked-exception
 */
public class ResultSet<T, E> {
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

    public void handleError(ErrorProcess<E> proc) {
        if (!isSucceed) proc.process(error);
    }
}
