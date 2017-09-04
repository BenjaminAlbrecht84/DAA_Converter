package maf;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import daa.reader.DAA_Header;
import daa.reader.DAA_Hit;
import daa.reader.DAA_Reader;
import daa.writer.DAA_Writer;
import hits.Hit;
import io.FastAQ_Reader;
import util.Hit_Filter;
import util.SparseString;

public class MAF_StreamConverter {

	private int maxProgress, lastProgress = 0;
	private AtomicInteger progress = new AtomicInteger();

	private CountDownLatch latch;
	private ExecutorService executor;

	public void run(File daaFile, ArrayList<File> daaFiles, File queryFile, int cores, boolean verbose, File headerFile, boolean doFiltering) {

		long time = System.currentTimeMillis();
		System.out.println("\nConverting batch files to " + daaFile.getAbsolutePath() + "...");

		this.executor = Executors.newFixedThreadPool(cores);
		Header headerInfo = new Header();
		if (headerFile.getName().endsWith("maf"))
			headerInfo.loadFromMaf(headerFile);
		if (headerFile.getName().endsWith("daa"))
			headerInfo.loadFromDAA(headerFile);

		ArrayList<DAA_Reader> daaReader = new ArrayList<DAA_Reader>();
		for (File f : daaFiles)
			daaReader.add(new DAA_Reader(f, false));

		// processing maf file
		System.out.println("STEP 1 - Processing daaFiles ");
		maxProgress = getTotalSeqUsed(daaReader);
		ConcurrentSkipListSet<SubjectEntry> subjectInfoSet = new ConcurrentSkipListSet<SubjectEntry>();
		ArrayList<Thread> processThreads = generateProcessThreads(daaReader, subjectInfoSet);
		runInParallel(processThreads);
		ArrayList<Object[]> subjectInfos = new ArrayList<Object[]>();
		Iterator<SubjectEntry> it = subjectInfoSet.iterator();
		while (it.hasNext()) {
			SubjectEntry e = it.next();
			Object[] subject = { e.getName(), e.getLength() };
			subjectInfos.add(subject);
		}
		if (verbose)
			System.out.println(subjectInfos.size() + " references processed!");
		reportFinish();

		// parsing read information
		System.out.println("STEP 2 - Processing read-file: " + queryFile.getAbsolutePath());
		ArrayList<Object[]> readInfos = FastAQ_Reader.read(queryFile);
		if (verbose)
			System.out.println(readInfos.size() + " reads processed!");

		// writing header of daa file
		DAA_Writer daaWriter = new DAA_Writer(daaFile);
		daaWriter.writeHeader(headerInfo.getDbSeqs(), headerInfo.getDbLetters(), headerInfo.getGapOpen(), headerInfo.getGapExtend(),
				headerInfo.getK(), headerInfo.getLambda());

		// writing hits into daa file
		System.out.println("STEP 3 - Writing into daa-file: " + daaFile.getAbsolutePath());
		maxProgress = (int) getTotalQueryRecords(daaReader);
		ArrayList<Thread> batchReaders = new ArrayList<Thread>();
		for (DAA_Reader reader : daaReader)
			batchReaders.add(new BatchReader(reader, subjectInfos));
		ArrayList<Hit> hits = new ArrayList<Hit>();
		long hitCounter = 0;
		for (int i = 0; i < readInfos.size(); i++) {

			Object[] readInfo = readInfos.get(i);

			// reading-out hits in parallel
			for (Thread reader : batchReaders)
				((BatchReader) reader).setReadName(readInfo);
			runInParallel(batchReaders);

			// storing hits
			ArrayList<MAF_Hit> allHits = new ArrayList<MAF_Hit>();
			for (Thread reader : batchReaders)
				allHits.addAll(((BatchReader) reader).getHits());

			// filtering hits
			ArrayList<Hit> batchHits = new ArrayList<Hit>();
			if (doFiltering) {
				for (MAF_Hit mafHit : Hit_Filter.run(allHits, headerInfo.getLambda(), headerInfo.getK()))
					batchHits.add(new Hit(mafHit));
			} else {
				for (MAF_Hit mafHit : allHits)
					batchHits.add(new Hit(mafHit));
			}
			hits.addAll(filterForUniqueHits(batchHits));

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

	private ArrayList<Hit> filterForUniqueHits(ArrayList<Hit> batchHits) {
		ArrayList<Hit> uniqueHits = new ArrayList<Hit>();
		for (int i = 0; i < batchHits.size(); i++) {
			boolean isUnique = true;
			for (int j = i + 1; j < batchHits.size(); j++) {
				if (batchHits.get(i).equals(batchHits.get(j))) {
					isUnique = false;
					break;
				}
			}
			if (isUnique)
				uniqueHits.add(batchHits.get(i));
		}
		return uniqueHits;
	}

	private int getTotalSeqUsed(ArrayList<DAA_Reader> daaReader) {
		int sum = 0;
		for (DAA_Reader reader : daaReader)
			sum += reader.getDAAHeader().getDbSeqsUsed();
		return sum;
	}

	private int getTotalQueryRecords(ArrayList<DAA_Reader> daaReader) {
		int sum = 0;
		for (DAA_Reader reader : daaReader)
			sum += reader.getDAAHeader().getNumberOfQueryRecords();
		return sum;
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
		private DAA_Reader daaReader;
		private Object[] readInfo;
		private ArrayList<MAF_Hit> hits;
		private int lastIndex = 0;
		private Long filePointer = null;

		private int index = 0;

		public BatchReader(DAA_Reader daaReader, ArrayList<Object[]> subjectInfo) {
			this.daaReader = daaReader;
			this.subjectInfo = subjectInfo;
		}

		public void run() {

			hits = new ArrayList<MAF_Hit>();
			Object[] result = daaReader.parseDAAHitByIndex(index, lastIndex, filePointer);
			ArrayList<DAA_Hit> daaHits = (ArrayList<DAA_Hit>) result[0];
			lastIndex = (int) result[1];
			filePointer = (long) result[2];
			if (index < daaReader.getDAAHeader().getNumberOfQueryRecords() && daaHits.get(0).getQueryName().equals(readInfo[0].toString())) {

				for (DAA_Hit daaHit : daaHits) {

					int rawScore = daaHit.getRawScore();
					String subjectName = daaHit.getReferenceName();
					int refStart = daaHit.getRefStart();
					int refEnd = refStart + daaHit.getRefLength();
					String queryName = daaHit.getQueryName();
					int queryStart = daaHit.getQueryStart();
					int queryLength = daaHit.getQueryLength();
					int frame = daaHit.getFrame();
					ArrayList<Byte> editOperations = daaHit.getEditByteOperations();
					byte[] dnaSequence = daaHit.getPackedQuerySequence();
					MAF_Hit mafHit = new MAF_Hit(rawScore, subjectName, refStart, refEnd, queryName, queryStart, queryLength, frame, editOperations,
							subjectInfo, dnaSequence, (int) readInfo[2]);
					hits.add(mafHit);

				}
				index++;

				if (index % 100 == 0)
					reportProgress(100);

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

		private DAA_Reader reader;
		private ConcurrentSkipListSet<SubjectEntry> subjectInfo_Set;

		public ProcessThread(DAA_Reader reader, ConcurrentSkipListSet<SubjectEntry> subjectInfo_Set) {
			this.reader = reader;
			this.subjectInfo_Set = subjectInfo_Set;
		}

		public void run() {

			DAA_Header daaHeader = reader.getDAAHeader();
			try {
				for (int i = 0; i < (int) daaHeader.getDbSeqsUsed(); i++) {
					SparseString refName = new SparseString(new String(daaHeader.getReferenceName(i)));
					int refLength = daaHeader.getRefLength(i);
					subjectInfo_Set.add(new SubjectEntry(refName, refLength));

					if (i % 100 == 0)
						reportProgress(100);

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			latch.countDown();

		}

	}

	public ArrayList<Thread> generateProcessThreads(ArrayList<DAA_Reader> daaReader, ConcurrentSkipListSet<SubjectEntry> subjectInfo_Set) {

		ArrayList<Thread> processThreads = new ArrayList<Thread>();
		for (DAA_Reader reader : daaReader)
			processThreads.add(new ProcessThread(reader, subjectInfo_Set));
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
