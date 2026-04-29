#!/bin/bash

thread="1 2 4 8 16 31"
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
#benchs="ru.dksu.semantic.ARATestStructure ru.dksu.semantic.ARATestStructureRW ru.dksu.semantic.ARATestStructureWithout" # ru.dksu.semantic.TestStructureRWSL"
# benchs="ru.dksu.semantic.TestStructureSimple"
#benchs="ru.dksu.semantic.TestStructureShortLong ru.dksu.semantic.TestStructureShortLongRW" # ru.dksu.semantic.ARATestStructureWithout" # ru.dksu.semantic.TestStructureRWSL"
#benchs="ru.dksu.semantic.TestStructure ru.dksu.semantic.TestStructureRW ru.dksu.semantic.TestStructureSimple ru.dksu.semantic.TestStructureShortLong ru.dksu.semantic.TestStructureShortLongRW"
# for s in ${snapshots}; do
#benchs="ru.dksu.semantic.TestStructure"# ru.dksu.semantic.TestStructureRW ru.dksu.semantic.TestStructureSimple ru.dksu.semantic.TestStructureWithout"
benchs="ru.dksu.semantic.ExtendedMapRW ru.dksu.semantic.ExtendedMapSL3 ru.dksu.semantic.ExtendedMapSL ru.dksu.semantic.ExtendedMapNoLock"
#benchs="trees.lockbased.IntegerCollaborativeHelperFairLockBasedStanfordTreeMap trees.lockbased.IntegerLockBasedStanfordTreeMap"

distrs=(
  "0 100 0"
  "100 0 0"
  "50 50 0"
#  "49 49 2"
#  "49 49 0"
#  "49 49 1"
#  "48 48 4"
#  "48 48 0"
#  "48 48 2"
#  "46 46 8"
#  "46 46 0"
#  "46 46 4"
#  "42 42 16"
#  "42 42 0"
#  "42 42 8"
#  "34 34 32"
#  "34 34 0"
#  "34 34 16"
#  "18 18 64"
#  "18 18 0"
#  "18 18 32"
#  "100 0 0"
#  "80 20 0"
#  "60 40 0"
#  "40 60 0"
#  "20 80 0"
#  "24 74 2"
#  "24 74 1"
#  "24 74 0"
#  "24 72 4"
#  "24 72 2"
#  "24 72 0"
#  "23 69 8"
#  "23 69 4"
#  "23 69 0"
#  "21 63 16"
#  "21 63 8"
#  "21 63 0"
#  "17 51 32"
#  "17 51 16"
#  "17 51 0"
#  "9 27 64"
#  "9 27 32"
#  "9 27 0"
)

count=0
for dist in "${distrs[@]}"; do
  for i in ${size}; do
    for t in ${thread}; do
      for bench in ${benchs}; do
        count=$((count+1))
      done
    done
  done
done

current=0
for dist in "${distrs[@]}"; do
  for i in ${size}; do
    r=$((i*2))
    for t in ${thread}; do
      for bench in ${benchs}; do
        current=$((current+1))
        out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/290126/${bench}-size-${i}-threads-${t}-33-33-34.log
        date
        echo "Experiment $current of $count"
        echo "numactl -p 3 taskset -c 48-63,112-143 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -d 7500 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} --distribution ${dist} --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/results-compare-simple.csv >> ${out}"
        numactl -p 3 taskset -c 48-63,112-143 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 4 -a 0 -d 5000 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} --distribution "${dist}" --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/results-compare-simple.csv >> ${out}
      done
    done
  done
done
