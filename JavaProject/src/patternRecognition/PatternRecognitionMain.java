package patternRecognition;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import tools.Pair;

public class PatternRecognitionMain {

	private static int[] NUMBER_OF_SERIES = { 10000, 40000, 100000, 200000, 500000 };
	private static float[] NUMBER_OF_THREAD_PER_CORE = { 0.5f, 1, 2, 5, 20, 50, 100 };
//	private static float[] NUMBER_OF_THREAD_PER_CORE = { 1,1,1,1,1 };
//	 private static float[] NUMBER_OF_THREAD_PER_CORE = {0.5f};
//	 private static int[] NUMBER_OF_SERIES = { 100, 200, 400, 500 };
	// private static int[] NUMBER_OF_SERIES = { 500 };
//	 private static int[] NUMBER_OF_THREAD_PER_CORE = { 2 };
	private int LENGTH_OF_SERIES = 1000;
	private int LENGTH_OF_PATTERN = 100;
	private int AMPLITUDE = 10;
	private static Random RANDOM = new Random();
	private static float[] pattern;
	private static float[][] series;
	private static boolean error;

	public PatternRecognitionMain(int numberOfSeries) {

		/*
		 * Creating the same series and pattern for both methods (sequential and
		 * parallel)
		 */
		System.out.println("Generating numerical series and pattern......");
		pattern = generatePattern();
		series = generateSeries(numberOfSeries);
	}

	public static void main(String[] args) {

		long seed = (long) (Math.random() * 10000);
		System.out.println("SEED: " + seed);
		RANDOM.setSeed(seed);

		Pair<PatternRecognitionResult, Long>[] resultSequential = new Pair[NUMBER_OF_SERIES.length];
		Pair<PatternRecognitionResult, Long>[][] resultParallel = new Pair[NUMBER_OF_SERIES.length][NUMBER_OF_THREAD_PER_CORE.length];
		int row = 0;
		for (int numberOfSeries : NUMBER_OF_SERIES) {
			int column = 0;

			System.out.println("\n\nNumber of series: " + numberOfSeries);
			PatternRecognitionMain patternRecognition = new PatternRecognitionMain(numberOfSeries);
			// Sequential method
			System.out.println("Sequential version ongoing......");
			long start = System.currentTimeMillis();
			 PatternRecognitionResult bestSad = new PatternRecognitionResult(0, 0, 0);
//			PatternRecognitionResult bestSad = patternRecognition.getBestSadSequential(series);
			long end = System.currentTimeMillis();
			resultSequential[row] = new Pair<PatternRecognitionResult, Long>(bestSad, end - start);

			System.out.println("#################   SEQUENTIAL VERSION RESULT  ##############");
			printReport(start, bestSad, end);

			for (float numberOfThreadPerCore : NUMBER_OF_THREAD_PER_CORE) {
				System.out.println("\n\nNumber of series: " + numberOfSeries + " - Number of thread per core: "
						+ numberOfThreadPerCore);

				// Parallel method
				System.out.println("\nParallel version ongoing......");
				long startP = System.currentTimeMillis();
				PatternRecognitionResult bestSadP = patternRecognition.getBestSadParallel(numberOfThreadPerCore);
				long endP = System.currentTimeMillis();

				System.out.println("#################   PARALLEL VERSION RESULT  ################");
				printReport(startP, bestSadP, endP);

				resultParallel[row][column] = new Pair<PatternRecognitionResult, Long>(bestSadP, endP - startP);
				column++;
				if (!bestSad.equals(bestSadP)) {
					System.out.println("!!!!!!!! ERROR ON CALCULATION OF SAD !!!!!!!!!!!!");
					error = true;
				}
			}
			row++;
		}

		printFullReport(resultSequential, resultParallel);

	}

	private static void printFullReport(Pair<PatternRecognitionResult, Long>[] resultSequential,
			Pair<PatternRecognitionResult, Long>[][] resultParallel) {
		System.out.println("\n\n#################   OVERALL RESULT  ################\n");
		if (error) {
			System.out.println("!!!!!!!! ERROR ON CALCULATION OF SAD !!!!!!!!!!!!");
		}

		// HEADER
		printLine();
		System.out.print("||    Series\t|| Sequential \t||");
		for (float thread : NUMBER_OF_THREAD_PER_CORE) {
			System.out.print("   " + thread + " threads\t||");
		}
		printLine();

		// TABLE CONTENT
		for (int row = 0; row < NUMBER_OF_SERIES.length; row++) {
			System.out
					.print("||    " + NUMBER_OF_SERIES[row] + " \t||    " + resultSequential[row].getSecond() + "\t||");
			for (int column = 0; column < NUMBER_OF_THREAD_PER_CORE.length; column++) {
				System.out.print("    " + resultParallel[row][column].getSecond() + "  \t||");
			}
			System.out.println();
		}

	}

	private static void printLine() {
		System.out.println();
		for (int i = 0; i < NUMBER_OF_THREAD_PER_CORE.length; i++) {
			System.out.print("---------------------------------");
		}
		System.out.println();

	}

	/**
	 * This function just print the obtained result
	 * 
	 * @param start
	 *            the starting time
	 * @param bestSad
	 *            the obtained SAD (Sum of Absolute Differences)
	 * @param end
	 *            the end of the method
	 */
	private static void printReport(long start, PatternRecognitionResult bestSad, long end) {
		System.out.println("Elapsed time:\t" + (end - start) + " ms");
		System.out.println("Best SAD:\t" + bestSad.getSadValue() + "\nSeries index:\t" + bestSad.getSeriesIndex()
				+ "\nPosition:\t" + bestSad.getPositionIndex() + " - " + (bestSad.getPositionIndex() + pattern.length));
	}

