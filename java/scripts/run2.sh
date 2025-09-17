#!/bin/bash

thread="4"
size="16384"

writes="0 40 100"

###############################
# records all benchmark outputs
###############################
snapshots="10 50"
benchs="trees.lockbased.IntegerCollaborativeLockBasedStanfordTreeMap trees.lockbased.IntegerLockBasedStanfordTreeMap" #trees.lockbased.IntegerPureLockBasedStanfordTreeMap"
#benchs="hashtables.lockbased.IntegerCollaborativeQueueHashMap hashtables.lockbased.IntegerQueueHashMap" #hashtables.lockbased.IntegerNonCollaborativeQueueHashMap
for s in ${snapshots}; do
  for t in ${thread}; do
    for write in ${writes}; do
      for bench in ${benchs}; do
        u=$((((100-$s)*$write)/100))
        for i in ${size}; do
          r=$((i*2))
          out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/${bench}-i${i}-u${u}-s${s}-t${t}.log
          echo "taskset -c 0-15 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -s ${s} -d 5000 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} -u ${u} >> ${out}"
          taskset -c 0-15 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W 5 -a 0 -s ${s} -d 5000 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} -u ${u} >> ${out}
        done
      done
    done
  done
done
