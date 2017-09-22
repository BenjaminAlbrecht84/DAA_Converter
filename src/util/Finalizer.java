package util;

import java.io.File;

public class Finalizer implements Runnable {
	
	private File tmpFolder;

	public Finalizer(File tmpFolder) {
		this.tmpFolder = tmpFolder;
	}

	@Override
	public void run() {
		deleteDir(tmpFolder);
	}
	
	private boolean deleteDir(File dir) {
		if (dir != null && dir.isDirectory()) {
			File[] files = dir.listFiles();
			for (int i = 0; i < files.length; i++) {
				if (files[i].isDirectory())
					deleteDir(files[i]);
				else
					files[i].delete();
			}
			return dir.delete();
		}
		return false;
	}
	
}
