package startUp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import maf.MAF_StreamConverter;

public class MainMerger {

	public static void main(String[] args) {

		System.out.println("DAA Merger by Benjamin Albrecht");
		System.out.println("Copyright (C) 2017 Benjamin Albrecht. This program comes with ABSOLUTELY NO WARRANTY.");

		File daaFolder = null;
		File queryFile = null;
		File daaFile = null;
		Integer cores = Runtime.getRuntime().availableProcessors();
		boolean verbose = false;

		boolean wrongSetting = false;
		for (int i = 0; i < args.length; i++) {
			String option = args[i];
			switch (option) {
			case "-f":
				daaFolder = new File(args[i + 1]);
				if (!daaFolder.exists()) {
					System.err.print("ERROR: invalid tmp directory " + (args[i + 1]) + " (directory could not be created)");
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

		ArrayList<File> daaFiles = findFiles(daaFolder, ".daa");
		if (!daaFiles.isEmpty())
			new MAF_StreamConverter().run(daaFile, daaFiles, queryFile, cores, verbose, daaFiles.get(0), false);

	}

	private static ArrayList<File> findFiles(File folder, String ending) {
		ArrayList<File> files = new ArrayList<File>();
		File[] listOfFiles = folder.listFiles();
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].getName().endsWith(ending))
				files.add(listOfFiles[i]);

		}
		return files;
	}

	private static void printOptionsAndQuit() {
		System.out.println("Mandatory Options: ");
		System.out.println("-f\t" + "path to folder containing all DAA Files");
		System.out.println("-q\t" + "path to query-file in FASTA or FASTQ format (can also be gzipped)");
		System.out.println("-d\t" + "name of the resulting merged DAA File");
		System.out.println("Optional: ");
		System.out.println("-p\t" + "number of available processors (default: maximal number)");
		System.out.println("-v\t" + "sets verbose mode reporting numbers of reads/references/alignments being analyzed)");
		System.out.println("--no-filter\t" + "disable filtering of dominated alignments (default: filtering activated))");
		System.exit(0);
	}

}
