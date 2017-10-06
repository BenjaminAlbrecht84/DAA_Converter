package util;

import java.io.File;
import java.io.FileFilter;

public class Finalizer implements Runnable {

	private File tmpFolder;

	public Finalizer(File tmpFolder) {
		this.tmpFolder = tmpFolder;
	}

	@Override
	public void run() {
		deleteDir(tmpFolder);
	}

	private void deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			File[] files = dir.listFiles((new FileFilter() {
				@Override
				public boolean accept(File file) {
					return true;
				}
			}));
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory())
					deleteDir(files[i]);
				else
					files[i].deleteOnExit();
			}
			dir.deleteOnExit();
		}
	}

}
