package com.nyankosama.mapred.worker;

import java.io.Serializable;

/**
 * @created: 2015/4/1
 * @author: nyankosama
 * @description:
 */
public class WorkerInfo implements Serializable{
    private static final long serialVersionUID = 4115848690782314054L;
    int workerID;
    String ip;
    int port;
    transient Worker workerHandler; //避免序列化

    public WorkerInfo(int workerID, String ip, int port) {
        this.workerID = workerID;
        this.ip = ip;
        this.port = port;
    }
}
