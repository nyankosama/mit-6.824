package com.nyankosama.rpc;

import com.nyankosama.base.ResultSet;

import java.io.Serializable;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
public class ResultObject implements Serializable{
    private static final long serialVersionUID = 4081298170087827067L;
    long threadId;
    ResultSet resultSet;

    public ResultObject(long threadId, ResultSet resultSet) {
        this.threadId = threadId;
        this.resultSet = resultSet;
    }
}
