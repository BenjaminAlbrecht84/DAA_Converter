package io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import util.LineCounter;
import util.SparseString;

public class FastAQ_Reader {

	private static int maxProgress, lastProgress = 0;
	private static AtomicInteger progress = new AtomicInteger();

	private static final HashMap<Character, Integer> nucToIndex;
	static {
		nucToIndex = new HashMap<Character, Integer>();
		nucToIndex.put('A', 0);
		nucToIndex.put('C', 1);
		nucToIndex.put('G', 2);
		nucToIndex.put('T', 3);
	}

	public static ArrayList<Object[]> read(File fastAQFile) {

		ArrayList<Object[]> readInfo = new ArrayList<Object[]>();
		try {

			maxProgress = (int) LineCounter.run(fastAQFile);

			BufferedReader buf;
			try {
				buf = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fastAQFile))));
			} catch (ZipException e) {
				buf = new BufferedReader(new FileReader(fastAQFile));
			}

			String line, id = "";
			boolean readSequence = false;
			StringBuilder seq = new StringBuilder("");
			int lineCounter = 0;
			while ((line = buf.readLine()) != null) {

				lineCounter++;
				if (lineCounter % 100 == 0)
					reportProgress(100);

				if (line.startsWith("@") || line.startsWith(">")) {
					if (seq.length() != 0 && !id.isEmpty()) {
						Object[] o = { new SparseString(id), packSequence(seq.toString()), seq.length() };
						readInfo.add(o);
					}
					seq = new StringBuilder("");
					id = line.substring(1).split(" ")[0];
					readSequence = true;
				} else if (line.startsWith("+")) {
					readSequence = false;
				} else if (readSequence) {
					seq.append(line);
				}

			}
			if (seq.length() != 0 && !id.isEmpty()) {
				Object[] o = { new SparseString(id), packSequence(seq.toString()), seq.length() };
				readInfo.add(o);
			}
			buf.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		reportFinish();

		return readInfo;

	}

	private static void reportFinish() {
		progress.set(0);
		lastProgress = 0;
		System.out.print(100 + "%\n");
	}

	private static void reportProgress(int delta) {
		progress.getAndAdd(delta);
		int p = ((int) ((((double) progress.get() / (double) maxProgress)) * 100) / 10) * 10;
		if (p > lastProgress && p < 100) {
			lastProgress = p;
			System.out.print(p + "% ");
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
