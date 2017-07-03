package daa.writer;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicLong;

import hits.Hit;
import hits.Hit.FrameDirection;
import util.SparseString;

public class DAA_Writer {

	private static final HashMap<Character, Integer> nucToIndex;
	static {
		nucToIndex = new HashMap<Character, Integer>();
		nucToIndex.put('A', 0);
		nucToIndex.put('C', 1);
		nucToIndex.put('G', 2);
		nucToIndex.put('T', 3);
	}
	private AtomicLong queryRecords = new AtomicLong(0), aliBlockSize = new AtomicLong(0), refNamesBlockSize = new AtomicLong(0),
			refLengthsBlockSize = new AtomicLong(0);

	private File out;

	public DAA_Writer(File out) {
		this.out = out;
	}

	public void writeHeader(Long dbSeqs, BigInteger dbLetters, Integer gapOpen, Integer gapExtend, Double k, Double lambda) {

		// filling long-section
		ArrayList<Byte> byteBuffer = new ArrayList<Byte>();
		long magicNumber = Long.valueOf("4327487858190246763");
		write(byteBuffer, readLittleEndian(magicNumber));
		long version = 0;
		write(byteBuffer, readLittleEndian(version));
		long diamondBuild = 0;
		write(byteBuffer, readLittleEndian(diamondBuild));
		dbSeqs = dbSeqs != null ? dbSeqs : 0;
		write(byteBuffer, readLittleEndian(dbSeqs));
		long dbSeqsUsed = 0;
		write(byteBuffer, readLittleEndian(dbSeqsUsed));
		dbLetters = dbLetters != null ? dbLetters : BigInteger.valueOf(0);
		write(byteBuffer, readLittleEndian(dbLetters.longValue()));
		long flags = 0;
		write(byteBuffer, readLittleEndian(flags));
		long queryRecords = 0;
		write(byteBuffer, readLittleEndian(queryRecords));

		// filling integer-section
		int modeRank = 3;
		write(byteBuffer, readLittleEndian(modeRank));
		gapOpen = gapOpen != null ? gapOpen : 0;
		write(byteBuffer, readLittleEndian(gapOpen));
		gapExtend = gapExtend != null ? gapExtend : 0;
		write(byteBuffer, readLittleEndian(gapExtend));
		int reward = 0;
		write(byteBuffer, readLittleEndian(reward));
		int penalty = 0;
		write(byteBuffer, readLittleEndian(penalty));
		int reserved1 = 0;
		write(byteBuffer, readLittleEndian(reserved1));
		int reserved2 = 0;
		write(byteBuffer, readLittleEndian(reserved2));
		int reserved3 = 0;
		write(byteBuffer, readLittleEndian(reserved3));

		// filling double-section
		k = k != null ? k : 0;
		write(byteBuffer, readLittleEndian(k));
		lambda = lambda != null ? lambda : 0;
		write(byteBuffer, readLittleEndian(lambda));
		double reserved4 = 0;
		write(byteBuffer, readLittleEndian(reserved4));
		double reserved5 = 0;
		write(byteBuffer, readLittleEndian(reserved5));

		// filling block-section
		for (int i = 128; i < 2192; i++)
			write(byteBuffer, readLittleEndian((byte) 0));
		for (int i = 2192; i < 2448; i++) {
			int rank = i - 2192 + 1;
			byte blockTypeRank = rank < 4 ? (byte) rank : 0;
			write(byteBuffer, readLittleEndian(blockTypeRank));
		}

		// writing-out buffer
		byte[] stream = new byte[byteBuffer.size()];
		for (int i = 0; i < byteBuffer.size(); i++)
			stream[i] = byteBuffer.get(i);
		writeInFile(stream, stream.length, false);

	}

