package startUp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import maf.MAF_Converter;
import maf.MAF_StreamConverter;
import maf.MAF_Streamer;
import util.Finalizer;

public class MainConverter {

	public final static String version = "v0.8.5";
	public static double MIN_PROPORTION_COVERAGE = 0.9;
	public static double MIN_PROPORTION_SCORE = 0.9;

	public static void main(String[] args) {

		System.out.println("MAF2DAA Converter by Benjamin Albrecht");
		System.out.println("Copyright (C) 2017 Benjamin Albrecht. This program comes with ABSOLUTELY NO WARRANTY.");

		Integer chunkSize = null;
		File mafFile = null;
		File queryFile = null;
		File daaFile = null;
		File tmpFolder = null;
		Integer cores = Runtime.getRuntime().availableProcessors(), cores_streaming = 1;
		double topPercent = 10.;
		boolean doFiltering = true;
		boolean verbose = false;

		boolean wrongSetting = false;
		for (int i = 0; i < args.length; i++) {
			String option = args[i];
			switch (option) {
			case "-i":
			case "--in":
				mafFile = new File(args[i + 1]);
				if (!mafFile.isFile()) {
					System.err.println("ERROR: invalid .maf file " + (args[i + 1]) + " (file does not exist)");
					wrongSetting = true;
				}
				i++;
				break;
			case "-ps":
			case "—-streamingProcs":
				try {
					cores_streaming = Integer.parseInt(args[i + 1]);
				} catch (Exception e) {
					System.err.println("ERROR: not an integer " + (args[i + 1]));
					wrongSetting = true;
				}
				i++;
				break;
			case "-cs":
			case "—-chunkSize":
				String s = args[i + 1];
				String num = s.substring(0, s.length() - 1);
				try {
					chunkSize = Integer.parseInt(num);
					int factor = 1000000;
					switch (s.charAt(s.length() - 1)) {
					case 'k':
					case 'K':
						factor = 1000;
						break;
					case 'm':
					case 'M':
						factor = 1000000;
						break;
					case 'g':
					case 'G':
						factor = 1000000000;
						break;
					default:
						System.err.println("Error: not a legal file-size identifier " + s.charAt(s.length() - 1)
								+ " - legal identifiers are: k (for KB), m (for MB), and g (for GB)");
						wrongSetting = true;
					}
					chunkSize *= factor;
				} catch (NumberFormatException e) {
					System.err.println("Error: not an integer " + num + ".");
					wrongSetting = true;
				}
				i++;
				break;
			case "-top":
			case "—-topPercent":
				try {
					topPercent = Double.parseDouble(args[i + 1]);
				} catch (Exception e) {
					System.err.println("ERROR: not a value " + (args[i + 1]));
					wrongSetting = true;
				}
				i++;
				break;
			case "-p":
			case "—-procs":
				try {
					cores = Integer.parseInt(args[i + 1]);
				} catch (Exception e) {
					System.err.println("ERROR: not an integer " + (args[i + 1]));
					wrongSetting = true;
				}
				i++;
				break;
			case "-r":
			case "--reads":
				queryFile = new File(args[i + 1]);
				if (!queryFile.isFile()) {
					System.err.println("ERROR: invalid reads file " + (args[i + 1]) + " (file does not exist)");
					wrongSetting = true;
				}
				i++;
				break;
			case "-o":
			case "--out":
				daaFile = new File(args[i + 1]);
				try {
					if (daaFile.exists())
						daaFile.delete();
					daaFile.createNewFile();
					if (!daaFile.isFile()) {
						wrongSetting = true;
						System.err.println("ERROR: not a proper file path " + daaFile.getAbsolutePath());
					}
				} catch (IOException e) {
					System.err.println("ERROR: cannot create DAA-file " + daaFile.getAbsolutePath());
					wrongSetting = true;
				}
				i++;
				break;
			case "-t":
			case "-tmp":
				tmpFolder = new File(args[i + 1]);
				if (!tmpFolder.exists() && !tmpFolder.mkdir()) {
					System.err.println("ERROR: invalid tmp directory " + (args[i + 1]) + " (directory could not be created)");
					wrongSetting = true;
				}
				i++;
				break;
			case "-v":
			case "--verbose":
				verbose = true;
				break;
			case "-h":
			case "--help":
				wrongSetting = true;
				break;
			default:
				System.err.println("ERROR: unknown paramter " + option);
				wrongSetting = true;
			}

		}

		if (daaFile == null || queryFile == null || wrongSetting)
			printOptionsAndQuit();

		MIN_PROPORTION_COVERAGE = (100. - new Double(topPercent)) / 100.;
		MIN_PROPORTION_SCORE = (100. - new Double(topPercent)) / 100.;

		Object[] streamResults = null;
		if (mafFile == null) {
			tmpFolder = (tmpFolder == null) ? daaFile.getAbsoluteFile().getParentFile() : tmpFolder;
			streamResults = new MAF_Streamer(queryFile, tmpFolder, chunkSize, cores_streaming, doFiltering, verbose).processInputStream();
		}

		if (mafFile == null && streamResults == null)
			printOptionsAndQuit();

		if (streamResults != null) {
			new MAF_StreamConverter().run(daaFile, (ArrayList<File>) streamResults[1], queryFile, cores, verbose, (File) streamResults[0],
					doFiltering);
		}
		if (mafFile != null)
			new MAF_Converter().run(daaFile, mafFile, queryFile, cores, verbose, null, doFiltering);

	}

	private static void printOptionsAndQuit() {
		int space = 25;
		System.out.println("Input");
		System.out.println(String.format("%-"+space+"s %s", "\t-i, --in", "sets path to MAF-File (can also be piped in, no gzip allowed here)"));
		System.out.println(String.format("%-"+space+"s %s", "\t-r, -- reads", "sets path to query-file in FASTA or FASTQ format (can also be gzipped)"));
		System.out.println("Output");
		System.out.println(String.format("%-"+space+"s %s", "\t-o, --out", "sets path of the reported DAA-File"));
		System.out.println("Parameter");
		System.out.println(String.format("%-"+space+"s %s","\t-top, --topPercent", "sets top percent of reads kept during filtering (default: 10.0)"));
		System.out.println(String.format("%-"+space+"s %s", "\t-p, --procs", "sets number of used processors (default: maximal number)"));
		System.out.println(
				String.format("%-"+space+"s %s", "\t-ps, --streamingProcs", "sets number of used processors while input is piped-in (default: 1)"));
		System.out.println(String.format("%-"+space+"s %s", "\t-cs, --chunkSize", "sets chunk-size of temporary MAF files (default: 500mb)"));
		System.out
				.println(String.format("%-"+space+"s %s", "\t-t, --tmp" , "sets folder for temporary files (default: parent folder of the resulting DAA-File)"));
		System.out.println("Other");
		System.out.println(
				String.format("%-"+space+"s %s", "\t-v, --verbose", "sets verbose mode reporting numbers of reads/references/alignments being analyzed)"));
		System.out.println(String.format("%-"+space+"s %s", "\t-h, --help", "shows program usage and quits"));
		System.out.println("AUTHOR");
		System.out.println("\tBenjamin Albrecht");
		System.out.println("VERSION");
		System.out.println("\t"+version);
		System.out.println("Copyright (C) 2017 Benjamin Albrecht. This program comes with ABSOLUTELY NO WARRANTY.");
		System.exit(0);
	}

}
