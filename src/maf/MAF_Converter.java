package maf;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import daa.writer.DAA_Writer;
import hits.Hit;
import io.FastAQ_Reader;
import util.LineCounter;
import util.SparseString;

public class MAF_Converter {

	private int maxProgress, lastProgress = 0;
	private AtomicInteger progress = new AtomicInteger();

	private CountDownLatch latch;
	private ExecutorService executor;

	public void run(File daaFile, File mafFile, File queryFile, int cores, boolean verbose, File headerFile) {

		long time = System.currentTimeMillis();
		System.out.println("\nConverting " + mafFile.getName() + " to " + daaFile.getName() + "...");

		this.executor = Executors.newFixedThreadPool(cores);
		MAF_Header headerInfo = headerFile == null ? new MAF_Header(mafFile) : new MAF_Header(headerFile);
		headerInfo.load();

		long numOfLines_header = headerFile != null ? LineCounter.run(headerFile) : countHeaderLines(mafFile);
		long numOfLines = LineCounter.run(mafFile);
		long chunk = (long) Math.ceil((double) numOfLines / (double) cores);

		// parsing read information
		System.out.println("STEP 1 - Processing read-file: " + queryFile.getAbsolutePath());
		ArrayList<Object[]> readInfos = FastAQ_Reader.read(queryFile);
		if (verbose)
			System.out.println(readInfos.size() + " reads processed!");

		// processing maf file
		System.out.println("STEP 2 - Processing maf-file: " + mafFile.getAbsolutePath());
		maxProgress = (int) numOfLines;
		ConcurrentSkipListSet<SubjectEntry> subjectInfoSet = new ConcurrentSkipListSet<SubjectEntry>();
		ConcurrentSkipListSet<Long> batchSet = new ConcurrentSkipListSet<Long>();
		ArrayList<Thread> processThreads = generateProcessThreads(mafFile, chunk, subjectInfoSet, batchSet, readInfos);
		runInParallel(processThreads);
		ArrayList<Object[]> subjectInfos = new ArrayList<Object[]>();
		Iterator<SubjectEntry> it = subjectInfoSet.iterator();
		while (it.hasNext()) {
			SubjectEntry e = it.next();
			Object[] subject = { e.getName(), e.getLength() };
			subjectInfos.add(subject);
		}
		reportFinish();
		if (verbose)
			System.out.println(subjectInfos.size() + " references processed!");

		// writing header of daa file
		DAA_Writer daaWriter = new DAA_Writer(daaFile);
		daaWriter.writeHeader(headerInfo.getDbSeqs(), headerInfo.getDbLetters(), headerInfo.getGapOpen(), headerInfo.getGapExtend(),
				headerInfo.getK(), headerInfo.getLambda());

		// writing hits into daa file
		System.out.println("STEP 3 - Writing into daa-file: " + daaFile.getAbsolutePath());
		maxProgress = (int) numOfLines - (int) numOfLines_header;
		progress.set(0);
		ArrayList<Thread> batchReaders = new ArrayList<Thread>();
		for (long filePointer : batchSet)
			batchReaders.add(new BatchReader(filePointer, mafFile, subjectInfos));
		ArrayList<Hit> hits = new ArrayList<Hit>();
		long hitCounter = 0;
		for (int i = 0; i < readInfos.size(); i++) {

			Object[] readInfo = readInfos.get(i);

			// reading-out hits in parallel
			for (Thread reader : batchReaders)
				((BatchReader) reader).setReadName(readInfo);
			runInParallel(batchReaders);

			// storing hits
			for (Thread reader : batchReaders) {
				for (MAF_Hit mafHit : ((BatchReader) reader).getHits())
					hits.add(new Hit(mafHit));
			}

			// writing hits into daa file
			if (hits.size() > 10000 || i == readInfos.size() - 1) {
				hitCounter += hits.size();
				daaWriter.writeHits(hits);
				hits.clear();
			}

		}

		// writing subject info into daa file
		daaWriter.writeEnd(subjectInfos);

		reportFinish();
		if (verbose)
			System.out.println(hitCounter + " alignments written into DAA-File!");

		executor.shutdown();

		long runtime = (System.currentTimeMillis() - time) / 1000;
		System.out.println("Runtime: " + (runtime / 60) + "min " + (runtime % 60) + "s");

	}

