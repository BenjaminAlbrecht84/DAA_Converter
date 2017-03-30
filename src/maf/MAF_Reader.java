package maf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import hits.Hit;
import io.FastAQ_Reader;
import util.DAACompressAlignment;
import util.SparseString;

public class MAF_Reader {

	public static Object[] run(File mafFile, File queryFile) {
		
		MAF_Header headerInfo = new MAF_Header(mafFile);
		headerInfo.load();
		
		ArrayList<Hit> hits = new ArrayList<Hit>();
		ArrayList<Object[]> subjectInfo = generateSubjectInfo(mafFile);
		ArrayList<Object[]> readInfo = FastAQ_Reader.read(queryFile);
		Collections.sort(readInfo, new InfoComparator());

		try {

			BufferedReader buf = new BufferedReader(new FileReader(mafFile));
			String l;
			String[] lineTriple = null;
			while ((l = buf.readLine()) != null) {

				// parsing scores
				if (l.startsWith("a")) {

					// creating new hit
					if (lineTriple != null) {
						MAF_Hit mafHit = new MAF_Hit(lineTriple, readInfo, subjectInfo);
						ArrayList<Byte> editOperations = DAACompressAlignment.run(mafHit.getAli());
						Hit h = new Hit(mafHit.getFrameDir(), mafHit.getRawScore(), mafHit.getRefStart(), mafHit.getQueryStart(),
								mafHit.getSubjectID(), mafHit.getReadID(), editOperations);
						hits.add(h);
					}

					lineTriple = new String[3];
					lineTriple[0] = l;

				}

				// adding subject line
				else if (l.startsWith("s") && lineTriple[1] == null)
					lineTriple[1] = l;

				// adding query line
				else if (l.startsWith("s") && lineTriple[2] == null)
					lineTriple[2] = l;

			}

			// creating new hit
			if (lineTriple != null) {
				MAF_Hit mafHit = new MAF_Hit(lineTriple, readInfo, subjectInfo);
				ArrayList<Byte> editOperations = DAACompressAlignment.run(mafHit.getAli());
				Hit h = new Hit(mafHit.getFrameDir(), mafHit.getRawScore(), mafHit.getRefStart(), mafHit.getQueryStart(), mafHit.getSubjectID(),
						mafHit.getReadID(), editOperations);
				hits.add(h);
			}

			buf.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		Collections.sort(hits, new HitComparator(readInfo));

		Object[] result = { headerInfo, hits, readInfo, subjectInfo };
		return result;

	}

	private static ArrayList<Object[]> generateSubjectInfo(File mafFile) {

		ArrayList<Object[]> subjectInfo = new ArrayList<Object[]>();
		try {

			BufferedReader buf = new BufferedReader(new FileReader(mafFile));
			String l;
			boolean b = true;
			while ((l = buf.readLine()) != null) {
				if (l.startsWith("s") && b) {
					String[] split = l.split("\\s+");
					Object[] subject = { new SparseString(split[1]), Integer.parseInt(split[5]) };
					int index = Collections.binarySearch(subjectInfo, subject, new InfoComparator());
					if (index < 0)
						subjectInfo.add(-index - 1, subject);
					b = false;
				}
				if (l.startsWith("a"))
					b = true;
			}
			buf.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

		return subjectInfo;

	}

	private static class InfoComparator implements Comparator<Object[]> {

		@Override
		public int compare(Object[] o1, Object[] o2) {
			SparseString s1 = (SparseString) o1[0];
			SparseString s2 = (SparseString) o2[0];
			return s1.toString().compareTo(s2.toString());
		}

	}

	private static class HitComparator implements Comparator<Hit> {

		private ArrayList<Object[]> readInfo;

		public HitComparator(ArrayList<Object[]> readInfo) {
			this.readInfo = readInfo;
		}

		@Override
		public int compare(Hit h1, Hit h2) {
			SparseString s1 = (SparseString) readInfo.get(h1.getReadID())[0];
			SparseString s2 = (SparseString) readInfo.get(h1.getReadID())[0];
			return s1.toString().compareTo(s2.toString());
		}

	}

}
