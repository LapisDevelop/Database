package com.lapisdev.database;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class QueryArray {
    public static <T> @Nullable T single(ArrayList<T> list) {
        if (list.isEmpty()) return null;
        return list.getFirst();
    }
}
