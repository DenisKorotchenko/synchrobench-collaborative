package contention.benchmark;

import java.lang.reflect.Method;
import java.util.Random;

import ru.dksu.semantic.ExtendedMap;

/**
 * The loop executed by each thread of the map 
 * benchmark.
 * 
 * @author Vincent Gramoli
 * 
 */
public class TestMapThreadLoop implements Runnable {

	/** The instance of the running benchmark */
	public ExtendedMap bench;
	/** The stop flag, indicating whether the loop is over */
	protected volatile boolean stop = false;
	/** The pool of methods that can run */
	protected Method[] methods;
	/** The number of the current thread */
	protected final short myThreadNum;

	/** The counters of the thread successful operations */
	public long numModify = 0;
	public long numGet = 0;
	public long numSum = 0;
    public long numCap = 0;
	/** The counter of the false-returning operations */
	public long failures = 0;
	/** The counter of the thread operations */
	public long total = 0;
	/** The random number */
	Random rand = new Random();

	public long getCount;
	public long nodesTraversed;
	public long structMods;

	public void clearCounters() {
		numModify = 0;
		numGet = 0;
		numSum = 0;
        numCap = 0;
		failures = 0;
		total = 0;
	}

	int[] cdf = new int[4];

	public TestMapThreadLoop(short myThreadNum,
                             ExtendedMap bench, Method[] methods) {
		this.myThreadNum = myThreadNum;
		this.bench = bench;
		/* initialize the method boundaries */
		assert Parameters.distribution.length == 3;
		cdf[0] = Parameters.distribution[0] * 10;
		cdf[1] = (Parameters.distribution[0] + Parameters.distribution[1]) * 10;
        cdf[2] = (Parameters.distribution[0] + Parameters.distribution[1] + Parameters.distribution[2]) * 10;
		cdf[3] = 1000;

		if (myThreadNum == 0) {
			System.out.println("Distribution: ");
			for (int _cdf : cdf) {
				System.out.print(_cdf);
				System.out.println(" ");
			}
		}
//		assert (Parameters.numWrites >= Parameters.numWriteAlls);
//		cdf[0] = 10 * Parameters.numWriteAlls;
//		cdf[1] = 10 * Parameters.numWrites;
//		cdf[2] = cdf[1] + 10 * Parameters.numSnapshots;
	}

	public void stopThread() {
		stop = true;
	}

	public void printDataStructure() {
		System.out.println(bench.toString());
	}

	public void run() {

		while (!stop) {
			Integer newInt = rand.nextInt(Parameters.range - 128);
			int coin = rand.nextInt(1000);
			if (coin < cdf[0]) { // 1. put / remove
				try {
                    if (rand.nextBoolean()) {
                        int value = rand.nextInt(-Parameters.range, Parameters.range);
                        bench.put(newInt, value);
                    } else {
                        bench.remove(newInt);
                    }
					numModify++;
				} catch (Exception e) {
					this.failures++;
				}
			} else if (coin < cdf[1]) { // 2. get
				try {
					bench.get(newInt);
					numGet++;
				} catch (Exception e) {
					this.failures++;
				}
			} else if (coin < cdf[2]) { // 3. sum
				try {
					bench.sum();
					numSum++;
				} catch (Exception e) {
					this.failures++;
				}
			} else { // 4. cap
                try {
                    int value = rand.nextInt(-Parameters.range, Parameters.range);
                    bench.cap(value);
                    numCap++;
                } catch (Exception e) {
                    this.failures++;
                }
            }
			total++;

			assert total == numGet + numModify + numSum + numCap + failures;
		}
		// System.out.println(numAdd + " " + numRemove + " " + failures);

		System.out.println("Thread #" + myThreadNum + " finished.");
	}
}
