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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import maf.MAF_StreamConverter.SubjectEntry;
import util.SparseString;

public class DAA_Header {

	private File daaFile;

	// header one
	protected long magicNumber;
	protected long version;

	// header two
	protected BigInteger dbLetters;
	protected long diamondBuild, dbSeqs, dbSeqsUsed, flags, queryRecords;
	protected int modeRank, gapOpen, gapExtend, reward, penalty, reserved1, reserved2, reserved3;

	protected double k, lambda, reserved4, reserved5;
	protected final byte[] scoreMatrix = new byte[16];
	protected final long[] blockSize = new long[256];
	protected final byte[] blockTypeRank = new byte[256];

	// reference information
	private byte[][] referenceNames;
	protected int[] refLengths;

	private final int referenceLocationChunkBits = 6; // 6 bits = 64 chunk size
	private final int referenceLocationChunkSize = 1 << referenceLocationChunkBits;
	private long[] referenceLocations; // location of every 2^referenceLocationChunkBits reference

	// reference annotations:
	protected int numberOfRefAnnotations;
	protected int[][] refAnnotations = new int[256][];
	protected String[] refAnnotationNames = new String[256];
	protected int refAnnotationIndexForTaxonomy = -1;

	// helper variables:
	protected String scoreMatrixName;

	protected long headerSize;

	protected int refNamesBlockIndex = -1;
	protected int refLengthsBlockIndex = -1;
	protected int alignmentsBlockIndex = -1;

	public DAA_Header(File daaFile) {
		this.daaFile = daaFile;
		if (magicNumber == 0)
			load();
	}

