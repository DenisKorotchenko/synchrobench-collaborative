package hashtables.lockfree;

import java.util.ArrayList;

import ru.dksu.semantic.ICollaborativeMap;

public class IntegerCliffMap extends ExtendedNonBlockingCliffHashMap<Integer, Integer> implements ICollaborativeMap {

    @Override
    public Integer rangeSum(Integer left, Integer right) {
        return 0;
    }

    @Override
    public Integer rangeCap(Integer left, Integer right, Integer maxValue) {
        return 0;
    }
}
