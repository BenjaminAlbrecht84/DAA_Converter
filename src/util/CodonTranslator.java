package util;

import java.util.concurrent.ConcurrentHashMap;

public class CodonTranslator {

	private ConcurrentHashMap<String, Character> codonMap;

	public CodonTranslator() {
		codonMap = new ConcurrentHashMap<String, Character>();

		codonMap.put("GGU", 'G');
		codonMap.put("GGC", 'G');
		codonMap.put("GGA", 'G');
		codonMap.put("GGG", 'G');

		codonMap.put("GCU", 'A');
		codonMap.put("GCC", 'A');
		codonMap.put("GCA", 'A');
		codonMap.put("GCG", 'A');

		codonMap.put("GUU", 'V');
		codonMap.put("GUC", 'V');
		codonMap.put("GUA", 'V');
		codonMap.put("GUG", 'V');

		codonMap.put("UUA", 'L');
		codonMap.put("UUG", 'L');
		codonMap.put("CUU", 'L');
		codonMap.put("CUC", 'L');
		codonMap.put("CUA", 'L');
		codonMap.put("CUG", 'L');

		codonMap.put("AUU", 'I');
		codonMap.put("AUC", 'I');
		codonMap.put("AUA", 'I');

		codonMap.put("UCU", 'S');
		codonMap.put("UCC", 'S');
		codonMap.put("UCA", 'S');
		codonMap.put("UCG", 'S');
		codonMap.put("AGU", 'S');
		codonMap.put("AGC", 'S');

		codonMap.put("ACU", 'T');
		codonMap.put("ACC", 'T');
		codonMap.put("ACA", 'T');
		codonMap.put("ACG", 'T');

		codonMap.put("GAU", 'D');
		codonMap.put("GAC", 'D');

		codonMap.put("GAA", 'E');
		codonMap.put("GAG", 'E');

		codonMap.put("AAU", 'N');
		codonMap.put("AAC", 'N');

		codonMap.put("CAA", 'Q');
		codonMap.put("CAG", 'Q');

		codonMap.put("AAG", 'K');
		codonMap.put("AAA", 'K');

		codonMap.put("CGU", 'R');
		codonMap.put("CGC", 'R');
		codonMap.put("CGA", 'R');
		codonMap.put("CGG", 'R');
		codonMap.put("AGA", 'R');
		codonMap.put("AGG", 'R');

		codonMap.put("CAU", 'H');
		codonMap.put("CAC", 'H');

		codonMap.put("UUU", 'F');
		codonMap.put("UUC", 'F');

		codonMap.put("UGU", 'C');
		codonMap.put("UGC", 'C');

		codonMap.put("UGG", 'W');

		codonMap.put("UAU", 'Y');
		codonMap.put("UAC", 'Y');

		codonMap.put("AUG", 'M');

		codonMap.put("CCU", 'P');
		codonMap.put("CCC", 'P');
		codonMap.put("CCA", 'P');
		codonMap.put("CCG", 'P');
	}

	public String translate(String dna) {
		String rna = dna.replaceAll("T", "U");
		StringBuffer prot = new StringBuffer("");
		String codon = "";
		for (int i = 0; i < rna.length(); i++) {
			codon = codon.concat("" + rna.charAt(i));
			if (codon.length() == 3) {
				char p = translateCodon(codon);
				prot.append(p);
				codon = "";
			}
		}
		return prot.toString();
	}

	public char translateCodon(String codon) {
		codon = codon.replaceAll("T", "U");
		if (codonMap.containsKey(codon))
			return codonMap.get(codon);
		return 'X';
	}

}
