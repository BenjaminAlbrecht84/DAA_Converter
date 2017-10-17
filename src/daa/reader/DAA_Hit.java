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

package daa.reader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Vector;

import util.AA_Alphabet;
import util.CodonTranslator;

public class DAA_Hit {

	private long filePointer;

	// query properties
	private int totalQueryLength;
	private String queryName;
	private int queryStart;
	private int queryLength;

	private byte[] packedQuerySequence;
	private byte[] totalQuerySequence;

	// reference properties
	private int refStart;
	private int refLength;
	private int totalRefLength;

	private int subjectID;
	private String refName;

	// hit properties
	private int rawScore, bitScore;
	private int frame;
	private ArrayList<Byte> editByteOperations;
	private Integer[] editOperations;
	private String[] alignment;

	public void parseQueryProperties(long filePointer, ByteBuffer buffer, boolean parseAlignment, boolean storePackedDNASequence) {

		this.filePointer = filePointer;
		totalQueryLength = buffer.getInt();

		StringBuilder buf = new StringBuilder();
		int b;
		while ((b = buffer.get()) != 0)
			buf = buf.append((char) b);
		queryName = buf.toString();

		buffer.order(ByteOrder.BIG_ENDIAN);
		int flags = buffer.get() & 0xFF;
		boolean hasN = ((flags & 1) == 1);
		int bits = hasN ? 3 : 2;

		buffer.order(ByteOrder.LITTLE_ENDIAN);
		byte[] packed = new byte[(totalQueryLength * bits + 7) / 8];
		buffer.get(packed);

		if (storePackedDNASequence)
			packedQuerySequence = packed;

		if (parseAlignment)
			totalQuerySequence = getUnpackedSequence(packed, totalQueryLength, bits);

	}

	public void parseHitProperties(DAA_Header header, ByteBuffer buffer, boolean parseAlignment) throws IOException {

		subjectID = buffer.getInt();
		int flag = buffer.get() & 0xFF;

		rawScore = readPacked(flag & 3, buffer);
		bitScore = (int) Math.round((header.getLambda() * (double) rawScore - Math.log(header.getK())) / Math.log(2.));
		queryStart = readPacked((flag >>> 2) & 3, buffer);
		refStart = readPacked((flag >>> 4) & 3, buffer);
		refName = new String(header.getReferenceName(subjectID));
		totalRefLength = header.getRefLength(subjectID);

		frame = (flag & (1 << 6)) == 0 ? queryStart % 3 : 3 + (totalQueryLength - 1 - queryStart) % 3;
		frame = frame < 3 ? frame + 1 : -(frame - 2);

		queryLength = 0;
		refLength = 0;
		Vector<Integer> v = new Vector<Integer>();
		ArrayList<Byte> vByte = new ArrayList<Byte>();
		String aaString = AA_Alphabet.getAaString();
		byte opByte = buffer.get();
		int op = opByte & 0xFF;
		while (op != 0) {
			switch (op >>> 6) {
			case (0): // handling match
				queryLength += ((op & 63) * 3);
				refLength += (op & 63);
				break;
			case (1): // handling insertion
				queryLength += ((op & 63) * 3);
				break;
			case (2): // handling deletion
				refLength += 1;
				break;
			case (3): // handling substitution
				char c = aaString.charAt(op & 63);
				if (c == '/') {
					queryLength -= 1;
				} else if (c == '\\') {
					queryLength += 1;
				} else {
					queryLength += 3;
					refLength += 1;
				}
				break;

			}
			v.add(op);
			vByte.add(opByte);
			opByte = buffer.get();
			op = opByte & 0xFF;
		}
		editOperations = v.toArray(new Integer[v.size()]);
		editByteOperations = vByte;

		if (parseAlignment)
			alignment = computeAlignment();

	}

