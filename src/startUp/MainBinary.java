package startUp;

import java.io.File;
import java.util.ArrayList;

import maf.MAF_Converter;
import maf.MAF_StreamConverter;
import maf.MAF_Streamer;

public class MainBinary {

	public static void main(String[] args) {
		
		System.out.println("MAF2DAA converter by Benjamin Albrecht");

		File mafFile = null;
		File queryFile = null;
		File daaFile = null;
		int cores = Runtime.getRuntime().availableProcessors();
		boolean verbose = false;

		for (int i = 0; i < args.length; i++) {
			String option = args[i];
			switch (option) {
			case "-m":
				try {
					mafFile = new File(args[i + 1]);
				} catch (Exception e) {
					System.err.print("ERROR: not a file " + (args[i + 1]));
				}
				i++;
				break;
			case "-p":
				try {
					cores = Integer.parseInt(args[i + 1]);
				} catch (Exception e) {
					System.err.print("ERROR: not an integer " + (args[i + 1]));
				}
				i++;
				break;
			case "-q":
				try {
					queryFile = new File(args[i + 1]);
				} catch (Exception e) {
					System.err.print("ERROR: not a file " + (args[i + 1]));
				}
				i++;
				break;
			case "-d":
				try {
					daaFile = new File(args[i + 1]);
				} catch (Exception e) {
					System.err.print("ERROR: not a file " + (args[i + 1]));
				}
				i++;
				break;
			case "-v":
				verbose = true;
				break;
			}

		}

		Object[] streamResults = null;
		if (mafFile == null){
			System.out.println("No MAF-File specified, now processing input stream...");
			streamResults = new MAF_Streamer(queryFile, cores, verbose).processInputStream();
		}

		if (queryFile == null || (mafFile == null && streamResults == null)) {
			System.out.println("Mandatory Options: ");
			System.out.println("-m\t" + "path to MAF-File (can be also be piped-in, no gzip allowed)");
			System.out.println("-q\t" + "path to query-file in FASTA or FASTQ format (can also be gzipped)");
			System.out.println("Optional: ");
			System.out.println("-p\t" + "number of available processors (default: maximal number)");
			System.out.println("-d\t" + "path of the reported DAA-file (default: same name and folder as the query-file)");
			System.out.println("-v\t" + "sets verbose mode reporting numbers of reads/references/alignments being analyzed)");
			System.exit(0);
		}

		String[] split = queryFile.getAbsolutePath().split("\\.");
		String daaFilePath = "";
		for (int i = 0; i < split.length - 1; i++)
			daaFilePath = daaFilePath.concat(split[i]);
		daaFile = daaFile != null ? daaFile : new File(daaFilePath + ".daa");

		if (streamResults != null) {
			new MAF_StreamConverter().run(daaFile, (ArrayList<File>) streamResults[1], queryFile, cores, verbose, (File) streamResults[0]);
			deleteDir((File) streamResults[2]);
		}
		if (mafFile != null)
			new MAF_Converter().run(daaFile, mafFile, queryFile, cores, verbose, null);

	}

	private static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}

}
