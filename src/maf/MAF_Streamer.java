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
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import startUp.MainConverter;

public class MAF_Streamer {

	private File queryFile, tmpFolder;
	private int cores;
	private boolean verbose, doFiltering;
	private int chunkSize = 500000000;

	private CountDownLatch countDownLatch = new CountDownLatch(0);
	private ExecutorService executor;

	public MAF_Streamer(File queryFile, File tmpFolder, Integer chunkSize, int cores, boolean doFiltering, boolean verbose) {
		this.queryFile = queryFile;
		this.tmpFolder = tmpFolder;
		this.cores = cores;
		this.verbose = verbose;
		this.doFiltering = doFiltering;
		this.executor = Executors.newFixedThreadPool(1);
		this.chunkSize = chunkSize != null ? chunkSize : this.chunkSize;
	}

	public Object[] processInputStream() {

		ArrayList<Thread> converterThreads = new ArrayList<Thread>();
		File headerFile = null;

		try {

			tmpFolder.mkdir();
			StringBuilder headerBuilder = new StringBuilder();
			StringBuilder batchBuilder = new StringBuilder();
			BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
			boolean firstWrite = true;

			File batchFile = null;

			try {
				int lineCounter = 0;
				int batchCounter = 0;
				String l;
				while ((l = buf.readLine()) != null) {
					if (headerBuilder != null && l.startsWith("#") && !l.startsWith("# batch")) {
						headerBuilder.append(l + "\n");
					} else if (!l.isEmpty()) {
						if (!l.startsWith("# batch")) {

							// writing header file
							if (headerBuilder != null) {
								headerFile = new File(tmpFolder.getAbsolutePath() + File.separatorChar + "header.maf");
								writeFile(headerBuilder.toString(), headerFile, false);
								headerBuilder = null;
							}
							batchBuilder.append(l + "\n");

						}

						if (++lineCounter > 1000 || l.startsWith("# batch")) {
							if (batchFile != null && lineCounter > 1000) {
								writeFile(batchBuilder.toString(), batchFile, !firstWrite);
								batchBuilder = new StringBuilder();
								firstWrite = false;
								lineCounter = 0;
							}
							if (l.startsWith("# batch") && batchFile == null) {
								firstWrite = true;
								batchFile = createTmpFile(batchCounter++);
								batchBuilder = new StringBuilder();
								batchBuilder.append("# batch " + batchCounter + "\n");
								lineCounter = 0;

							}
						}
					} else if (batchFile != null && batchFile.length() > chunkSize) {
						if (batchFile != null) {
							Thread daaThread = writeDAAFile(batchFile, queryFile, headerFile, cores, verbose);
							converterThreads.add(daaThread);
							executor.submit(daaThread);
							countDownLatch = new CountDownLatch((int) countDownLatch.getCount() + 1);
						}

						firstWrite = true;
						batchFile = createTmpFile(batchCounter++);
						batchBuilder = new StringBuilder();
						batchBuilder.append("# batch " + batchCounter + "\n");
						lineCounter = 0;
					}

				}
				if (batchBuilder.length() > 0) {

					writeFile(batchBuilder.toString(), batchFile, !firstWrite);

					Thread daaThread = writeDAAFile(batchFile, queryFile, headerFile, cores, verbose);
					converterThreads.add(daaThread);
					executor.submit(daaThread);
					countDownLatch = new CountDownLatch((int) countDownLatch.getCount() + 1);
				}

			} finally {
				buf.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		// waiting for termination
		try {
			countDownLatch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor.shutdown();

		// collecting daa files
		ArrayList<File> daaFiles = new ArrayList<File>();
		for (Thread daaThreads : converterThreads)
			daaFiles.add(((ConverterThread) daaThreads).getDaaFile());

		if (daaFiles.isEmpty())
			return null;

		Object[] result = { headerFile, daaFiles };
		return result;

	}

	private File createTmpFile(int batchCounter) {
		File f = new File(tmpFolder.getAbsolutePath() + File.separatorChar + "z" + System.currentTimeMillis() + "-" + batchCounter + ".maf");
		while (f.exists())
			f = new File(tmpFolder.getAbsolutePath() + File.separatorChar + "z" + System.currentTimeMillis() + "-" + batchCounter + ".maf");
		return f;
	}

	private Thread writeDAAFile(File batchFile, File queryFile, File headerFile, int cores, boolean verbose) {
		File daaFile = new File(batchFile.getAbsolutePath().replace(".maf", ".daa"));
		return new ConverterThread(daaFile, batchFile, queryFile, headerFile, 1, verbose);
	}

	public class ConverterThread extends Thread {

		private File batchFile, daaFile, queryFile, headerFile;
		private int cores;
		private boolean verbose;

		public ConverterThread(File daaFile, File batchFile, File queryFile, File headerFile, int cores, boolean verbose) {
			this.batchFile = batchFile;
			this.daaFile = daaFile;
			this.queryFile = queryFile;
			this.headerFile = headerFile;
			this.cores = cores;
			this.verbose = verbose;
		}

		@Override
		public void run() {
			new MAF_Converter().run(daaFile, batchFile, queryFile, cores, verbose, headerFile, doFiltering);
			batchFile.delete();
			countDownLatch.countDown();
		}

		public File getDaaFile() {
			return daaFile;
		}

	}

	private static void writeFile(String output, File file, boolean append) {
		try {
			FileWriter writer = new FileWriter(file, append);
			writer.write(output);
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
