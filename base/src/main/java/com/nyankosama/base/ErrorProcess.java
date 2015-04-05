package com.nyankosama.base;

/**
 * @created: 2015/3/28
 * @author: nyankosama
 * @description:
 */
@FunctionalInterface
public interface ErrorProcess<E> {
    public abstract void process(E e);
}
