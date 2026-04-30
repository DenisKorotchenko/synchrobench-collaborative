package hashtables.lockfree;

import java.util.ArrayList;

import ru.dksu.semantic.ICollaborativeMap;

public class IntegerBaseCliffMap extends NonBlockingCliffHashMap<Integer, Integer> implements ICollaborativeMap {
    @Override
    public ArrayList<Entry<Integer, Integer>> snapshot() {
        return new ArrayList<>();
    }

    @Override
    public Integer sum() {
        return 0;
    }
}
