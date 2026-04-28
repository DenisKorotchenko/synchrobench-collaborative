#!/bin/bash

thread="1 2 4 8 16 24 31"
# thread="32"
# thread="1 4 8"
size="4194304"
#size="67108864"
#size="8192"

#writes="0 20 50 100"
# writes="20"

###############################
# records all benchmark outputs
###############################
#snapshots="0 1 2 4 8 16 32 64 100"
# snapshots="8"
#benchs="trees.lockbased.IntegerCollaborativeLockBasedStanfordTreeMap trees.lockbased.IntegerLockBasedStanfordTreeMap" #trees.lockbased.IntegerPureLockBasedStanfordTreeMap"
# benchs="hashtables.lockbased.IntegerCollaborativeQueueHashMap hashtables.lockbased.IntegerQueueHashMap hashtables.lockbased.IntegerNonCollaborativeQueueHashMap"
# benchs="hashtables.lockbased.ReduceCollaborative hashtables.lockbased.ReduceSimple hashtables.lockbased.ReduceUnlinearizable"
#benchs="ru.dksu.semantic.TestStructure ru.dksu.semantic.TestStructureRW ru.dksu.semantic.TestStructureSimple" # ru.dksu.semantic.TestStructureRWSL"
# benchs="ru.dksu.semantic.TestStructureSimple"
#benchs="ru.dksu.semantic.ARATestStructure ru.dksu.semantic.ARATestStructureRW ru.dksu.semantic.ARATestStructureWithout"
#benchs="ru.dksu.semantic.TestStructure ru.dksu.semantic.TestStructureRW ru.dksu.semantic.TestStructureSimple
#benchs="ru.dksu.semantic.TestStructureShortLong ru.dksu.semantic.TestStructureShortLongRW"
#benchs="ru.dksu.semantic.TestStructure" #ru.dksu.semantic.TestStructureRW ru.dksu.semantic.TestStructureSimple ru.dksu.semantic.TestStructureWithout"
# for s in ${snapshots}; do
#benchs="ru.dksu.semantic.ExtendedMapRW ru.dksu.semantic.ExtendedMapSL3 ru.dksu.semantic.ExtendedMapSL ru.dksu.semantic.ExtendedMapNoLock"
benchs="trees.lockbased.IntegerCollaborativeHelperFairLockBasedStanfordTreeMap trees.lockbased.IntegerLockBasedStanfordTreeMap"

distrs=(
  "42 42 16"
)

for dist in "${distrs[@]}"; do
  for i in ${size}; do
    r=$((i*2))
    for t in ${thread}; do
      for bench in ${benchs}; do
        out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/290126/${bench}-size-${i}-threads-${t}-33-33-34.log
        date
        echo "numactl -p 0 taskset -c 0-15,64-79 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -d 7500 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} --distribution ${dist} --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/results-compare-simple.csv >> ${out}"
        numactl -p 1 taskset -c 16-31,80-95 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -d 7500 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} --distribution "${dist}" --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/results-compare-simple.csv >> ${out}
      done
    done
  done
done
