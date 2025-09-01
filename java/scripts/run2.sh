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
          out=/home/denis/IdeaProjects/synchrobench-collaborative/log/3107-rangequeries-tree/${bench}-i${i}-u${u}-s${s}-t${t}.log
          echo "/home/denis/.jdks/corretto-17.0.13/bin/java -javaagent:/snap/intellij-idea-community/588/lib/idea_rt.jar=41201 -Dfile.encoding=UTF-8 -classpath /home/denis/IdeaProjects/synchrobench-collaborative/out/production/synchrobench-collaborative:/home/denis/IdeaProjects/synchrobench-collaborative/java/lib/deuceAgent-1.3.0.jar:/home/denis/IdeaProjects/synchrobench-collaborative/java/lib/junit.jar contention.benchmark.Test -W 5 -a 0 -s ${s} -d 5000 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} -u ${u}"
          /home/denis/.jdks/corretto-17.0.13/bin/java -javaagent:"/snap/intellij-idea-community/current/lib/idea_rt.jar=41201" -Dfile.encoding=UTF-8 -classpath /home/denis/IdeaProjects/synchrobench-collaborative/out/production/synchrobench-collaborative:/home/denis/IdeaProjects/synchrobench-collaborative/java/lib/deuceAgent-1.3.0.jar:/home/denis/IdeaProjects/synchrobench-collaborative/java/lib/junit.jar contention.benchmark.Test -W 5 -a 0 -s ${s} -d 5000 -t ${t} -i ${i} -r ${r} -n 5 -b ${bench} -u ${u} >> ${out}
        done
      done
    done
  done
done
