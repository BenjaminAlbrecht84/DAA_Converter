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

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class Finalizer implements Runnable {

	private ArrayList<File> tmpFiles;

	public Finalizer(ArrayList<File> tmpFiles) {
		this.tmpFiles = tmpFiles;
	}

	@Override
	public void run() {
		for (File f : tmpFiles)
			deleteFile(f);
	}

	private void deleteFile(File f) {
		if (f != null && f.isDirectory()) {
			File[] files = f.listFiles((new FileFilter() {
				@Override
				public boolean accept(File file) {
					return true;
				}
			}));
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory())
					deleteFile(files[i]);
				else
					deleteSingleFile(files[i]);
			}
			f.deleteOnExit();
		}
		if (f != null && f.isFile())
			deleteSingleFile(f);
	}

	private void deleteSingleFile(File f) {
		f.delete();
	}

}
