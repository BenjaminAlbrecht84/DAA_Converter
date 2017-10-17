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

package maf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;

import daa.reader.DAA_Header;
import daa.reader.DAA_Reader;

public class Header {

	private int gapOpen, gapExtend;
	private long dbSeqs;
	private BigInteger dbLetters;
	private double lambda, K;

	public void loadFromDAA(File daaFile) {
		DAA_Reader daaReader = new DAA_Reader(daaFile, false);
		DAA_Header daaHeader = daaReader.getDAAHeader();
		gapOpen = daaHeader.getGapOpen();
		gapExtend = daaHeader.getGapExtend();
		dbSeqs = daaHeader.getDbSeqsUsed();
		dbLetters = daaHeader.getDbLetters();
		lambda = daaHeader.getLambda();
		K = daaHeader.getK();
	}

	public void loadFromMaf(File maf_file) {

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