	public void writeHits(ArrayList<Hit> hits) {

		ArrayList<Byte> byteBuffer = new ArrayList<Byte>();
		HashMap<String, byte[]> readIDToPackedSeq = new HashMap<String, byte[]>();

		Vector<int[]> allocPairs = new Vector<int[]>();
		String lastReadName = null;
		int begin = -1, alloc = -1;

		for (Hit h : hits) {

			String readName = h.getReadName();

			if (lastReadName == null || !h.getReadName().equals(lastReadName)) {

				if (alloc != -1) {
					alloc = byteBuffer.size() - 4 - begin;
					int[] allocPair = { begin, alloc };
					allocPairs.add(allocPair);
				}

				begin = byteBuffer.size();

				alloc = 0;
				write(byteBuffer, readLittleEndian(alloc));

				int totalQueryLength = h.getTotalQueryLenth();
				write(byteBuffer, readLittleEndian(totalQueryLength));

				String queryName = readName;
				write(byteBuffer, readLittleEndian(queryName));
				write(byteBuffer, (byte) 0);

				byte nFlag = 0;
				write(byteBuffer, nFlag);

				if (!readIDToPackedSeq.containsKey(queryName)) {
					byte[] packedSequence = h.getPackedQuerySequence();
					readIDToPackedSeq.put(queryName, packedSequence);
				}
				byte[] packedSequence = readIDToPackedSeq.get(readName);
				write(byteBuffer, packedSequence);

			}

			int subjectID = h.getSubjectID();
			write(byteBuffer, readLittleEndian(subjectID));

			byte typeFlags = 1 << 1;
			typeFlags |= 1 << 3;
			typeFlags |= 1 << 5;
			typeFlags |= h.getFrame() == FrameDirection.POSITIVE ? 0 : 1 << 6;
			write(byteBuffer, typeFlags);

			int rawScore = h.getRawScore();
			write(byteBuffer, readLittleEndian(rawScore));

			int queryStart = h.getQuery_start();
			write(byteBuffer, readLittleEndian(queryStart));

			int refStart = h.getRef_start();
			write(byteBuffer, readLittleEndian(refStart));

			ArrayList<Byte> editOperations = h.getEditOperations();
			for (byte op : editOperations)
				write(byteBuffer, op);
			write(byteBuffer, (byte) 0);

			lastReadName = h.getReadName();

		}

		if (alloc != -1) {

			alloc = byteBuffer.size() - 4 - begin;
			int[] allocPair = { begin, alloc };
			allocPairs.add(allocPair);

			byte[] stream = new byte[byteBuffer.size()];
			for (int i = 0; i < byteBuffer.size(); i++)
				stream[i] = byteBuffer.get(i);

			for (int[] p : allocPairs) {
				int counter = 0;
				for (byte b : readLittleEndian(p[1])) {
					int pos = p[0] + (counter++);
					stream[pos] = b;
				}
			}

			writeInFile(stream, stream.length, true);

			aliBlockSize.getAndAdd(stream.length);
			queryRecords.getAndAdd(allocPairs.size());

		}

	}

	private void write(ArrayList<Byte> byteBuffer, byte b) {
		byteBuffer.add(b);
	}

	private void write(ArrayList<Byte> byteBuffer, byte[] bytes) {
		for (byte b : bytes)
			byteBuffer.add(b);
	}

	private byte[] readLittleEndian(String s) {
		ByteBuffer buffer = ByteBuffer.allocate(s.length());
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < s.length(); i++)
			buffer.put((byte) s.charAt(i));
		return buffer.array();
	}

	private byte[] readLittleEndian(byte o) {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(o);
		return buffer.array();
	}

	private byte[] readLittleEndian(int o) {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putInt(o);
		return buffer.array();
	}

	private byte[] readLittleEndian(double o) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putDouble(o);
		return buffer.array();
	}

	private byte[] readLittleEndian(long o) {
		ByteBuffer buffer = ByteBuffer.allocate(8);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		buffer.putLong(o);
		return buffer.array();
	}

	public void writeEnd(ArrayList<Object[]> subjectInfo) {
		try {

			// finishing alignment block
			writeInFile(readLittleEndian((int) 0), 4, true);
			aliBlockSize.getAndAdd(4);

			// inserting reference names
			ArrayList<Byte> byteBuffer = new ArrayList<Byte>();
			for (Object[] o : subjectInfo) {
				String refName = ((SparseString) o[0]).toString();
				write(byteBuffer, readLittleEndian(refName));
				write(byteBuffer, (byte) 0);
			}
			refNamesBlockSize.getAndSet(byteBuffer.size());

			// inserting reference length
			for (Object[] o : subjectInfo) {
				int refLength = ((int) o[1]);
				write(byteBuffer, readLittleEndian(refLength));
			}
			refLengthsBlockSize.getAndSet(byteBuffer.size() - refNamesBlockSize.get());

			// writing-out buffer
			byte[] stream = new byte[byteBuffer.size()];
			for (int i = 0; i < byteBuffer.size(); i++)
				stream[i] = byteBuffer.get(i);
			writeInFile(stream, stream.length, true);

			// updating #dbSeqsUsed
			writeByteInFile(readLittleEndian((long) subjectInfo.size()), 32);

			// updating #queryRecords
			writeByteInFile(readLittleEndian((long) queryRecords.longValue()), 56);

			// updating alignment block size
			writeByteInFile(readLittleEndian(aliBlockSize.get()), 144);

			// updating refNames block size
			writeByteInFile(readLittleEndian(refNamesBlockSize.get()), 152);

			// updating refLengths block size
			writeByteInFile(readLittleEndian(refLengthsBlockSize.get()), 160);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private synchronized void writeByteInFile(byte[] b, long pos) {
		try {
			RandomAccessFile raf = new RandomAccessFile(out, "rw");
			try {
				raf.seek(pos);
				raf.write(b);
			} finally {
				raf.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private synchronized void writeInFile(byte[] b, int len, boolean append) {
		try {
			OutputStream output = null;
			try {
				output = new BufferedOutputStream(new FileOutputStream(out, append));
				output.write(b, 0, len);
			} finally {
				if (output != null)
					output.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getTotalQueryDNA(byte[] unpackedSequence) {
		char[] sigma = { 'A', 'C', 'G', 'T' };
		StringBuilder buf = new StringBuilder();
		for (byte a : unpackedSequence) {
			buf.append(sigma[a]);
		}
		return buf.toString();
	}

	public String toStringUnpacked(byte[] unpacked) {
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

}
