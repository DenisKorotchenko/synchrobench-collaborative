package ru.dksu.semantic;

import java.util.ArrayList;
import java.util.Map;

public interface ICollaborativeMap extends Map<Integer, Integer> {
    ArrayList<Entry<Integer, Integer>> snapshot();
    Integer sum();
    Integer cap(Integer maxValue);
    Integer rangeSum(Integer left, Integer right);
    Integer rangeCap(Integer left, Integer right, Integer maxValue);
}