	private int countHeaderLines(File mafFile) {
		int counter = 0;
		try {
			String l;
			BufferedReader buf = new BufferedReader(new FileReader(mafFile));
			while ((l = buf.readLine()) != null) {
				if (!l.startsWith("# batch"))
					counter++;
				else {
					buf.close();
					return counter;
				}
			}
			buf.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return counter;
	}

	private void reportProgress(int delta) {
		progress.getAndAdd(delta);
		int p = ((int) ((((double) progress.get() / (double) maxProgress)) * 100) / 10) * 10;
		if (p > lastProgress && p < 100) {
			lastProgress = p;
			System.out.print(p + "% ");
		}
	}

	private void reportFinish() {
		progress.set(0);
		lastProgress = 0;
		System.out.print(100 + "%\n");
	}

	public class BatchReader extends Thread {

		private ArrayList<Object[]> subjectInfo;
		private RandomAccessFile raf;
		private Object[] readInfo;
		private ArrayList<MAF_Hit> hits;

		private int parsedLines = 0;
		private boolean endOfBatchReached = false;
		private MAF_Hit lastParsedHit;
		private byte[] buffer = new byte[1024 * 1024];
		private int readChars;
		private int last_i = 0;

		int counter = 0;

		public BatchReader(long filePointer, File mafFile, ArrayList<Object[]> subjectInfo) {
			try {
				raf = new RandomAccessFile(mafFile, "r");
				raf.seek(filePointer);
				readChars = raf.read(buffer);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			this.subjectInfo = subjectInfo;
		}

		public void run() {

			hits = new ArrayList<MAF_Hit>();
			if (lastParsedHit != null) {
				if (!lastParsedHit.getReadName().equals(readInfo[0].toString())) {
					latch.countDown();
					return;
				}

				lastParsedHit.setReadInfo(readInfo);
				hits.add(lastParsedHit);
				lastParsedHit = null;

			}

			if (endOfBatchReached) {
				latch.countDown();
				return;
			}

			try {

				StringBuilder line = new StringBuilder();
				String[] lineTriple = new String[3];
				for (int i = last_i; i < readChars; i++) {

					char c = (char) buffer[i];

					if (c != '\n')
						line.append(c);
					else {

						parsedLines++;
						if (parsedLines > 0 && parsedLines % 10 == 0)
							reportProgress(10);

						last_i = i + 1;
						String l = line.toString();
						line = new StringBuilder();

						if (l.startsWith("s") && lineTriple[1] != null) {

							lineTriple[2] = l;

							MAF_Hit hit = new MAF_Hit(lineTriple, readInfo, subjectInfo);
							if (!hit.getReadName().equals(readInfo[0].toString())) {
								lastParsedHit = hit;
								break;
							}
							hit.setReadInfo(readInfo);
							if (hit.makesSense()) {
								hits.add(hit);
							}

							lineTriple = new String[3];

						}

						else if (l.startsWith("a"))
							lineTriple[0] = l;

						else if (l.startsWith("s") && lineTriple[1] == null)
							lineTriple[1] = l;

					}

					if (i == readChars - 1) {
						i = -1;
						readChars = raf.read(buffer);
					}

				}
			} catch (

			Exception e) {
				e.printStackTrace();
			}

			latch.countDown();

		}

		public void setReadName(Object[] readInfo) {
			this.readInfo = readInfo;
		}

		public ArrayList<MAF_Hit> getHits() {
			return hits;
		}

	}

	public void runInParallel(ArrayList<Thread> threads) {
		latch = new CountDownLatch(threads.size());
		for (Thread t : threads)
			executor.execute(t);
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class ProcessThread extends Thread {

		private File mafFile;
		private long startPos, chunkSize;
		private ConcurrentSkipListSet<SubjectEntry> subjectInfo_Set;
		private ConcurrentSkipListSet<Long> batchSet;
		private ArrayList<Object[]> readInfos;
		private boolean processHeader;

		public ProcessThread(File mafFile, long startPos, long chunkSize, ConcurrentSkipListSet<SubjectEntry> subjectInfo_Set,
				ConcurrentSkipListSet<Long> batchSet, ArrayList<Object[]> readInfos, boolean firstThread) {
			this.mafFile = mafFile;
			this.startPos = startPos;
			this.chunkSize = chunkSize;
			this.subjectInfo_Set = subjectInfo_Set;
			this.batchSet = batchSet;
			this.readInfos = readInfos;
			this.processHeader = firstThread;
		}

		public void run() {

			try {

				RandomAccessFile raf = new RandomAccessFile(mafFile, "r");
				raf.seek(startPos);

				byte[] buffer = new byte[1024 * 1024];
				int readChars = 0, colNumber = 0, parsedLines = 0;
				boolean enterRefLine = false, isRefLine = false, isQueryLine = false, isHashLine = false, startNewBatchBlock = false,
						startNewBatchSubBlock = false, doBreak = false;
				int readIndex = 0, lastReadIndex = 0;
				Character lastChar = null;

				StringBuilder buf = new StringBuilder();
				StringBuilder line = new StringBuilder();
				Object[] subject = new Object[2];
				while ((readChars = raf.read(buffer)) != -1) {

					if (doBreak)
						break;

					int lastI = 0;
					for (int i = 0; i < readChars; i++) {

						if (doBreak)
							break;

						char c = (char) buffer[i];
						if (c == '\n') {
							parsedLines++;
							if (parsedLines % 100 == 0)
								reportProgress(100);
						}

						switch (c) {
						case '\n':
							if (isRefLine) {
								if (subject[0] != null && subject[1] != null)
									subjectInfo_Set.add(new SubjectEntry((SparseString) subject[0], (int) subject[1]));
								isRefLine = false;

							}

							if (startNewBatchBlock && processHeader) {
								batchSet.add(raf.getFilePointer() - (readChars - i));
								startNewBatchBlock = false;
								readIndex = 0;
								lastReadIndex = 0;
								processHeader = false;
							}
							if (startNewBatchSubBlock) {
								batchSet.add(raf.getFilePointer() - (readChars - lastI));
								startNewBatchSubBlock = false;
							}
							if (isQueryLine) {
								lastI = i;
								isQueryLine = false;
								if (parsedLines > chunkSize)
									doBreak = true;
							}
							buf = new StringBuilder();
							line = new StringBuilder();
							colNumber = 0;
							break;
						case ' ':
							if (lastChar != null && lastChar != ' ') {
								String content = buf.toString();

								if (colNumber == 0 && content.equals("a"))
									enterRefLine = true;
								if (colNumber == 0 && content.equals("s") && enterRefLine) {
									enterRefLine = false;
									isRefLine = true;
									subject = new Object[2];
								}
								if (colNumber == 0 && content.equals("s") && !isRefLine)
									isQueryLine = true;
								if (colNumber == 0 && content.equals("#"))
									isHashLine = true;

								if (isQueryLine && colNumber == 1) {
									String query = content;
									while (!readInfos.get(readIndex)[0].toString().equals(query)) {
										readIndex++;
										if (readIndex == readInfos.size()) {
											readIndex = 0;
										}
									}
									if (readIndex < lastReadIndex)
										startNewBatchSubBlock = true;
									lastReadIndex = readIndex;
								}
								if (isRefLine && colNumber == 1)
									subject[0] = new SparseString(buf.toString());
								if (isHashLine && colNumber == 1 && buf.toString().equals("batch")) {
									subject[0] = new SparseString(buf.toString());
									isHashLine = false;
									startNewBatchBlock = true;
								}
								if (isRefLine && colNumber == 5)
									subject[1] = Integer.parseInt(buf.toString());
								buf = new StringBuilder();
								line.append(c);
								colNumber++;
							}
							break;
						default:
							buf.append(c);
							line.append(c);
						}

						lastChar = c;

					}

				}

				raf.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

			latch.countDown();

		}

	}

	public ArrayList<Thread> generateProcessThreads(File file, long chunk, ConcurrentSkipListSet<SubjectEntry> subjectInfo_Set,
			ConcurrentSkipListSet<Long> batchSet, ArrayList<Object[]> readInfos) {

		ArrayList<Thread> processThreads = new ArrayList<Thread>();
		try {

			InputStream is;
			try {
				is = new BufferedInputStream(new GZIPInputStream(new FileInputStream(file)));
			} catch (ZipException e) {
				is = new BufferedInputStream(new FileInputStream(file));
			}

			try {
				boolean initNewThread = false;
				byte[] c = new byte[1024];
				int count = 0;
				int readChars = 0;
				long filePointer = 0;
				processThreads.add(new ProcessThread(file, filePointer, chunk, subjectInfo_Set, batchSet, readInfos, true));
				while ((readChars = is.read(c)) != -1) {
					for (int i = 0; i < readChars; ++i) {
						filePointer++;
						if (c[i] == '\n') {
							count++;
							if (count % (chunk + 1) == 0)
								initNewThread = true;
							if (initNewThread && i > 0 && c[i - 1] == '\n') {
								processThreads.add(new ProcessThread(file, filePointer, chunk, subjectInfo_Set, batchSet, readInfos, false));
								initNewThread = false;
							}
						}
					}
				}
				return processThreads;
			} finally {
				is.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return processThreads;

	}

	public class SubjectEntry implements Comparable<SubjectEntry> {

		private SparseString name;
		private int length;

		public SubjectEntry(SparseString name, int length) {
			this.name = name;
			this.length = length;
		}

		@Override
		public int compareTo(SubjectEntry o) {
			return name.compareTo(o.getName());
		}

		public SparseString getName() {
			return name;
		}

		public int getLength() {
			return length;
		}

	}

}
