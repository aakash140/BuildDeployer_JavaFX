package lsipl.retail.stores.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
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
import lsipl.retail.stores.javafx.util.FileQueue;

public class DeploymentWorker {

	private static Logger logger = Logger.getLogger(DeploymentWorker.class);

	private Thread javaFxThread = null;

	public void deployCompleteBuild(ProgressBar pb, Text pbText, String deployDir, String storeID, String registerID,
			Thread thread) throws IOException, FileNotFoundException, ConfigurationException {
		logger.info("Initiating complete build deployment.: DeploymentWorker.deployCompleteBuild()");
		javaFxThread = thread;
		if (javaFxThread.isAlive()) {
			deployServerBuild(pb, pbText, deployDir, thread);
			if (javaFxThread.isAlive())
				deployClientBuild(pb, pbText, deployDir, storeID, registerID, thread);
			else {
				logger.info("Exiting application since JAVAFX application thread is not alive");
				System.exit(0);
			}
		} else {
			logger.info("Exiting application since JAVAFX application thread is not alive");
			System.exit(0);
		}

	}

	public void deployServerBuild(ProgressBar pb, Text pbText, String deployDir, Thread thread)
			throws IOException, FileNotFoundException {
		logger.info("Initiating server build deployment.: DeploymentWorker.deployServerBuild()");
		javaFxThread = thread;
		Task<Object> task = new Task<Object>() {
			@Override
			public Object call() throws IOException, ConfigurationException {
				if (javaFxThread.isAlive()) {
					try {
						FileQueue fileQueue = new FileQueue();
						File dir = new File("product\\");
						queueUpFiles(dir, fileQueue);
						String deploymentDirectory = deployDir + "\\Server";
						int totalTasks = fileQueue.getNumberOfFiles();
						int tasksComplete = 0;

						String progress = "Deployment Successful";
						updateMessage(progress);
						updateProgress(tasksComplete, totalTasks);

						while (!fileQueue.isEmpty() && javaFxThread.isAlive()) {
							File file = fileQueue.pop();
							String fileName = file.toString();

							if (fileName.indexOf("application.properties") <= 0) {
								deploy(deploymentDirectory, file);
							}

							if (fileQueue.isEmpty()) {
								progress = "Server Deployed";
								updateMessage(progress);
								updateProgress(++tasksComplete, totalTasks);
							} else
								updateProgress(++tasksComplete, totalTasks);
						}
					} catch (Exception exception) {
						throw exception;
					}
				} else {
					logger.info("Exiting application since JAVAFX application thread is not alive");
					System.exit(0);
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
				String headerText = "Error occured while deploying Server";
				displayErrorAlert(exception, headerText);
			}
		});
		new Thread(task).start();
	}

	public void deployClientBuild(ProgressBar pb, Text pbText, String deployDir, String storeID, String registerID,
			Thread thread) throws IOException, FileNotFoundException, ConfigurationException {
		logger.info("Initiating client build deployment.: DeploymentWorker.deployClientBuild()");
		javaFxThread = thread;
		Task<Object> task = new Task<Object>() {
			@Override
			public Object call() throws IOException, ConfigurationException {
				if (javaFxThread.isAlive()) {
					try {
						FileQueue fileQueue = new FileQueue();
						File dir = new File("product\\");
						queueUpFiles(dir, fileQueue);
						String deploymentDirectory = deployDir + "\\Client";
						int totalTasks = fileQueue.getNumberOfFiles() + 1;
						int tasksComplete = 0;

						String progress = "Updating store details";
						updateMessage(progress);
						updateProgress(tasksComplete, totalTasks);

						updateStoreDetails(storeID, registerID);

						progress = "Store details updated";
						updateMessage(progress);
						updateProgress(++tasksComplete, totalTasks);

						progress = "Deploying Client";
						updateMessage(progress);
						updateProgress(tasksComplete, totalTasks);

						while (!fileQueue.isEmpty() && javaFxThread.isAlive()) {
							File file = fileQueue.pop();

							deploy(deploymentDirectory, file);

							if (fileQueue.isEmpty()) {
								progress = "Deployment Successful";
								updateMessage(progress);
								updateProgress(++tasksComplete, totalTasks);
							} else
								updateProgress(++tasksComplete, totalTasks);
						}
					} catch (Exception exception) {
						throw exception;
					}
				} else {
					logger.info("Exiting application since JAVAFX application thread is not alive");
					System.exit(0);
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
				String headerText = "Error occured while deploying Client";
				displayErrorAlert(exception, headerText);
			}
		});

		new Thread(task).start();
	}

	private void deploy(String deploymentDir, File newFile) throws IOException, FileNotFoundException {
		String newFilePath = getAbsolutePath(newFile.toString());
		File dest = new File(deploymentDir + newFilePath);
		dest.setWritable(true);
		dest.setExecutable(true);

		logger.info("Deploying build at " + dest.toString() + " :DeploymentWorker.deploy()");
		int length = 0;

		byte buffer[] = new byte[1024];

		if (newFile.exists() && dest.exists()) {
			try (FileInputStream fin = new FileInputStream(newFile);
					FileOutputStream fout = new FileOutputStream(dest);) {

				while ((length = fin.read(buffer)) != -1 && javaFxThread.isAlive()) {
					fout.write(buffer, 0, length);
				}
				fin.close();
				fout.close();
			} catch (IOException exception) {
				logger.error(exception);
				throw exception;
			}
		} else {
			if (!newFile.exists()) {
				logger.error(new FileNotFoundException(newFile.toString()));
				throw new FileNotFoundException(newFile.toString());
			} else if (!dest.exists()) {
				logger.error(new FileNotFoundException(dest.toString()));
				throw new FileNotFoundException(dest.toString());
			}
		}
	}

	private String getAbsolutePath(String path) {
		logger.info("Getting customized absolute path for " + path);
		int index = 0;
		if ((index = path.indexOf("\\common\\")) > 0)
			path = path.substring(index, path.length());

		if ((index = path.indexOf("\\pos\\")) > 0)
			path = path.substring(index, path.length());

		logger.info("New Absolute path is: " + path);
		return path;
	}

	private void queueUpFiles(File dir, FileQueue filequeue) throws FileNotFoundException {

		if (dir.isDirectory() && javaFxThread.isAlive()) {
			for (File file : dir.listFiles()) {
				if (file.isDirectory())
					queueUpFiles(file, filequeue);
				else {
					logger.info("Pusing " + file.toString() + " in FileQueue");
					filequeue.push(file);
				}
			}
		} else {
			logger.info("Exiting application since JAVAFX application thread is not alive");
			System.exit(0);
		}
	}

	private void updateStoreDetails(String storeID, String registerID)
			throws FileNotFoundException, ConfigurationException {
		logger.info("Updating StoreId and RegisterId in application.properties.");
		File appPropFile = new File("product/shared/pos/config/application.properties");
		if (appPropFile.exists()) {
			PropertiesConfiguration prop = new PropertiesConfiguration(appPropFile);
			prop.setProperty("StoreID", storeID);
			prop.setProperty("WorkstationID", registerID);
			prop.save(appPropFile);
			logger.info(
					"Updated StoreId: " + storeID + " and RegisterId: " + registerID + " in application.properties.");
		} else {
			logger.error(new FileNotFoundException("application.properties"));
			throw new FileNotFoundException("application.properties");
		}
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
