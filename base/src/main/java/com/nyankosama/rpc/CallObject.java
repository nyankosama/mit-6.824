package com.nyankosama.rpc;

import java.io.Serializable;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
public class CallObject implements Serializable{
    private static final long serialVersionUID = -4416093517562038659L;

    long threadId;
    String interfaceName;
    String methodName;
    Class<?>[] parameterTypes;
    Object[] arguments;

    public CallObject() {}

    public CallObject(long threadId, String interfaceName, String methodName, Class<?>[] parameterTypes, Object[] arguments) {
        this.threadId = threadId;
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.arguments = arguments;
    }
}
