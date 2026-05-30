package ru.dksu.semantic;

public interface ITestStructure {
//    public void updateElement(int index, int value);
//
//    public void addElement(int index, int add);
//
//    public int getElement(int index);

    public void updateRange(int from, int to, int value);

    public void addRange(int from, int to, int add);

    public int getRangeSum(int from, int to);

    public void clear();

    public int get(int index);
    public int set(int index, int value);
}
