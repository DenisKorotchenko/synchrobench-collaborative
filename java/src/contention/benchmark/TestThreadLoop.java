package contention.benchmark;

import contention.abstractions.CompositionalMap;
import ru.dksu.semantic.ITestStructure;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * The loop executed by each thread of the map 
 * benchmark.
 * 
 * @author Vincent Gramoli
 * 
 */
public class TestThreadLoop implements Runnable {

	/** The instance of the running benchmark */
	public ITestStructure bench;
	/** The stop flag, indicating whether the loop is over */
	protected volatile boolean stop = false;
	/** The pool of methods that can run */
	protected Method[] methods;
	/** The number of the current thread */
	protected final short myThreadNum;

	/** The counters of the thread successful operations */
	public long numAddRange = 0;
	public long numUpdateRange = 0;
	public long numGetRange = 0;
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
		numAddRange = 0;
		numUpdateRange = 0;
		numGetRange = 0;
		failures = 0;
		total = 0;
	}

	int[] cdf = new int[3];

	public TestThreadLoop(short myThreadNum,
						  ITestStructure bench, Method[] methods) {
		this.myThreadNum = myThreadNum;
		this.bench = bench;
		/* initialize the method boundaries */
		assert Parameters.distribution.length == 2;
		cdf[0] = Parameters.distribution[0] * 10;
		cdf[1] = (Parameters.distribution[0] + Parameters.distribution[1]) * 10;
		cdf[2] = 1000;
		System.out.println("Distribution: ");
		for (int _cdf: cdf) {
			System.out.print(_cdf);
			System.out.println(" ");
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
			Integer newInt = rand.nextInt(Parameters.range);
			int coin = rand.nextInt(1000);
			if (coin < cdf[0]) { // 1. addRange
				int newInt2 = rand.nextInt(Parameters.range);
				if (newInt > newInt2) {
					int temp = newInt;
					newInt = newInt2;
					newInt2 = temp;
				}
				int value = rand.nextInt(-Parameters.range, Parameters.range);
				try {
					bench.addRange(newInt, newInt2, value);
					numAddRange++;
				} catch (Exception e) {
					this.failures++;
				}
			} else if (coin < cdf[1]) { // 2. updateRange
				int newInt2 = rand.nextInt(Parameters.range);
				if (newInt > newInt2) {
					int temp = newInt;
					newInt = newInt2;
					newInt2 = temp;
				}
				int value = rand.nextInt(-Parameters.range, Parameters.range);
				try {
					bench.addRange(newInt, newInt2, value);
					numUpdateRange++;
				} catch (Exception e) {
					this.failures++;
				}
			} else { // 3. getRange
				int newInt2 = rand.nextInt(Parameters.range);
				if (newInt > newInt2) {
					int temp = newInt;
					newInt = newInt2;
					newInt2 = temp;
				}
				int value = rand.nextInt(-Parameters.range, Parameters.range);
				try {
					bench.addRange(newInt, newInt2, value);
					numGetRange ++;
				} catch (Exception e) {
					this.failures++;
				}
			}
			total++;

			assert total == numAddRange + numGetRange + numUpdateRange + failures;
		}
		// System.out.println(numAdd + " " + numRemove + " " + failures);

		System.out.println("Thread #" + myThreadNum + " finished.");
	}
}
