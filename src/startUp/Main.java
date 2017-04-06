package startUp;

import java.io.File;

import maf.MAF_Converter;

public class Main {

	public static void main(String[] args) {

		File mafFile = null;
		File queryFile = null;
		File daaFile = null;
		int cores = Runtime.getRuntime().availableProcessors();
		for (int i = 0; i < args.length; i++) {
			String option = args[i];
			switch (option) {
			case "-m":
				try {
					mafFile = new File(args[i + 1]);
				} catch (Exception e) {
					System.err.print("ERROR: not an file " + (args[i + 1]));
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
					System.err.print("ERROR: not an file " + (args[i + 1]));
				}
				i++;
				break;
			case "-d":
				try {
					daaFile = new File(args[i + 1]);
				} catch (Exception e) {
					System.err.print("ERROR: not an file " + (args[i + 1]));
				}
				i++;
				break;
			}
		}

		if (mafFile == null || queryFile == null) {
			System.out.println("Mandatory Options: ");
			System.out.println("-m\t" + "path to MAF-File");
			System.out.println("-q\t" + "path to query-file (FASTA/FASTQ)");
			System.out.println("Optional: ");
			System.out.println("-p\t" + "number of available processors");
			System.out.println("-d\t" + "path to daa-file");
			System.exit(0);
		}
		String[] split = mafFile.getAbsolutePath().split("\\.");
		String daaFilePath = "";
		for (int i = 0; i < split.length - 1; i++)
			daaFilePath = daaFilePath.concat(split[i]);
		daaFile = daaFile != null ? daaFile : new File(daaFilePath + ".daa");

		new MAF_Converter().run(daaFile, mafFile, queryFile, cores);

	}

}