	/**
	 * The sequential version of the pattern recognition using the SAD technique
	 * 
	 * @return the best SAD between the generated series and pattern with the
	 *         indication of series index, series position and sad value.
	 */
	private PatternRecognitionResult getBestSadSequential(float[][] seriesToAnalize) {
		// It simply call the sequential method on the whole matrix of series
		return getBestSadSequential(seriesToAnalize, 0, series.length - 1);
	}

	/**
	 * It analyzes the series in the range [startIndex ; endIndex] calculating the
	 * best SAD for that range.
	 * 
	 * @param seriesToAnalize
	 *            the whole bunch of series generated
	 * @param startIndex
	 *            the first series to be analyzed
	 * @param endIndex
	 *            the last series to be analyzed
	 * @return the best SAD calculated in the passed range, represented as a
	 *         {@link PatternRecognitionResult}.
	 */
	private PatternRecognitionResult getBestSadSequential(float[][] seriesToAnalize, int startIndex, int endIndex) {
		PatternRecognitionResult bestSad = new PatternRecognitionResult(-1, -1, Float.MAX_VALUE);
		for (int i = startIndex; i <= endIndex; i++) {
			for (int j = 0; j < seriesToAnalize[0].length - pattern.length; j++) {
				float sad = getSad(seriesToAnalize[i], j);
				if (sad < bestSad.getSadValue()) {
					bestSad = new PatternRecognitionResult(i, j, sad);
				}
			}
		}

		return bestSad;
	}

	/**
	 * It calculate the SAD of a portion of series
	 * 
	 * @param seriesToAnalize
	 *            the series to be analyzed
	 * @param j
	 *            the starting point from which we want to calculate the SAD
	 * @return the SAD between the starting point of the passed series and the
	 *         pattern
	 */
	private float getSad(float[] seriesToAnalize, int j) {
		float sad = 0;
		for (int i = 0; i < pattern.length; i++) {
			sad += Math.abs(seriesToAnalize[j + i] * 0.90 - pattern[i]);
		}
		return sad;
	}

	/**
	 * The parallel version of the pattern recognition using the SAD technique
	 * 
	 * @param numberOfThreadPerCore
	 *            the number of threads per core we want to use
	 * 
	 * @return the best SAD between the generated series and pattern with the
	 *         indication of series index, series position and sad value.
	 */
	private PatternRecognitionResult getBestSadParallel(float numberOfThreadPerCore) {
		/*
		 * A good rule of thumb is to have approximately the same number of threads as
		 * available cores.
		 */
		int threadPoolSize = new Integer((int) ((float)Runtime.getRuntime().availableProcessors() * numberOfThreadPerCore));
		/*
		 * I create an executor with a fixed size to avoid the generation of an
		 * excessive number of threads.
		 */
		ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
//		ExecutorService executor = Executors.newCachedThreadPool();
		/*
		 * Each thread will take care of a portion of the whole matrix of series
		 */
		int seriesSlice = new Double(Math.ceil((float) series.length / (float) threadPoolSize)).intValue();
		PatternRecognitionResult bestSad = new PatternRecognitionResult(-1, -1, Float.MAX_VALUE);
		for (int i = 0; i < threadPoolSize; i++) {
			int startIndex = i * seriesSlice;
			int endIndex;
			if (i == threadPoolSize - 1) {
				endIndex = series.length - 1;
			} else {
				endIndex = Math.min((i + 1) * seriesSlice - 1, series.length - 1);
			}
			executor.execute(new Runnable() {

				@Override
				public void run() {
					PatternRecognitionResult bestLocalSad = getBestSadSequential(series, startIndex, endIndex);
					/*
					 * Once the thread found its best local SAD related to its portion of series, in
					 * a synchronized manner, it will update the general best SAD (if it's the
					 * case).
					 */
					synchronized (bestSad) {
						if (bestLocalSad.getSadValue() < bestSad.getSadValue()) {
							bestSad.setPositionIndex(bestLocalSad.getPositionIndex());
							bestSad.setSeriesIndex(bestLocalSad.getSeriesIndex());
							bestSad.setSadValue(bestLocalSad.getSadValue());
						}
					}
				}
			});
		}
		executor.shutdown();
		try {
			/*
			 * Manual barrier with timeout
			 */
			executor.awaitTermination(60, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return bestSad;
	}

	/**
	 * It generated random numerical series
	 * 
	 * @param numberOfSeries
	 *            the number of series we want to generate
	 * 
	 * @return a matrix filled with the generated numerical series
	 */
	private float[][] generateSeries(int numberOfSeries) {
		float[][] newSeries = new float[numberOfSeries][LENGTH_OF_SERIES];
		int start = 0;
		if (PatternRecognitionMain.series != null) {
			start = PatternRecognitionMain.series.length;
			for (int j = 0; j < start; j++) {
				newSeries[j] = series[j];
			}
		}
		for (int i = start; i < numberOfSeries; i++) {
			newSeries[i][0] = RANDOM.nextFloat() * AMPLITUDE;
			for (int j = 1; j < LENGTH_OF_SERIES; j++) {
				newSeries[i][j] = new Float(newSeries[i][j - 1] * 0.90 + RANDOM.nextGaussian() * AMPLITUDE);
			}
		}
		return newSeries;
	}

	/**
	 * It generates a random pattern to be found in the series
	 * 
	 * @return the random pattern
	 */
	private float[] generatePattern() {
		float[] pattern = new float[LENGTH_OF_PATTERN];
		pattern[0] = RANDOM.nextFloat() * AMPLITUDE;
		for (int i = 1; i < pattern.length; i++) {
			pattern[i] = new Float(pattern[i - 1] + RANDOM.nextGaussian() * AMPLITUDE);
		}
		return pattern;
	}

}
