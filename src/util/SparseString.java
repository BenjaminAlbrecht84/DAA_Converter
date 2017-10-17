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
