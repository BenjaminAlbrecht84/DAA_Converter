package util;

import java.util.ArrayList;

import hits.Hit.FrameDirection;
import maf.MAF_Hit;
import startUp.MainConverter;

public class Hit_Filter {

	public static ArrayList<MAF_Hit> run(ArrayList<MAF_Hit> hits, double lambda, double K) {

		ArrayList<MAF_Hit> passedHits = new ArrayList<MAF_Hit>();
		for (MAF_Hit h1 : hits) {
			
			// checking whether h1 is dominated by another hit
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

		return passedHits;

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
