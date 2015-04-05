package com.nyankosama.mapred.worker;

import com.nyankosama.base.ResultSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @created: 2015/4/1
 * @author: nyankosama
 * @description:
 */
public class MasterManager implements Master{
    private static final int DEFAULT_SINGLETON_WORKER_NUM = 10;
    private List<WorkerInfo> workerList = new ArrayList<>();
    //等待分派的job队列
    private Queue<MapredJob> idleJobQueue = new ConcurrentLinkedQueue<>();
    //正在执行的job队列
    private Queue<MapredJob> inProcessJobQueue = new ConcurrentLinkedDeque<>();
    //已经分派的（包括正在执行以及执行完毕）的job队列
    private Map<Integer, List<MapredJob>> dispatchedJobMap = new ConcurrentHashMap<>();

    private String masterIP;
    private int masterPort;
    private int mapJobNum;
    private int reduceJobNum;
    private Class<?> mapperClz;
    private Class<?> reduceClz;

    private MasterManager(final MapredConfig config) {
        String[] addrSlit = config.masterAddr.split(":");
        masterIP = addrSlit[0];
        masterPort = Integer.parseInt(addrSlit[1]);
        mapJobNum = config.mapJobNum;
        reduceJobNum = config.reduceJobNum;
        mapperClz = config.mapperClz;
        reduceClz = config.reducerClz;
        for (String addr : config.workerAddrs) {
            addrSlit = addr.split(":");
            //TODO
        }
    }

    public static MasterManager makeDistributedMapred(MapredConfig config) {
        //TODO
        return null;
    }

    /**
     * Master调用，开始工作
     */
    public void start() {
        /**
         * TODO
         * - split files for map
         * - apply map job
         * - wait map job to be completed
         * - apply reduce job
         * - wait reduce job to be completed
         * - merge the reduce files
         */
    }

    private void splitFiles() {

    }


    @Override
    public ResultSet<Boolean, Throwable> replyJobResults(List<MapredJob> jobs) {
        //TODO
        return null;
    }
}
