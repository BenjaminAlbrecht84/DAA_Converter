package util;

import java.util.Arrays;

public class SparseString implements Comparable<SparseString> {

	private final byte[] data;

	public SparseString(String s) {
		this.data = s.getBytes();
	}

	public SparseString(SparseString s) {
		this.data = s.getData().clone();
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SparseString))
			return false;
		return Arrays.equals(data, ((SparseString) o).data);
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(data);
	}

	@Override
	public String toString() {
		return new String(data);
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public int compareTo(SparseString s2) {
		byte[] data2 = s2.getData();
		for (int i = 0; i < Math.min(data.length, data2.length); i++) {
			if (data[i] > data2[i])
				return 1;
			else if (data[i] < data2[i])
				return -1;
		}
		if (data.length < data2.length)
			return -1;
		if (data.length > data2.length)
			return 1;
		return 0;
	}

}
