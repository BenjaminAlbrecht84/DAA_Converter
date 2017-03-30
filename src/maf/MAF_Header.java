package maf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;

public class MAF_Header {

	private File maf_file;

	private int gapOpen, gapExtend;
	private long dbSeqs;
	private BigInteger dbLetters;
	private double lambda, K;

	public MAF_Header(File maf_file) {
		this.maf_file = maf_file;
		load();
	}

	public void load() {

		try {

			BufferedReader buf = new BufferedReader(new FileReader(maf_file));
			String l;
			while ((l = buf.readLine()) != null) {
				if (l.startsWith("#")) {
					for (String s : l.split("\\s+")) {
						if (s.startsWith("a="))
							gapOpen = Integer.valueOf(s.substring(2));
						if (s.startsWith("b="))
							gapExtend = Integer.valueOf(s.substring(2));
						if (s.startsWith("sequences="))
							dbSeqs = Long.valueOf(s.substring(10));
						if (s.startsWith("letters="))
							dbLetters = BigInteger.valueOf(Long.valueOf(s.substring(8)));
						if (s.startsWith("lambda="))
							lambda = Double.valueOf(s.substring(7));
						if (s.startsWith("K="))
							K = Double.valueOf(s.substring(2));
					}
				} else
					break;

			}
			buf.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public int getGapOpen() {
		return gapOpen;
	}

	public int getGapExtend() {
		return gapExtend;
	}

	public long getDbSeqs() {
		return dbSeqs;
	}

	public BigInteger getDbLetters() {
		return dbLetters;
	}

	public double getLambda() {
		return lambda;
	}

	public double getK() {
		return K;
	}
	
}
