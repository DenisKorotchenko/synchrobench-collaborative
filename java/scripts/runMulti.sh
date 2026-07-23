#!/bin/bash

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
#benchs="ru.dksu.semantic.TestStructureAF ru.dksu.semantic.TestStructureAU"
#benchs="ru.dksu.semantic.TestStructureLF ru.dksu.semantic.TestStructureLU"
# ru.dksu.semantic.TestStructureRW ru.dksu.semantic.TestStructureSimple ru.dksu.semantic.TestStructureWithout"
#benchs="ru.dksu.semantic.ExtendedMapSL3_U ru.dksu.semantic.ExtendedMapNoLock ru.dksu.semantic.ExtendedMapRW ru.dksu.semantic.ExtendedMapSL_U"
#benchs="ru.dksu.semantic.ExtendedMapSL3_U ru.dksu.semantic.ExtendedMapSL_U"
benchs="ru.dksu.semantic.ExtendedMapSLGME"
#benchs="ru.dksu.semantic.TestStructureU" # ru.dksu.semantic.TestStructureRW ru.dksu.semantic.TestStructureWithout"
#benchs="trees.lockbased.IntegerCollaborativeHelperFairLockBasedStanfordTreeMap trees.lockbased.IntegerLockBasedStanfordTreeMap"

distrs=(
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
#  "0 0 100"
#  "0 0 0"
#  "0 0 50"
#  "23 69 8"
#  "69 23 8"
#  "92 0 8"
#  "0 92 8"
#  "0 100 0"
#  "25 75 0"
#  "50 50 0"
#  "75 25 0"
#  "100 0 0"
#  "33 33"
#  "90 5"
#  "80 10"
#  "100 0"
#  "0 100"
#  "25 25 25"
#  "5 5 45"
#  "45 5 45"
#  "30 30 30"

# MAP

  "50 50 0"
  "50 0 50"
  "25 25 25"
  "5 5 80"
  "10 40 45"
  "40 10 40"
#  "10 40 40"
#  "40 40 10"

# TEST STRUCTURE
#  "0 0 50 50"
#  "0 0 0 50"
#  "20 20 20 20"

#  "10 0 40 40"
#  "10 0 10 40"
#  "90 5 5 0"

  #  "5 90 5 0"

#  "10 0 30 30"
#  "40 0 20 20"
#  "30 10 20 20"
)

iterations=5
W=5
d=10000

thread="1"
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

parts=2

part=1
current=0
thread="1 2 4 8 15"
for dist in "${distrs[@]}"; do
  for i in ${size}; do
    r=$((i*2))
    for t in ${thread}; do
      for bench in ${benchs}; do
        current=$((current+1))
        out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/290126/${bench}-size-${i}-threads-${t}-33-33-34-unfair.log
        date
        echo "Experiment $current of $((count*5)), part $part / $parts"
        echo "numactl --physcpubind=0-15 --interleave=0 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W ${W} -a 0 -d ${d} -t ${t} -i ${i} -r ${r} -n ${iterations} -b ${bench} --distribution ${dist} --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/res.csv >> ${out}"
        numactl --physcpubind=0-15 --interleave=0 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W ${W} -a 0 -d ${d} -t ${t} -i ${i} -r ${r} -n ${iterations} -b ${bench} --distribution "${dist}" --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/res-unfair.csv >> ${out}
      done
    done
  done
done

part=2
current=0
thread="24 31"
for dist in "${distrs[@]}"; do
  for i in ${size}; do
    r=$((i*2))
    for t in ${thread}; do
      for bench in ${benchs}; do
        current=$((current+1))
        out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/290126/${bench}-size-${i}-threads-${t}-33-33-34-unfair.log
        date
        echo "Experiment $current of $((count*2)), part $part / $parts"
        echo "numactl --physcpubind=0-15,64-79 --interleave=0 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W ${W} -a 0 -d ${d} -t ${t} -i ${i} -r ${r} -n ${iterations} -b ${bench} --distribution ${dist} --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/res.csv >> ${out}"
        numactl --physcpubind=0-15,64-79 --interleave=0 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W ${W} -a 0 -d ${d} -t ${t} -i ${i} -r ${r} -n ${iterations} -b ${bench} --distribution "${dist}" --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/res-unfair.csv >> ${out}
      done
    done
  done
done

#
#part=3
#current=0
#thread="47"
#for dist in "${distrs[@]}"; do
#  for i in ${size}; do
#    r=$((i*2))
#    for t in ${thread}; do
#      for bench in ${benchs}; do
#        current=$((current+1))
#        out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/290126/${bench}-size-${i}-threads-${t}-33-33-34.log
#        date
#        echo "Experiment $current of $count, part $part / $parts"
#        echo "numactl --physcpubind=0-47 --interleave=0-2 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W ${W} -a 0 -d ${d} -t ${t} -i ${i} -r ${r} -n ${iterations} -b ${bench} --distribution ${dist} --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/res.csv >> ${out}"
#        numactl --physcpubind=0-47 --interleave=0-2 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W ${W} -a 0 -d ${d} -t ${t} -i ${i} -r ${r} -n ${iterations} -b ${bench} --distribution "${dist}" --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/res.csv >> ${out}
#      done
#    done
#  done
#done
#
#part=4
#current=0
#thread="63"
#for dist in "${distrs[@]}"; do
#  for i in ${size}; do
#    r=$((i*2))
#    for t in ${thread}; do
#      for bench in ${benchs}; do
#        current=$((current+1))
#        out=/home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/log/290126/${bench}-size-${i}-threads-${t}-33-33-34.log
#        date
#        echo "Experiment $current of $count, part $part / $parts"
#        echo "numactl --physcpubind=0-63 --interleave=0-3 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W ${W} -a 0 -d ${d} -t ${t} -i ${i} -r ${r} -n ${iterations} -b ${bench} --distribution ${dist} --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/res.csv >> ${out}"
#        numactl --physcpubind=0-63 --interleave=0-3 java -server -cp ../lib/compositional-deucestm-0.1.jar:../lib/mydeuce.jar:../bin contention.benchmark.Test -W ${W} -a 0 -d ${d} -t ${t} -i ${i} -r ${r} -n ${iterations} -b ${bench} --distribution "${dist}" --csvPath /home/dkorotchenko/collaborative-operations/synchrobench-collaborative/java/output/res.csv >> ${out}
#      done
#    done
#  done
#done