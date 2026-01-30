#!/bin/bash

thread="1 2 4 8 16 24 31"
# thread="32"
# thread="1 4 8"
# size="4194304"
size="67108864"

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
benchs="ru.dksu.semantic.TestStructure ru.dksu.semantic.TestStructureRW ru.dksu.semantic.TestStructureSimple" # ru.dksu.semantic.TestStructureRWSL"
# benchs="ru.dksu.semantic.TestStructureSimple"
# for s in ${snapshots}; do
for t in ${thread}; do
    # for write in ${writes}; do
  for bench in ${benchs}; do
      # u=$((((100-$s)*$write)/100))
    for i in ${size}; do
        # r=$((i*2))
      r=$i
      out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/290126/${bench}-size-${i}-threads-${t}-33-33-34.log
      echo "numactl -p 0 taskset -c 0-15,64-79 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -d 10000 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} --distribution 5 5 --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/results-compare-simple.csv >> ${out}"
      numactl -p 0 taskset -c 0-15,64-79 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -d 7500 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} --distribution "5 5" --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/results-compare-simple.csv >> ${out}
    done
  done
done
