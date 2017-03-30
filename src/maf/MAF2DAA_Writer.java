package maf;

import java.io.File;
import java.util.ArrayList;

import daa.writer.DAA_Writer;
import hits.Hit;

public class MAF2DAA_Writer {

	public static void main(String[] args) {

		// File mafFile = new File("/Users/Benjamin/Documents/Uni_Tuebingen/DFG_Project/DIAOMOND_Paper/real_ONT_Data/EColi_R73_20x/lastOut.maf");
		// File queryFile = new
		// File("/Users/Benjamin/Documents/Uni_Tuebingen/DFG_Project/DIAOMOND_Paper/real_ONT_Data/EColi_R73_20x/combinedReads.fa");

		File mafFile = new File("/Users/Benjamin/Documents/Uni_Tuebingen/DFG_Project/DIAOMOND_Paper/real_ONT_Data/ERR1254532/lastOut.maf");
		File queryFile = new File("/Users/Benjamin/Documents/Uni_Tuebingen/DFG_Project/DIAOMOND_Paper/real_ONT_Data/ERR1254532/ERR1254532.fasta");

		Object[] result = MAF_Reader.run(mafFile, queryFile);

		File daaFile = new File("/Users/Benjamin/Documents/Uni_Tuebingen/DFG_Project/DIAOMOND_Paper/real_ONT_Data/ERR1254532/lastOut.daa");
		DAA_Writer daaWriter = new DAA_Writer(daaFile);
		MAF_Header header = (MAF_Header) result[0];
		daaWriter.writeHeader(header.getDbSeqs(), header.getDbLetters(), header.getGapOpen(), header.getGapExtend(), header.getK(),
				header.getLambda());
		daaWriter.writeHits((ArrayList<Hit>) result[1], (ArrayList<Object[]>) result[2]);
		daaWriter.writeEnd((ArrayList<Object[]>) result[3]);

	}

}