	private void load() {

		try {

			InputStream is = new BufferedInputStream(new FileInputStream(daaFile));
			ByteBuffer buffer = ByteBuffer.allocate(2448);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			is.read(buffer.array());

			try {

				magicNumber = buffer.getLong(0);
				version = buffer.getLong(8);

				diamondBuild = buffer.getLong(16);
				dbSeqs = buffer.getLong(24);
				dbSeqsUsed = buffer.getLong(32);
				dbLetters = BigInteger.valueOf(buffer.getLong(40));
				flags = buffer.getLong(48);
				queryRecords = buffer.getLong(56);

				modeRank = buffer.getInt(64);
				gapOpen = buffer.getInt(68);
				gapExtend = buffer.getInt(72);
				reward = buffer.getInt(76);
				penalty = buffer.getInt(80);
				reserved1 = buffer.getInt(84);
				reserved2 = buffer.getInt(88);
				reserved3 = buffer.getInt(92);

				k = buffer.getDouble(96);
				lambda = buffer.getDouble(104);
				reserved4 = buffer.getDouble(112);
				reserved5 = buffer.getDouble(120);

				buffer.position(128);
				for (int i = 0; i < scoreMatrix.length; i++)
					scoreMatrix[i] = (byte) buffer.get();

				buffer.position(144);
				for (int i = 0; i < blockSize.length; i++) {
					blockSize[i] = buffer.getLong();
				}

				buffer.position(2192);
				for (int i = 0; i < blockTypeRank.length; i++) {
					blockTypeRank[i] = (byte) buffer.get();
					switch (blockTypeRank[i]) {
					case 1:
						alignmentsBlockIndex = i;
						break;
					case 2:
						refNamesBlockIndex = i;
						break;
					case 3:
						refLengthsBlockIndex = i;
						break;
					}
				}

				buffer.position(2448);
				headerSize = buffer.position();

			} finally {
				is.close();
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void loadAllReferences() {
		try {
			RandomAccessFile raf = new RandomAccessFile(daaFile, "r");
			try {

				raf.seek(getLocationOfBlockInFile(refNamesBlockIndex));

				referenceNames = new byte[(int) dbSeqsUsed][];
				referenceLocations = new long[1 + ((int) dbSeqsUsed >>> referenceLocationChunkBits)];
				for (int i = 0; i < (int) dbSeqsUsed; i++) {
					if ((i & (referenceLocationChunkSize - 1)) == 0) {
						referenceLocations[i >>> referenceLocationChunkBits] = raf.getFilePointer();
					}
					int c = raf.read();
					while (c != 0)
						c = raf.read();
				}

				refLengths = new int[(int) dbSeqsUsed];
				for (int i = 0; i < dbSeqsUsed; i++) {
					ByteBuffer buffer = ByteBuffer.allocate(4);
					buffer.order(ByteOrder.LITTLE_ENDIAN);
					raf.read(buffer.array());
					refLengths[i] = buffer.getInt();
				}

			} finally {
				raf.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public byte[] getReferenceName(int index) throws IOException {

		if (referenceNames[index] != null)
			return referenceNames[index];

		RandomAccessFile raf = new RandomAccessFile(daaFile, "r");

		try {

			int iChunk = (index >>> referenceLocationChunkBits);
			raf.seek(referenceLocations[iChunk]);

			int start = iChunk * referenceLocationChunkSize;
			int stop = Math.min((int) dbSeqsUsed, start + referenceLocationChunkSize);

			for (int i = start; i < stop; i++) {
				StringBuffer buf = new StringBuffer();
				byte b = (byte) raf.read();
				while (b != 0) {
					buf.append((char) b);
					b = (byte) raf.read();
				}
				referenceNames[i] = buf.toString().getBytes();
			}

		} finally {
			raf.close();
		}

		return referenceNames[index];
	}

	public int getAlignmentsBlockIndex() {
		return alignmentsBlockIndex;
	}

	public long getLocationOfBlockInFile(int blockIndex) {
		long location = headerSize;
		for (int i = 0; i < blockIndex; i++)
			location += blockSize[i];
		return location;
	}

	public long getNumberOfQueryRecords() {
		return queryRecords;
	}

	public int getRefLength(int i) {
		return refLengths[i];
	}

	public double getK() {
		return k;
	}

	public double getLambda() {
		return lambda;
	}

	public BigInteger getDbLetters() {
		return dbLetters;
	}

	public long getHeaderSize() {
		return headerSize;
	}

	public long getDbSeqsUsed() {
		return dbSeqsUsed;
	}

	public int getGapOpen() {
		return gapOpen;
	}

	public int getGapExtend() {
		return gapExtend;
	}

	public void print() {

		System.out.println("DIAMOND HEADER *********************\n");

		System.out.println("Magic Number: " + this.magicNumber);
		System.out.println("Version: " + this.version);

		System.out.println();
		System.out.println("diamondBuild: " + diamondBuild);
		System.out.println("dbSeqs: " + dbSeqs);
		System.out.println("dbSeqsUsed: " + dbSeqsUsed);
		System.out.println("dbLetters: " + dbLetters);
		System.out.println("flags: " + flags);
		System.out.println("queryRecords: " + queryRecords);

		System.out.println();
		System.out.println("modeRank: " + modeRank);
		System.out.println("gapOpen: " + gapOpen);
		System.out.println("gapExtend: " + gapExtend);
		System.out.println("reward: " + reward);
		System.out.println("penalty: " + penalty);
		System.out.println("reserved1: " + reserved1);
		System.out.println("reserved2: " + reserved2);
		System.out.println("reserved3: " + reserved3);

		System.out.println();
		System.out.println("k: " + k);
		System.out.println("lambda: " + lambda);
		System.out.println("reserved4: " + reserved4);
		System.out.println("reserved5: " + reserved5);

		System.out.println();
		System.out.print("ScoringMatrix: ");
		for (int i = 0; i < scoreMatrix.length; i++)
			System.out.print((char) scoreMatrix[i]);

		System.out.println();
		for (int i = 0; i < blockSize.length; i++)
			System.out.println("BlockSize-" + i + ": " + blockSize[i]);

		System.out.println("alignmentsBlockIndex: " + this.alignmentsBlockIndex + " " + blockSize[alignmentsBlockIndex]);
		System.out.println("refNamesBlockIndex: " + this.refNamesBlockIndex + " " + blockSize[refNamesBlockIndex]);
		System.out.println("refLengthsBlockIndex: " + this.refLengthsBlockIndex + " " + blockSize[refLengthsBlockIndex]);

		System.out.println("**************************************\n");

	}

}