	public String[] computeAlignment() {

		StringBuilder[] bufs = { new StringBuilder(), new StringBuilder() };

		// String aaString = "ARNDCQEGHILKMFPSTWYVBJZX*";
		String aaString = AA_Alphabet.getAaString();
		String queryDNA = getQueryDNA();
		CodonTranslator aaTranslator = new CodonTranslator();

		int q = 0;
		for (int editOp : editOperations) {
			switch (editOp >>> 6) {
			case (0): // handling match
				for (int i = 0; i < (editOp & 63); i++) {
					char aa = aaTranslator.translateCodon(queryDNA.substring(q, q + 3));
					bufs[0].append(aa);
					bufs[1].append(aa);
					q += 3;
				}
				break;
			case (1): // handling insertion
				for (int i = 0; i < (editOp & 63); i++) {
					char aa = aaTranslator.translateCodon(queryDNA.substring(q, q + 3));
					bufs[0].append(aa);
					bufs[1].append('-');
					q += 3;
				}
				break;
			case (2): // handling deletion
				char c = aaString.charAt(editOp & 63);
				bufs[0].append('-');
				bufs[1].append(c);
				break;
			case (3): // handling substitution
				c = aaString.charAt(editOp & 63);
				if (c == '/') {
					bufs[0].append("/");
					bufs[1].append("-");
					q -= 1;
				} else if (c == '\\') {
					bufs[0].append("\\");
					bufs[1].append("-");
					q += 1;
				} else {
					char aa = aaTranslator.translateCodon(queryDNA.substring(q, q + 3));
					bufs[0].append(aa);
					bufs[1].append(c);
					q += 3;
				}
				break;
			}
		}

		String[] alignment = { bufs[0].toString(), bufs[1].toString() };
		return alignment;

	}

	public int readPacked(int kind, ByteBuffer buffer) {
		switch (kind) {
		case 0:
			return buffer.get() & 0xFF;
		case 1:
			return buffer.getChar();
		case 2:
			return buffer.getInt();
		default:
			throw new RuntimeException("unknown kind: " + kind);
		}
	}

	private String reverseComplementString(String s) {
		String rev = new StringBuilder(s).reverse().toString();
		String revComp = rev.replace("A", "t").replace("T", "a").replace("C", "g").replace("G", "c").toUpperCase();
		return revComp;
	}

	public String getQueryDNA() {
		String querySeq = getTotalQueryDNA();
		querySeq = frame > 0 ? querySeq : reverseComplementString(querySeq);
		int start = frame > 0 ? queryStart : totalQueryLength - queryStart - 1;
		String queryDNA = (String) querySeq.subSequence(start, start + queryLength);
		return queryDNA;
	}

	public byte[] getPackedQuerySequence() {
		return packedQuerySequence;
	}

	public String getTotalQueryDNA() {
		char[] sigma = { 'A', 'C', 'G', 'T' };
		StringBuilder buf = new StringBuilder();
		for (byte a : totalQuerySequence)
			buf.append(sigma[a]);
		return buf.toString();
	}

	public static String toStringUnpacked(byte[] unpacked) {
		StringBuilder buf = new StringBuilder();
		for (byte a : unpacked)
			buf.append(String.format("%d", a));
		return buf.toString();
	}

	public static byte[] getUnpackedSequence(byte[] packed, int query_len, int bits) {
		byte[] result = new byte[query_len];
		long x = 0;
		int n = 0, l = 0;
		int mask = (1 << bits) - 1;

		for (int i = 0; i < packed.length; i++) {
			x |= (packed[i] & 0xFF) << n;
			n += 8;

			while (n >= bits && l < query_len) {
				result[l] = (byte) (x & mask);
				n -= bits;
				x >>>= bits;
				l++;
			}
		}
		return result;
	}

	public int getSubjectID() {
		return subjectID;
	}

	public long getFilePointer() {
		return filePointer;
	}

	public String getQueryName() {
		return queryName;
	}

	public int getQueryStart() {
		return queryStart;
	}

	public int getQueryLength() {
		return queryLength;
	}

	public int getRefStart() {
		return refStart;
	}

	public int getRefLength() {
		return refLength;
	}

	public int getTotalRefLength() {
		return totalRefLength;
	}

	public String getReferenceName() {
		return refName;
	}

	public int getRawScore() {
		return rawScore;
	}

	public int getBitScore() {
		return bitScore;
	}

	public int getFrame() {
		return frame;
	}

	public String[] getAlignment() {
		return alignment;
	}

	public ArrayList<Byte> getEditByteOperations() {
		return editByteOperations;
	}

	public int getTotalQueryLength() {
		return totalQueryLength;
	}

	public byte[] getTotalQuerySequence() {
		return totalQuerySequence;
	}

	public void copyQueryProperties(DAA_Hit hit) {
		totalQueryLength = hit.getTotalQueryLength();
		totalQuerySequence = hit.getTotalQuerySequence();
		queryName = hit.getQueryName();
		queryStart = hit.getQueryStart();
		queryLength = hit.getQueryLength();
		packedQuerySequence = hit.getPackedQuerySequence();
	}

}
