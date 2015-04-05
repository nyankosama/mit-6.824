package com.nyankosama.mapred.worker;

import java.util.List;

/**
 * @created: 2015/4/1
 * @author: nyankosama
 * @description:
 */
public class MapredConfig {
    /**
     * 1. Master和Worker机器的信息
     * 2. mapred job的数量
     * 3. Map和Reduce用户实现
     * 4. 输入流定义
     */

    String masterAddr;
    List<String> workerAddrs;
    int mapJobNum;
    int reduceJobNum;
    Class<?> mapperClz;
    Class<?> reducerClz;

    String inputFileName; //TODO简洁起见，这里只考虑文本输入流
}
