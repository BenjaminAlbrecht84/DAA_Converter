/*
 * Copyright 2017 Benjamin Albrecht
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

public class DAACompressAlignment {

	private static final HashMap<Character, Integer> aaToIndex = new HashMap<Character, Integer>();
	static {
		new AA_Alphabet();
		// String aaString = "ARNDCQEGHILKMFPSTWYVBJZX*";
		String aaString = AA_Alphabet.getAaString();
		for (int i = 0; i < aaString.length(); i++)
			aaToIndex.put(aaString.charAt(i), i);
	}

	public static ArrayList<Byte> run(String[] ali) {

		ArrayList<Byte> editOps = new ArrayList<Byte>();
		char lastType = '-';
		int num = 0;
		for (int i = 0; i < ali[0].length(); i++) {

			char c1 = ali[0].charAt(i);
			char c2 = ali[1].charAt(i);
			char type = getEditType(c1, c2);

			if (type == 'M' || type == 'I') {
				if (type != lastType && num != 0) {
					editOps.addAll(getEditOperation(lastType, num));
					num = 0;
				}
				num++;
			} else {
				if (num != 0 && lastType != '-') {
					editOps.addAll(getEditOperation(lastType, num));
					num = 0;
				}
				if (c1 == '/' || c1 == '\\')
					editOps.addAll(getEditOperation(type, c1));
				else
					editOps.addAll(getEditOperation(type, c2));
			}
			lastType = type;

		}
		if (lastType == 'M' || lastType == 'I')
			editOps.addAll(getEditOperation(lastType, num));

		return editOps;
	}

	private static Vector<Byte> getEditOperation(char type, int total) {

		Vector<Byte> opVec = new Vector<Byte>();
		while (total > 0) {
			int num = total > 63 ? 63 : total;
			total -= num;
			byte op = 0;
			if (type == 'I')
				op |= 1 << 6;
			op |= num;
			opVec.add(op);
		}
		return opVec;
	}

	private static Vector<Byte> getEditOperation(char type, char c) {

		byte op = 0;
		if (type == 'D')
			op |= 2 << 6;
		else
			op |= 3 << 6;

		if (!aaToIndex.containsKey(c))
			c = '*';
		op |= aaToIndex.get(c);

		Vector<Byte> opVec = new Vector<Byte>();
		opVec.add(op);
		return opVec;
	}

	private static char getEditType(char c1, char c2) {

		// filtering out unknown symbols (like slashes in last-alignments)
		if (c1 == '/')
			return 'S';
		if (c1 == '\\')
			return 'S';
		if (c1 == '-')
			return 'D';
		if (c2 == '-')
			return 'I';
		if (c1 != c2)
			return 'S';
		return 'M';

	}

}
