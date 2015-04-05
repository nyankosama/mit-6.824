package com.nyankosama.mapred.worker;

import java.io.Serializable;
import java.util.List;

/**
 * @created: 2015/4/1
 * @author: nyankosama
 * @description:
 */
public class MapredJob implements Serializable{
    private static final long serialVersionUID = -3055189335575425314L;
    int jobID;
    int status; //idle, in-process, completed
    int type; //MapJob ReduceJob

    //TODO 为了方便，只考虑文本输入流
    String filePath; //for mapper and reducer
    List<WorkerInfo> workerInfos; //对于reduceJob需要知道每一个Worker的地址
}
