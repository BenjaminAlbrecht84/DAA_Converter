package startUp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;

import maf.MAF_Converter;

public class Main {

	public static void main(String[] args) {

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
			case "-v":
				verbose = true;
				break;
			}

		}

		if (mafFile == null)
			mafFile = readFromInputStream();

		if (mafFile == null || queryFile == null) {
			System.out.println("Mandatory Options: ");
			System.out.println("-m\t" + "path to MAF-File (can be also be piped-in, no gzip allowed)");
			System.out.println("-q\t" + "path to query-file in FASTA or FASTQ format (can also be gzipped)");
			System.out.println("Optional: ");
			System.out.println("-p\t" + "number of available processors (default: maximal number)");
			System.out.println("-d\t" + "path of the reported DAA-file (default: source folder with name of the MAF-file)");
			System.out.println("-v\t" + "sets verbose mode for reporting numbers of reads/references/alignments analyzed)");
			System.exit(0);
		}

		String[] split = mafFile.getAbsolutePath().split("\\.");
		String daaFilePath = "";
		for (int i = 0; i < split.length - 1; i++)
			daaFilePath = daaFilePath.concat(split[i]);
		daaFile = daaFile != null ? daaFile : new File(daaFilePath + ".daa");

		new MAF_Converter().run(daaFile, mafFile, queryFile, cores, verbose);

	}

	private static File readFromInputStream() {

		File mafFile = new File("./lastOutput.maf");
		StringBuilder build = new StringBuilder();
		BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
		boolean firstWrite = true;
		try {
			try {
				int lineCounter = 0;
				String l;
				while ((l = buf.readLine()) != null) {
					build.append(l + "\n");
					if (++lineCounter > 1000) {
						writeFile(build.toString(), mafFile, !firstWrite);
						build = new StringBuilder();
						firstWrite = false;
					}
				}

			} finally {
				buf.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (build.length() > 0)
			writeFile(build.toString(), mafFile, !firstWrite);

		return mafFile;

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
