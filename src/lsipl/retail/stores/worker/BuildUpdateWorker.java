package lsipl.retail.stores.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import lsipl.retail.stores.javafx.app.BuildDeploymentApp;
import lsipl.retail.stores.javafx.util.DeploymentUtil;
import lsipl.retail.stores.javafx.util.FileQueue;

public class BuildUpdateWorker {

	private Thread javafxThread = null;
	private static Logger logger = Logger.getLogger(BuildDeploymentApp.class);

	public void updateBuild(ProgressBar pb, Text pbText, Thread currentThread)
			throws IOException, InterruptedException {
		logger.info("Initiating Build Update Process: BuildUpdateWorker.updateBuild()");
		javafxThread = currentThread;
		Task<Object> task = new Task<Object>() {
			@Override
			public Object call() throws IOException, InterruptedException {
				try {
					double totalTasks = 5.0;
					double taskComplete = 0.0;
					File downloadLoc = null;
					String progress = "Downloading Latest Build";
					updateMessage(progress);
					updateProgress(taskComplete, totalTasks);

					if (javafxThread.isAlive())
						downloadLoc = getLatestBuild();
					else {
						logger.info("Exiting application since JAVAFX application thread is not alive");
						System.exit(0);
					}

					progress = "Buid Downloaded from FTP";
					updateMessage(progress);
					updateProgress(++taskComplete, totalTasks);

					FileQueue fileQueue = new FileQueue();

					if (downloadLoc != null && downloadLoc.exists() && javafxThread.isAlive()) {
						logger.info("Build downloaded at " + downloadLoc.toString());
						progress = "Queueing up files";
						updateMessage(progress);

						queueUpFiles(downloadLoc, fileQueue);
						updateProgress(++taskComplete, totalTasks);
					}
					progress = "Backing up old build";
					updateMessage(progress);

					if (javafxThread.isAlive())
						backupOldBuild();
					else {
						logger.info("Exiting application since JAVAFX application thread is not alive");
						System.exit(0);
					}

					updateProgress(++taskComplete, totalTasks);

					progress = "Initiating installation";
					updateMessage(progress);

					if (javafxThread.isAlive())
						installNewBuild(fileQueue);

					else {
						logger.info("Exiting application since JAVAFX application thread is not alive");
						System.exit(0);
					}

					updateProgress(++taskComplete, totalTasks);

					progress = "Deleting temporary files";
					updateMessage(progress);

					FileUtils.deleteDirectory(downloadLoc);
					updateProgress(++taskComplete, totalTasks);

					progress = "\tUpdate Complete";
					updateMessage(progress);

					updateReport();
				} catch (Exception exception) {
					throw exception;
				}
				return null;
			}

		};

		pb.progressProperty().unbind();
		pb.progressProperty().bind(task.progressProperty());

		task.messageProperty().addListener((ob, oldVal, newVal) -> {
			pbText.setText(newVal);
		});

		task.exceptionProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null) {
				Exception exception = (Exception) newValue;
				String headerText = "Error occured while updating build.";
				displayErrorAlert(exception, headerText);
			}
		});

		new Thread(task).start();
	}

	private void installNewBuild(FileQueue fileQueue) throws IOException, InterruptedException {
		logger.info("Initiating build installation process: BuildUpdateWorker.installNewBuild()	");
		while (!fileQueue.isEmpty() && javafxThread.isAlive()) {
			File src = fileQueue.pop();
			String temp = src.getPath();
			String destStr = temp.substring(temp.indexOf("product\\"), temp.length());
			File dest = new File(destStr);
			logger.info("Copying " + src.toString() + " " + dest.toString());
			FileInputStream fin = new FileInputStream(src);
			FileOutputStream fout = new FileOutputStream(dest);
			byte[] buffer = new byte[1024];
			int length = 0;
			while ((length = fin.read(buffer)) != -1 && javafxThread.isAlive()) {
				fout.write(buffer, 0, length);
			}

			fin.close();
			fout.close();
		}
	}

	private File getLatestBuild() throws IOException {
		logger.info("Initiating the process for fetching latest build: BuildUpdateWorker.getLatestBuild()");
		DeploymentUtil depUtil = new DeploymentUtil();
		File downloadLoc = null;
		Properties prop = new Properties();
		File file = new File("update/FTP_loc.properties");
		InputStream is = new FileInputStream(file);
		prop.load(is);
		String ftpPath = prop.getProperty("FTP_Directory");

		logger.info("FTP directory: " + ftpPath);

		if (depUtil.isValidDirectory(ftpPath)) {
			if (depUtil.isValidDirectory(ftpPath + "/server")) {
				if (depUtil.isValidDirectory(ftpPath + "/client")) {
					if (depUtil.isValidDirectory(ftpPath + "/shared")) {
						downloadLoc = downloadBuild(new File(ftpPath));
					} else {
						logger.error(new FileNotFoundException(ftpPath + "/shared"));
						throw new FileNotFoundException(ftpPath + "/shared");
					}
				} else {
					logger.error(new FileNotFoundException(ftpPath + "/client"));
					throw new FileNotFoundException(ftpPath + "/client");
				}
			} else {
				logger.error(new FileNotFoundException(ftpPath + "/server"));
				throw new FileNotFoundException(ftpPath + "/server");
			}
		} else {
			logger.error(new FileNotFoundException(ftpPath));
			throw new FileNotFoundException(ftpPath);
		}

		return downloadLoc;
	}

	private void queueUpFiles(File dir, FileQueue fileQueue) {
		if (dir.isDirectory() && javafxThread.isAlive()) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory())
					queueUpFiles(file, fileQueue);
				else {
					logger.info("Pusing " + file.toString() + " in FileQueue");
					fileQueue.push(file);
				}
			}
		} else {
			logger.info("Exiting application since JAVAFX application thread is not alive");
			System.exit(0);
		}
	}

	private File downloadBuild(File ftpLoc) throws IOException {

		File downloadLoc = new File("./update/temp/product");
		if (downloadLoc.exists())
			downloadLoc.delete();
		downloadLoc.mkdir();

		logger.info("Downloading build from " + ftpLoc.toString() + " to " + downloadLoc.toString());

		FileUtils.copyDirectory(ftpLoc, downloadLoc);
		return downloadLoc;
	}

	private void backupOldBuild() throws IOException {
		File oldBuildLoc = new File("./product");
		String time = LocalDateTime.now().toString();
		time = time.replaceAll("[:.T-]", "");
		File backupLoc = new File("./build_backups/" + time + "/product");
		if (backupLoc.exists())
			backupLoc.delete();
		backupLoc.mkdir();

		logger.info("Creating a backup for old build at " + backupLoc.toString());
		FileUtils.copyDirectory(oldBuildLoc, backupLoc);
		logger.info("Cleaning up files in  " + oldBuildLoc.toString());
		cleanDirectory(oldBuildLoc);
	}

	private void cleanDirectory(File dir) throws IOException {
		if (dir.isDirectory()) {
			for (File file : dir.listFiles())
				if (file.isDirectory())
					cleanDirectory(file);
				else {
					logger.info("Deleted file " + file.toString() + " during file cleanup process.");
					file.delete();
				}
		}
	}

	private void updateReport() throws FileNotFoundException, IOException {
		logger.info("Updating build update report.");
		DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:SS");
		String time = LocalDateTime.now().format(format).toString();
		String reportText = "Build Updated on " + time;
		FileWriter fw = new FileWriter("update\\BuildUpdateReport.txt", true);
		fw.append(reportText + "\n");
		fw.close();
	}

	private void displayErrorAlert(Exception exception, String headerText) {
		logger.info("Displaying Error alert :ApplicationUI.displayErrorAlert(String headerText)");
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);

		Label errorCause = new Label("Following is the error cause");
		TextArea exceptionText = new TextArea(sw.toString());
		exceptionText.setWrapText(true);
		exceptionText.setEditable(false);

		GridPane grid = new GridPane();
		grid.add(errorCause, 0, 0);
		grid.add(exceptionText, 0, 1);

		Alert error = new Alert(AlertType.ERROR);
		error.setHeaderText(headerText);
		error.setContentText("Press OK to return");
		error.getDialogPane().setExpandableContent(grid);
		Optional<ButtonType> buttonPressed = error.showAndWait();
		if (buttonPressed.isPresent()) {
			if (buttonPressed.get().getText().equalsIgnoreCase("OK")) {
				Platform.exit();
				System.exit(0);
			}
		}
	}

}