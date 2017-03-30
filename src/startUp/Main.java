package startUp;

import java.io.File;
import java.util.ArrayList;

import daa.writer.DAA_Writer;
import hits.Hit;
import maf.MAF_Header;
import maf.MAF_Reader;

public class Main {

	public static void main(String[] args) {

		File mafFile = null;
		File queryFile = null;
		File daaFile = null;
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
			System.out.println("optional: ");
			System.out.println("-d\t" + "path to daa-file");
			System.exit(0);
		}
		String[] split = mafFile.getAbsolutePath().split("\\.");
		String daaFilePath = "";
		for (int i = 0; i < split.length - 1; i++)
			daaFilePath = daaFilePath.concat(split[i]);
		daaFile = daaFile != null ? daaFile : new File(daaFilePath + ".daa");

		Object[] result = MAF_Reader.run(mafFile, queryFile);

		DAA_Writer daaWriter = new DAA_Writer(daaFile);
		MAF_Header header = (MAF_Header) result[0];
		daaWriter.writeHeader(header.getDbSeqs(), header.getDbLetters(), header.getGapOpen(), header.getGapExtend(), header.getK(),
				header.getLambda());
		daaWriter.writeHits((ArrayList<Hit>) result[1], (ArrayList<Object[]>) result[2]);
		daaWriter.writeEnd((ArrayList<Object[]>) result[3]);

	}

}
