package maf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import hits.Hit.FrameDirection;
import util.DAACompressAlignment;
import util.SparseString;

public class MAF_Hit {

	private String readName;
	private int totalQueryLength;
	private byte[] packedQuerySequence;
	private int rawScore;
	private int subjectID;
	private int queryStart, refStart, queryLength;
	private FrameDirection frameDir;
	private String[] ali = new String[2];
	public ArrayList<Byte> editOperations;

	public MAF_Hit(String[] lineTriple, Object[] readInfo, ArrayList<Object[]> subjectInfo) {
		loadProperties(lineTriple, readInfo, subjectInfo);
	}

	public MAF_Hit(int rawScore, String subjectName, int refStart, int refEnd, String readName, int queryStart, int queryLength, int frame,
			ArrayList<Byte> editOperations, ArrayList<Object[]> subjectInfo, byte[] packedQuerySequence, int totalQueryLength) {

		this.rawScore = rawScore;
		Object[] subject = { new SparseString(subjectName), null };
		this.subjectID = Collections.binarySearch(subjectInfo, subject, new InfoComparator());
		this.refStart = refStart;
		this.readName = readName;
		this.queryStart = queryStart;
		this.queryLength = queryLength;
		this.frameDir = frame < 0 ? FrameDirection.NEGATIVE : FrameDirection.POSITIVE;
		this.editOperations = editOperations;
		this.packedQuerySequence = packedQuerySequence;
		this.totalQueryLength = totalQueryLength;

	}

	public void loadProperties(String[] lineTriple, Object[] readInfo, ArrayList<Object[]> subjectInfo) {

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
		readName = split[1];
		queryStart = Integer.parseInt(split[2]);
		queryLength = Integer.parseInt(split[3]);
		frameDir = split[4].equals("+") ? FrameDirection.POSITIVE : FrameDirection.NEGATIVE;
		ali[0] = split[6].toUpperCase();

		if (ali[0].length() == ali[1].length())
			editOperations = DAACompressAlignment.run(ali);

	}

	private static class InfoComparator implements Comparator<Object[]> {
		@Override
		public int compare(Object[] o1, Object[] o2) {
			SparseString s1 = (SparseString) o1[0];
			SparseString s2 = (SparseString) o2[0];
			return s1.toString().compareTo(s2.toString());
		}
	}

	public void setReadInfo(Object[] readInfo) {
		packedQuerySequence = (byte[]) readInfo[1];
		totalQueryLength = (int) readInfo[2];
		if (frameDir == FrameDirection.NEGATIVE)
			queryStart = totalQueryLength - queryStart - 1;
	}

	public int getRawScore() {
		return rawScore;
	}

	public int getSubjectID() {
		return subjectID;
	}

	public int getQueryStart() {
		return queryStart;
	}

	public int getRefStart() {
		return refStart;
	}

	public int getQueryLength() {
		return queryLength;
	}

	public FrameDirection getFrameDir() {
		return frameDir;
	}

	public String[] getAli() {
		return ali;
	}

	public ArrayList<Byte> getEditOperations() {
		return editOperations;
	}

	public String getReadName() {
		return readName;
	}

	public byte[] getPackedQuerySequence() {
		return packedQuerySequence;
	}

	public int getTotalQueryLength() {
		return totalQueryLength;
	}

	public boolean makesSense() {
		if (subjectID < 0 || editOperations == null)
			return false;
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MAF_Hit) {
			MAF_Hit h = (MAF_Hit) o;
			if (h.getQueryStart() == queryStart && h.getSubjectID() == subjectID && h.getReadName().equals(readName) && h.getRawScore() == rawScore
					&& h.getQueryStart() == queryStart && h.getRefStart() == refStart && h.getEditOperations().size() == editOperations.size()) {
				for (int i = 0; i < editOperations.size(); i++) {
					if (editOperations.get(i) != h.getEditOperations().get(i))
						return false;
				}
			}
		}
		return false;
	}

}
