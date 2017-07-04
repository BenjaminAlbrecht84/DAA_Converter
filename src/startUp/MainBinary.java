package startUp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;

import maf.MAF_Converter;
import maf.MAF_StreamConverter;
import maf.MAF_Streamer;

public class MainBinary {

	public static void main(String[] args) {

		System.out.println("MAF2DAA Converter by Benjamin Albrecht");
		System.out.println("Copyright (C) 2017 Benjamin Albrecht. This program comes with ABSOLUTELY NO WARRANTY.");

		File mafFile = null;
		File queryFile = null;
		File daaFile = null;
		File tmpFolder = null;
		Integer cores = Runtime.getRuntime().availableProcessors(), cores_streaming = 1;
		boolean verbose = false;

		boolean wrongSetting = false;
		for (int i = 0; i < args.length; i++) {
			String option = args[i];
			switch (option) {
			case "-m":
				mafFile = new File(args[i + 1]);
				if (!mafFile.isFile()) {
					System.err.print("ERROR: invalid MAF-file " + (args[i + 1]) + " (file does not exist)");
					wrongSetting = true;
				}
				i++;
				break;
			case "-ps":
				try {
					cores_streaming = Integer.parseInt(args[i + 1]);
				} catch (Exception e) {
					System.err.print("ERROR: not an integer " + (args[i + 1]));
					wrongSetting = true;
				}
				i++;
				break;
			case "-p":
				try {
					cores = Integer.parseInt(args[i + 1]);
				} catch (Exception e) {
					System.err.print("ERROR: not an integer " + (args[i + 1]));
					wrongSetting = true;
				}
				i++;
				break;
			case "-q":
				queryFile = new File(args[i + 1]);
				if (!queryFile.isFile()) {
					System.err.print("ERROR: invalid query-file " + (args[i + 1]) + " (file does not exist)");
					wrongSetting = true;
				}
				i++;
				break;
			case "-d":
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
				tmpFolder = new File(args[i + 1]);
				if (tmpFolder.exists())
					deleteDir(tmpFolder);
				if (!tmpFolder.mkdir()) {
					System.err.print("ERROR: invalid tmp directory " + (args[i + 1]) + " (directory could not be created)");
					wrongSetting = true;
				}
				i++;
				break;
			case "-v":
				verbose = true;
				break;
			default:
				System.err.println("ERROR: unknown paramter " + option);
				wrongSetting = true;
			}

		}

		if (daaFile == null || queryFile == null || wrongSetting)
			printOptionsAndQuit();

		Object[] streamResults = null;
		if (mafFile == null)
			streamResults = new MAF_Streamer(queryFile, tmpFolder, cores_streaming, verbose).processInputStream();

		if (mafFile == null && streamResults == null)
			printOptionsAndQuit();

		if (streamResults != null) {
			new MAF_StreamConverter().run(daaFile, (ArrayList<File>) streamResults[1], queryFile, cores, verbose, (File) streamResults[0]);
			deleteDir((File) streamResults[2]);
		}
		if (mafFile != null)
			new MAF_Converter().run(daaFile, mafFile, queryFile, cores, verbose, null);

	}

	private static void printOptionsAndQuit() {
		System.out.println("Mandatory Options: ");
		System.out.println("-m\t" + "path to MAF-File (can also be piped in, no gzip allowed here)");
		System.out.println("-q\t" + "path to query-file in FASTA or FASTQ format (can also be gzipped)");
		System.out.println("-d\t" + "name of the resulting DAA-File");
		System.out.println("Optional: ");
		System.out.println("-p\t" + "number of available processors (default: maximal number)");
		System.out.println("-ps\t" + "number of available processors while input is piped-in (default: 1)");
		System.out.println("-t\t" + "folder for temporary files (default: system's tmp folder)");
		System.out.println("-v\t" + "sets verbose mode reporting numbers of reads/references/alignments being analyzed)");
		System.exit(0);
	}

	private static boolean deleteDir(File dir) {
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory())
					deleteDir(files[i]);
				 else 
					files[i].delete();				
			}
		}
		return dir.delete();
	}

}
