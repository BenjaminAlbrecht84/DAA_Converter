package util;

import java.util.Arrays;

public class SparseString {

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

}
