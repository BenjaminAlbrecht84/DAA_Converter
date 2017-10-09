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
