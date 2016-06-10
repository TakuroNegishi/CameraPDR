package hosei.negishi.pdrtam.model;

import hosei.negishi.pdrtam.app.MainActivity;

import java.io.File;

import android.app.AlertDialog;

public class FileChooser {

	private File[] fileList;
	
	public FileChooser() {
	}
	
	public void showFiles(File parent, MainActivity activity) {
		fileList = parent.listFiles();
		AlertDialog.Builder listDlg = new AlertDialog.Builder(activity);
		listDlg.setTitle("ファイル選択");
		listDlg.setItems(parent.list(), activity);
		listDlg.create().show();
	}
	
	public void showFiles(int index, MainActivity activity) {
		showFiles(fileList[index], activity);
	}
	
	public boolean isDirectory(int index) {
		return fileList[index].isDirectory();
	}

//	@Override
//	public void onClick(DialogInterface dialog, int which) {
//		if (fileList[which].isDirectory()) {
//			showFiles(fileList[which]);
//		} else {
//			filePath = fileList[which].getAbsolutePath();
//		}
//	}
	
	public String getFilePath(int index) {
		return fileList[index].getAbsolutePath();
	}
}
