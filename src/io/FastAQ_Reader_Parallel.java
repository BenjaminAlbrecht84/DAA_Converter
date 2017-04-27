package io;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.Vector;

import util.FileParallelLineStreamer;
import util.SparseString;

public class FastAQ_Reader_Parallel extends FileParallelLineStreamer {

	private static final HashMap<Character, Integer> nucToIndex;
	static {
		nucToIndex = new HashMap<Character, Integer>();
		nucToIndex.put('A', 0);
		nucToIndex.put('C', 1);
		nucToIndex.put('G', 2);
		nucToIndex.put('T', 3);
	}

	private HashMap<Integer, ArrayList<Object[]>> idToReadInfo;
	private String id = "";
	private boolean readSequence = false;
	private StringBuffer seq = new StringBuffer("");

	public ArrayList<Object[]> read(File fastAQFile, int cores, boolean reportProgress) {

		// initializing parameters
		idToReadInfo = new HashMap<Integer, ArrayList<Object[]>>();
		id = "";
		readSequence = false;
		seq = new StringBuffer("");

		// parsing file in parallel
		run(fastAQFile, cores, reportProgress);

		// sorting threadIDs
		ArrayList<Integer> threadIDs_sorted = new ArrayList<Integer>();
		threadIDs_sorted.addAll(idToReadInfo.keySet());
		Collections.sort(threadIDs_sorted);

		// collecting read information
		ArrayList<Object[]> readInfo = new ArrayList<Object[]>();
		for (int id : threadIDs_sorted)
			readInfo.addAll(idToReadInfo.get(id));

		return readInfo;

	}

	@Override
	public void processLine(String line, int threadID, boolean lastLine) {
		if (line.startsWith("@") || line.startsWith(">")) {
			if (seq.length() != 0 && !id.isEmpty()) {
				Object[] o = { new SparseString(id), packSequence(seq.toString()), seq.length() };
				idToReadInfo.putIfAbsent(threadID, new ArrayList<Object[]>());
				idToReadInfo.get(threadID).add(o);
			}
			seq = new StringBuffer("");
			id = line.substring(1).split(" ")[0];
			readSequence = true;
		} else if (line.startsWith("+")) {
			readSequence = false;
		} else if (readSequence) {
			seq = seq.append(line);
		}
		if (lastLine) {
			if (seq.length() != 0 && !id.isEmpty()) {
				Object[] o = { new SparseString(id), packSequence(seq.toString()), seq.length() };
				idToReadInfo.putIfAbsent(threadID, new ArrayList<Object[]>());
				idToReadInfo.get(threadID).add(o);
			}
		}
	}

	private static byte[] packSequence(String dna) {

		Vector<Byte> packed = new Vector<Byte>();
		byte p = 0;
		for (int i = 0; i < dna.length(); i++) {
			char c = dna.charAt(i);
			byte b = 0;
			b |= nucToIndex.get(c) << (i * 2) % 8;
			p |= b;
			if (i == dna.length() - 1 || (((i + 1) * 2) % 8 == 0 && i != 0)) {
				packed.add(p);
				p = 0;
			}
		}

		byte[] a = new byte[packed.size()];
		for (int i = 0; i < packed.size(); i++)
			a[i] = packed.get(i);

		return a;

	}

}
