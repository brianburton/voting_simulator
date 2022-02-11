package com.burtonzone.common;

import static org.javimmutable.collections.util.JImmutables.*;

import org.javimmutable.collections.JImmutableList;

public class DataUtils
{
    public static <T> JImmutableList<JImmutableList<T>> combos(JImmutableList<T> source,
                                                               int length)
    {
        JImmutableList<JImmutableList<T>> answer = list();
        for (int i = 0; i <= source.size() - length; ++i) {
            final var first = source.get(i);
            if (length > 1) {
                for (var suffix : combos(source.slice(i + 1, -1), length - 1)) {
                    answer = answer.insertLast(suffix.insertFirst(first));
                }
            } else {
                answer = answer.insertLast(list(first));
            }
        }
        return answer;
    }

    public static int wrap(int value,
                           int minValue,
                           int maxValue)
    {
        if (value < minValue) {
            value = maxValue - (minValue - value);
        } else if (value > maxValue) {
            value = minValue + (value - maxValue);
        }
        assert value >= minValue;
        assert value <= maxValue;
        return value;
    }
}
