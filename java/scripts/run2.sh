#!/bin/bash

thread="1 2 4 8 15 31 47 63"
# thread="1 4 8"
size="1000000"

writes="0 20 50 100"
# writes="20"

###############################
# records all benchmark outputs
###############################
snapshots="0 1 2 4 8 16 32 64 100"
# snapshots="8"
#benchs="trees.lockbased.IntegerCollaborativeLockBasedStanfordTreeMap trees.lockbased.IntegerLockBasedStanfordTreeMap" #trees.lockbased.IntegerPureLockBasedStanfordTreeMap"
# benchs="hashtables.lockbased.IntegerCollaborativeQueueHashMap hashtables.lockbased.IntegerQueueHashMap hashtables.lockbased.IntegerNonCollaborativeQueueHashMap"
benchs="hashtables.lockbased.ReduceCollaborative hashtables.lockbased.ReduceSimple hashtables.lockbased.ReduceUnlinearizable"
for s in ${snapshots}; do
  for t in ${thread}; do
    for write in ${writes}; do
      for bench in ${benchs}; do
        u=$((((100-$s)*$write)/100))
        for i in ${size}; do
          r=$((i*2))
          out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/071225/${bench}-i${i}-u${u}-s${s}-t${t}.log
          echo "taskset -c 0-63 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -s ${s} -d 5000 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} -u ${u} >> ${out}"
          taskset -c 0-63 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -s ${s} -d 5000 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} -u ${u} --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/results.csv >> ${out}
        done
      done
    done
  done
done
