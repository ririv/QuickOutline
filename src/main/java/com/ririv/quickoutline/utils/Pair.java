package com.ririv.quickoutline.utils;


public class Pair<K,V>{
    final K x;
    final V y;

    public Pair(K x, V y) {
        this.x = x;
        this.y = y;
    }

    public K getX() {
        return x;
    }

    public V getY() {
        return y;
    }
}
