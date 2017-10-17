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

package hits;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import util.SparseString;

public class ReadHits {

	private ConcurrentHashMap<SparseString, ConcurrentHashMap<Integer, Vector<Hit>>> hitMap = new ConcurrentHashMap<SparseString, ConcurrentHashMap<Integer, Vector<Hit>>>();

	public void add(Hit h, SparseString gi, int frame) {

		if (!hitMap.containsKey(gi))
			hitMap.put(gi, new ConcurrentHashMap<Integer, Vector<Hit>>());
		ConcurrentHashMap<Integer, Vector<Hit>> frameMap = hitMap.get(gi);

		if (!frameMap.containsKey(frame))
			frameMap.put(frame, new Vector<Hit>());
		Vector<Hit> hits = frameMap.get(frame);

		hits.add(h);

	}

	public ConcurrentHashMap<SparseString, ConcurrentHashMap<Integer, Vector<Hit>>> getHitMap() {
		return hitMap;
	}

	public Vector<Hit> getAllHits() {
		Vector<Hit> allHits = new Vector<Hit>();
		for (SparseString gi : hitMap.keySet()) {
			for (int frame : hitMap.get(gi).keySet()) {
				for (Hit h : hitMap.get(gi).get(frame)) {
					allHits.add(h);
				}
			}
		}
		return allHits;
	}

	public void print() {
		for (SparseString gi : hitMap.keySet()) {
			System.out.println(">GI: " + gi);
			for (int frame : hitMap.get(gi).keySet()) {
				System.out.println("\t>>Frame: " + frame);
				for (Hit h : hitMap.get(gi).get(frame)) {
					h.print("\t");
				}
			}
		}
	}

	public void freeFrameHits(SparseString gi, int frame) {
		hitMap.get(gi).remove(frame);
	}

	public void freeGiHits(SparseString gi) {
		for (int frame : hitMap.get(gi).keySet())
			freeFrameHits(gi, frame);
		hitMap.remove(gi);
	}

}
