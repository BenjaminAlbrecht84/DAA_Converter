package util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class FileParallelLineStreamer {

	private CountDownLatch latch;

	private long startingTime;
	private int delta = 100;
	private AtomicInteger progress;
	private int maxProgress, lastProgress;
	private ExecutorService executor;

	public void run(File file, int cores, boolean reportProgress) {
		parseFile(file, null, cores, reportProgress);
	}

	public void run(File file, int numOfLines, int cores, boolean reportProgress) {
		parseFile(file, numOfLines, cores, reportProgress);
	}

	private void parseFile(File file, Integer numOfLines, int cores, boolean reportProgress) {

		startingTime = System.currentTimeMillis();

		numOfLines = numOfLines == null ? LineCounter.run(file) : numOfLines;
		long chunk = (long) Math.ceil((double) numOfLines / (double) cores);

		System.out.println(numOfLines + " " + chunk + " " + cores);

		lastProgress = 0;
		progress = new AtomicInteger(0);
		maxProgress = numOfLines;

		executor = Executors.newFixedThreadPool(cores);
		ArrayList<Thread> lineParser = generateLineParser(file, chunk);
		runInParallel(lineParser, cores);

		if (reportProgress)
			reportFinish();

	}

	public class LineParser extends Thread {

		private long filePointer, chunk;
		private File file;
		private int id;

		public LineParser(File file, long filePointer, long chunk, int id) {
			this.file = file;
			this.filePointer = filePointer;
			this.chunk = chunk;
			this.id = id;
		}

		@Override
		public void run() {

			try {

				RandomAccessFile raf = new RandomAccessFile(file, "r");

				try {

					raf.seek(filePointer);

					byte[] buffer = new byte[1024 * 1024];
					int readChars = 0, parsedLines = 0;
					boolean chunkCompleted = false, stop = false;
					StringBuffer buf = new StringBuffer();
					while ((readChars = raf.read(buffer)) != -1 && !stop) {
						for (int i = 0; i < readChars; i++) {

							char c = (char) buffer[i];
							if (chunkCompleted && c == '>') {
								stop = true;
								break;
							}

							switch (c) {
							case '\n':

								parsedLines++;
								if (parsedLines > chunk) {
									chunkCompleted = true;
								}

								String line = buf.toString();
								processLine(line, id, false);
								buf = new StringBuffer();

								if (parsedLines % delta == 0)
									reportProgress(delta);

								break;
							default:
								buf = buf.append(c);
							}
						}
					}
					if (readChars == -1) {
						String line = buf.toString();
						processLine(line, id, true);
					}

				} finally {
					raf.close();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			latch.countDown();

		}

	}

	public abstract void processLine(String line, int threadID, boolean lastLine);

	private ArrayList<Thread> generateLineParser(File file, long chunk) {

		ArrayList<Thread> processThreads = new ArrayList<Thread>();
		try {
			InputStream is = new BufferedInputStream(new FileInputStream(file));
			try {
				byte[] c = new byte[1024];
				int lineCounter = 0, threadCounter = 0;
				int readChars = 0;
				long filePointer = 0;
				processThreads.add(new LineParser(file, filePointer, chunk, threadCounter++));
				while ((readChars = is.read(c)) != -1) {
					for (int i = 0; i < readChars; ++i) {
						filePointer++;
						if (c[i] == '\n') {
							lineCounter++;
							if (lineCounter % (chunk + 1) == 0)
								processThreads.add(new LineParser(file, filePointer, chunk, threadCounter++));
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
		long runtime = (System.currentTimeMillis() - startingTime) / 1000;
		System.out.print(100 + "% (" + runtime + "s)\n");
	}

	public void runInParallel(ArrayList<Thread> threads, int cores) {
		latch = new CountDownLatch(threads.size());
		for (Thread t : threads)
			executor.execute(t);
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor.shutdown();
	}

	public void cancel() {
		if (executor != null) {
			executor.shutdownNow();
			while (latch.getCount() > 0)
				latch.countDown();
		}
	}

}
