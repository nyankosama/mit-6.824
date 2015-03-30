package com.nyankosama.rpc;

import com.nyankosama.base.ResultSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @created: 2015/3/29
 * @author: nyankosama
 * @description:
 */
public class ServiceHandler {
    private static Logger logger = LogManager.getLogger(ServiceHandler.class);
    private Object serviceInstance;

    public ServiceHandler(Object serviceInstance) {
        this.serviceInstance = serviceInstance;
    }

    public ResultSet tryInvoke(String methodName, Class<?>[] parameterTypes, Object[] arguments) {
        Method method;
        ResultSet result;
        try {
            method = serviceInstance.getClass().getMethod(methodName, parameterTypes);
            Object ret = method.invoke(serviceInstance, arguments);
            if (!(ret instanceof ResultSet)) {
                logger.error("method invocated doesn't return the type ResultSet!");
                return new ResultSet<>(null, false, "method invocated doesn't return the type ResultSet!");
            }
            result = (ResultSet) ret;
        } catch (NoSuchMethodException e) {
            logger.error("wrong invocation! no such method!");
            return new ResultSet<>(null, false, "wrong invocation! no such method!");
        } catch (InvocationTargetException e) {
            logger.error("the method invocated throws an exception.");
            return new ResultSet<>(null, false, "the method invocated throws an exception.");
        } catch (IllegalAccessException e) {
            logger.error("target methods cannot be accessed.");
            return new ResultSet<>(null, false, "target methods cannot be accessed.");
        }
        return result;
    }
}
