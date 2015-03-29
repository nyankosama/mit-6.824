package com.nyankosama.test;

import com.nyankosama.base.ResultSet;

/**
 * @created: 2015/3/28
 * @author: nyankosama
 * @description:
 */
public class CommonTest {

    public static void main(String[] args) {
        ResultSet<String, String> set = new ResultSet<>("success", false, "failed!");
        if (!set.handleError(System.out::println)) {
            System.out.println(set.get());
        }
    }
}
