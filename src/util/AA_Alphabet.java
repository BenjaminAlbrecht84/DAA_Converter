package util;

public class AA_Alphabet {

	private final static String aaString = "ARNDCQEGHILKMFPSTWYVBJZX*/\\";

	public static String getAaString() {
		return aaString;
	}

	public static char getCharacter(int i) {
		return aaString.charAt(i);
	}

	public static boolean contains(char c) {
		for (int i = 0; i < aaString.length(); i++) {
			if (aaString.charAt(i) == c)
				return true;
		}
		return false;
	}

}
