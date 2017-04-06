package daa.reader;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import hits.Hit;
import hits.ReadHits;
import util.SparseString;

public class DAA_Reader {

	private File daaFile;
	private boolean verbose = false;

	private DAA_Header header;
	private CountDownLatch latch;
	private ConcurrentHashMap<String, ReadHits> readMap;

	private AtomicLong allParsedRecords;;
	private int last_p;
	private long numOfRecords;

	public DAA_Reader(File daaFile, boolean verbose) {
		this.verbose = verbose;
		this.daaFile = daaFile;
		header = new DAA_Header(daaFile);
		header.loadAllReferences();

		if (verbose)
			header.print();

	}

	public ConcurrentHashMap<String, ReadHits> parseAllHits(int cores) {

		System.out.println("STEP_3>Parsing DIAMOND output...");
		long time = System.currentTimeMillis();

		readMap = new ConcurrentHashMap<String, ReadHits>();

		System.out.println("OUTPUT>Parsing " + header.getNumberOfQueryRecords() + " query records...");

		last_p = 0;
		allParsedRecords = new AtomicLong(0);
		numOfRecords = header.getNumberOfQueryRecords();

		int chunk = (int) Math.ceil((double) header.getNumberOfQueryRecords() / (double) cores);
		Vector<Thread> allParser = new Vector<Thread>();
		for (int l = 0; l < header.getNumberOfQueryRecords(); l += chunk) {
			int r = (l + chunk) < header.getNumberOfQueryRecords() ? l + chunk : (int) header.getNumberOfQueryRecords();
			int[] bounds = { l, r };
			DAA_Parser parser = new DAA_Parser(bounds);
			allParser.add(parser);
		}

		latch = new CountDownLatch(allParser.size());
		ExecutorService executor = Executors.newFixedThreadPool(cores);
		for (Thread thread : allParser)
			executor.execute(thread);

		// awaiting termination
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		executor.shutdown();

		long runtime = (System.currentTimeMillis() - time) / 1000;
		System.out.println("OUTPUT>" + 100 + "% (" + numOfRecords + "/" + numOfRecords + ") of all records parsed.[" + runtime + "s]\n");

		return readMap;

	}

	private synchronized void reportProgress(int p) {
		p = ((int) Math.floor((double) p / 10.)) * 10;
		if (p != 100 && p != last_p && p % 1 == 0) {
			System.out.println("OUTPUT>" + p + "% (" + allParsedRecords + "/" + numOfRecords + ") of all records parsed.");
			last_p = p;
		}
	}

	private synchronized void addHits(HashMap<String, ReadHits> localReadMap) {
		for (String read_id : localReadMap.keySet()) {
			if (!readMap.containsKey(read_id))
				readMap.put(read_id, new ReadHits());
			ReadHits hits = localReadMap.get(read_id);
			for (SparseString gi : hits.getHitMap().keySet()) {
				for (int frame : hits.getHitMap().get(gi).keySet()) {
					for (Hit h : hits.getHitMap().get(gi).get(frame))
						readMap.get(read_id).add(h, gi, frame);
				}
			}
		}
	}

	public class DAA_Parser extends Thread {

		private int[] bounds;

		public DAA_Parser(int[] queryRecordsBounds) {
			this.bounds = queryRecordsBounds;
		}

		public void run() {

			HashMap<String, ReadHits> localReadMap = new HashMap<String, ReadHits>();

			try {

				RandomAccessFile raf = new RandomAccessFile(daaFile, "r");

				try {

					raf.seek(header.getLocationOfBlockInFile(header.getAlignmentsBlockIndex()));
					for (int i = 0; i < header.getNumberOfQueryRecords(); i++) {

						DAA_Hit hit = new DAA_Hit();

						long filePointer = raf.getFilePointer();
						ByteBuffer buffer = ByteBuffer.allocate(4);
						buffer.order(ByteOrder.LITTLE_ENDIAN);
						raf.read(buffer.array());
						int alloc = buffer.getInt();

						ByteBuffer hitBuffer = ByteBuffer.allocate(alloc);
						hitBuffer.order(ByteOrder.LITTLE_ENDIAN);
						raf.read(hitBuffer.array());

						if (i >= bounds[0] && i < bounds[1]) {

							// parsing query properties
							hit.parseQueryProperties(filePointer, hitBuffer, false);

							while (hitBuffer.position() < hitBuffer.capacity()) {

								int accessPoint = hitBuffer.position();

								// parsing match properties
								hit.parseHitProperties(header, hitBuffer, false);

								int ref_start = hit.getRefStart() + 1;
								int ref_end = ref_start - 1 + hit.getRefLength();
								int bitScore = hit.getBitScore();
								int rawScore = hit.getRawScore();
								long pointer = hit.getFilePointer();
								int query_start = hit.getQueryStart() + 1;
								int ref_length = hit.getTotalRefLength();
								int query_length = hit.getQueryLength() / 3;

								int frame = hit.getFrame();
								SparseString gi = new SparseString(hit.getReferenceName().split(" ")[0]);
								int subjectID = hit.getSubjectID();

								// parsing query name
								String queryName = hit.getQueryName();
								String[] id_split = mySplit(queryName, ':');
								String read_id = id_split[0];

								// initializing hit
								Hit h = new Hit(ref_start, ref_end, bitScore, rawScore, pointer, accessPoint, query_start, ref_length,
										query_length, subjectID);
								h.setFrame(frame);

								// storing hit
								if (!localReadMap.containsKey(read_id))
									localReadMap.put(read_id, new ReadHits());
								localReadMap.get(read_id).add(h, gi, frame);

							}

							if (i != 0 && i % 1000 == 0) {
								int p = (int) Math.round(((double) allParsedRecords.addAndGet(1000) / (double) numOfRecords) * 100.);
								reportProgress(p);
							}

						}

						if (i >= bounds[1])
							break;

					}

				} finally {
					raf.close();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			addHits(localReadMap);
			latch.countDown();

		}

		private String[] mySplit(String s, char c) {
			List<String> words = new ArrayList<String>();
			int pos = 0, end;
			while ((end = s.indexOf(c, pos)) >= 0) {
				words.add(s.substring(pos, end));
				pos = end + 1;
			}
			if (pos < s.length())
				words.add(s.substring(pos, s.length()));
			String[] entries = words.toArray(new String[words.size()]);
			return entries;

		}

	}

	public DAA_Hit parseHit(RandomAccessFile raf, long filePointer, int accessPoint) {

		DAA_Hit hit = null;

		try {

			raf.seek(filePointer);

			ByteBuffer buffer = ByteBuffer.allocate(4);
			buffer.order(ByteOrder.LITTLE_ENDIAN);
			raf.read(buffer.array());
			int alloc = buffer.getInt();

			ByteBuffer hitBuffer = ByteBuffer.allocate(alloc);
			hitBuffer.order(ByteOrder.LITTLE_ENDIAN);
			raf.read(hitBuffer.array());

			// parsing query properties
			hit = new DAA_Hit();
			hit.parseQueryProperties(filePointer, hitBuffer, true);

			hitBuffer.position(accessPoint);

			// parsing match properties
			hit.parseHitProperties(header, hitBuffer, true);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return hit;

	}

	public File getDAAFile() {
		return daaFile;
	}

	public DAA_Header getDAAHeader() {
		return header;
	}

}
