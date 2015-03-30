package com.nyankosama.test.dubbo;

/**
 * @created: 2015/3/30
 * @author: nyankosama
 * @description:
 */
public class HelloIpml implements Hello{
    @Override
    public String sayHi(String str) {
        return "Hi" + str;
    }
}
