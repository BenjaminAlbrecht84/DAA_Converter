
/*
 * Copyright 2017 Benjamin Albrecht
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import hits.Hit.FrameDirection;
import maf.MAF_Hit;
import startUp.MainConverter;

public class Hit_Filter_parallel {

	private static List<MAF_Hit> passedHits;

	public static final int TRESHOLD = 100000;
	private static int maxProgress, lastProgress = 0;
	private static AtomicInteger progress = new AtomicInteger();

	private static CountDownLatch latch;
	private static ExecutorService executor;

	public static ArrayList<MAF_Hit> run(ArrayList<MAF_Hit> hits, double lambda, double K, int cores) {

		System.out.println("\nSTEP 3.1 - Filtering " + hits.size() + " hits...");
		passedHits = Collections.synchronizedList(new ArrayList<MAF_Hit>());
		executor = Executors.newFixedThreadPool(cores);

		int numOfHits = hits.size();
		int chunk = (int) Math.ceil((double) numOfHits / (double) cores);

		ArrayList<Thread> filterThreads = new ArrayList<Thread>();
		for (int i = 0; i < hits.size(); i += chunk)
			filterThreads.add(new FilterThread(hits, i, i + chunk, lambda, K));
		maxProgress = hits.size();
		runInParallel(filterThreads);
		executor.shutdown();

		reportFinish();

		return new ArrayList<MAF_Hit>(passedHits);

	}

	private static void reportProgress(int delta) {
		progress.getAndAdd(delta);
		int p = ((int) ((((double) progress.get() / (double) maxProgress)) * 100) / 10) * 10;
		if (p > lastProgress && p < 100) {
			lastProgress = p;
			System.out.print(p + "% ");
		}
	}

	private static void reportFinish() {
		progress.set(0);
		lastProgress = 0;
		System.out.print(100 + "%\n");
	}

	private static void runInParallel(ArrayList<Thread> threads) {
		latch = new CountDownLatch(threads.size());
		for (Runnable t : threads)
			executor.execute(t);
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static class FilterThread extends Thread {

		private ArrayList<MAF_Hit> hits;
		private int l, r;
		private double lambda, K;

		public FilterThread(ArrayList<MAF_Hit> hits, int l, int r, double lambda, double k) {
			this.hits = hits;
			this.l = l;
			this.r = Math.min(r, hits.size());
			this.lambda = lambda;
			K = k;
		}

		@Override
		public void run() {
			applyFilter(hits, l, r, lambda, K);
		}

		public static void applyFilter(ArrayList<MAF_Hit> hits, int l, int r, double lambda, double K) {

			int progress = 0;
			for (int i = l; i < r; i++) {

				if ((++progress) % 1000 == 0)
					reportProgress(1000);

				MAF_Hit h1 = hits.get(i);

				// checking if h1 is dominated by another hit
				boolean isDominated = false;
				for (MAF_Hit h2 : hits) {
					if (!h1.equals(h2)) {
						double coverage = cmpHitCoverage(h1, h2);
						double bitScore1 = cmpBitScore(h1.getRawScore(), lambda, K);
						double bitScore2 = cmpBitScore(h2.getRawScore(), lambda, K);
						if (coverage > MainConverter.MIN_PROPORTION_COVERAGE && MainConverter.MIN_PROPORTION_SCORE * bitScore2 > bitScore1) {
							isDominated = true;
							break;
						}

					}
				}

				// only non-dominated hits are reported
				if (!isDominated)
					passedHits.add(h1);

			}

			latch.countDown();

		}

		private static double cmpBitScore(int rawScore, double lambda, double K) {
			return (new Double(rawScore) * lambda - Math.log(K)) / Math.log(2);
		}

		private static double cmpHitCoverage(MAF_Hit h1, MAF_Hit h2) {

			int[] h1Coord = getQueryCoordinates(h1);
			int[] h2Coord = getQueryCoordinates(h2);

			// checking if overlap exists
			if (h1Coord[0] > h2Coord[1])
				return 0;
			if (h2Coord[0] > h1Coord[1])
				return 0;

			// computing coverage of h1 by h2
			double l = Math.max(h1Coord[0], h2Coord[0]);
			double r = Math.min(h1Coord[1], h2Coord[1]);
			double overlap = r - l + 1.;
			double length1 = h1Coord[1] - h1Coord[0] + 1;
			double coverage = overlap / length1;

			return coverage;
		}

		private static int[] getQueryCoordinates(MAF_Hit h) {
			int queryStart = h.getFrameDir() == FrameDirection.POSITIVE ? h.getQueryStart() : h.getQueryStart() - h.getQueryLength() + 1;
			int queryEnd = queryStart + h.getQueryLength() - 1;
			int[] coord = { queryStart, queryEnd };
			return coord;
		}

	}

}
