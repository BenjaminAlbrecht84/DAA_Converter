package maf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import hits.Hit.FrameDirection;
import util.SparseString;

public class MAF_Hit {

	private int rawScore;
	private int subjectID, readID;
	private int queryStart, refStart;
	private FrameDirection frameDir;
	private String[] ali = new String[2];

	public MAF_Hit(String[] lineTriple, ArrayList<Object[]> readInfo, ArrayList<Object[]> subjectInfo) {
		loadProperties(lineTriple, readInfo, subjectInfo);
	}

	public void loadProperties(String[] lineTriple, ArrayList<Object[]> readInfo, ArrayList<Object[]> subjectInfo) {

		// parsing scoring parameters
		String[] split = lineTriple[0].split("\\s+");
		rawScore = Integer.parseInt(split[1].substring(6));

		// parsing subject info
		split = lineTriple[1].split("\\s+");
		Object[] subject = { new SparseString(split[1]), Integer.parseInt(split[5]) };
		subjectID = Collections.binarySearch(subjectInfo, subject, new InfoComparator());
		refStart = Integer.parseInt(split[2]);
		ali[1] = split[6].toUpperCase();

		// reading query info
		split = lineTriple[2].split("\\s+");
		Object[] read = { new SparseString(split[1]), null };
		readID = Collections.binarySearch(readInfo, read, new InfoComparator());
		queryStart = Integer.parseInt(split[2]);
		frameDir = split[4].equals("+") ? FrameDirection.POSITIVE : FrameDirection.NEGATIVE;
		ali[0] = split[6].toUpperCase();

		if (frameDir == FrameDirection.NEGATIVE) {
			int totalReadLength = (int) readInfo.get(readID)[2];
			queryStart = totalReadLength - queryStart - 1;
		}

	}

	private static class InfoComparator implements Comparator<Object[]> {
		@Override
		public int compare(Object[] o1, Object[] o2) {
			SparseString s1 = (SparseString) o1[0];
			SparseString s2 = (SparseString) o2[0];
			return s1.toString().compareTo(s2.toString());
		}
	}

	public int getRawScore() {
		return rawScore;
	}

	public int getSubjectID() {
		return subjectID;
	}

	public int getReadID() {
		return readID;
	}

	public int getQueryStart() {
		return queryStart;
	}

	public int getRefStart() {
		return refStart;
	}

	public FrameDirection getFrameDir() {
		return frameDir;
	}

	public String[] getAli() {
		return ali;
	}

}
