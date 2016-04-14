package lsipl.retail.stores.javafx.ui;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lsipl.retail.stores.javafx.util.DeploymentUtil;
import lsipl.retail.stores.worker.BuildUpdateWorker;
import lsipl.retail.stores.worker.DeploymentWorker;

public class ApplicationUI extends Application {

	private static Logger logger = Logger.getLogger(ApplicationUI.class);

	private MenuBar mainMenu;
	private Hyperlink aboutMe = new Hyperlink("Aakash Gupta");

	private ComboBox<String> deploymentOption;
	private ObservableList<String> deploymentList;

	private Label deploymentLabel;
	private Label directoryLabel;
	private Label storeIDLabel;
	private Label registerIDLabel;

	private TextField directoryField;
	private TextField storeIDField;
	private TextField registerIDField;

	private Button deploy;
	private Button reset;

	private ProgressBar pb;
	private Text pbText = new Text("Waiting for job(s)");

	private InnerShadow shadow;

	private boolean isUpdateProcess;

	public static void displayUserInterface(String[] args) {
		Application.launch(args);
	}

	@Override
	public void start(Stage stage) {
		logger.info("ApplicationUI.start()");
		stage.setTitle("ORPOS Build Deployment Utility");
		stage.setResizable(false);
		BorderPane rootNode = new BorderPane();
		Scene scene = new Scene(rootNode, 550, 500);

		configureMenus(scene, stage);
		rootNode.setTop(mainMenu);

		configureLabels();

		configureTextFields();

		configureComboBox();

		configureButtons();

		configureShadow();

		configureProgressBar();

		GridPane grid = new GridPane();
		paintGridPane(grid);

		rootNode.setCenter(grid);

		rootNode.setBottom(new ImageView(getImage("footer.jpg")));

		stage.setScene(scene);
		stage.show();
	}

	private void configureShadow() {
		logger.info("Configuring shadow: ApplicationUI.configureShadow()");
		shadow = new InnerShadow();
		shadow.setRadius(10.0);
		shadow.setColor(Color.RED);
	}

	private void configureButtons() {
		logger.info("Configuring buttons: ApplicationUI.configureButtons()");
		Font font = Font.font(null, FontWeight.BOLD, 15);
		deploy = new Button("Deploy Build", new ImageView(getImage("Deploy.png")));
		deploy.setPrefWidth(150);
		deploy.setFont(font);

		deploy.setOnAction((ae) -> {
			directoryField.setEffect(null);
			if (isVaildDeployment()) {
				String headerText = "Please Note:\n 1. This will deploy the build on existing store setup.\n 2. Changes made will be irreversible.";
				ButtonType choice = displayConfirmationAlert(headerText);
				if (choice.getText().equals("OK")) {
					disableAllNodes();
					isUpdateProcess = false;
					try {
						DeploymentWorker deployWorker = new DeploymentWorker();
						switch (deploymentOption.getValue()) {
						case "Server":
							deployWorker.deployServerBuild(pb, pbText, directoryField.getText(),
									Thread.currentThread());
							break;

						case "Client":
							deployWorker.deployClientBuild(pb, pbText, directoryField.getText(), storeIDField.getText(),
									registerIDField.getText(), Thread.currentThread());
							break;

						case "Both":
							deployWorker.deployCompleteBuild(pb, pbText, directoryField.getText(),
									storeIDField.getText(), registerIDField.getText(), Thread.currentThread());
							break;

						}

					} catch (IOException | ConfigurationException exception) {
						logger.error(exception);
						headerText = "Exception occured while deploying build";
						displayErrorAlert(exception, headerText);
					}
				}
			}
		});

		reset = new Button("Reset All", new ImageView(getImage("Reset.png")));
		reset.setPrefWidth(150);
		reset.setFont(font);

		reset.setOnAction((ae) -> {
			directoryField.setEffect(null);
			deploymentOption.setEffect(null);
			registerIDField.setEffect(null);
			storeIDField.setEffect(null);
			resetAllNodes();
		});
	}

	private void configureTextFields() {
		logger.info("Configuring text fields: ApplicationUI.configureTextFields()");
		directoryField = new TextField();
		directoryField
				.setTooltip(new Tooltip("Directory where the build will be deployed. e.g.  E:\\OracleRetailStore"));
		directoryField.setPromptText("E:\\OracleRetailStore");

		storeIDField = new TextField();
		storeIDField.setTooltip(new Tooltip("Store Id of the store where the build will be deployed."));
		storeIDField.setPromptText("04241");
		storeIDField.setDisable(true);

		registerIDField = new TextField();
		registerIDField.setTooltip(new Tooltip("Register ID of the register where the build will be deployed."));
		registerIDField.setPromptText("101");
		registerIDField.setDisable(true);
	}

	private void configureLabels() {
		logger.info("Configuring labels: ApplicationUI.configureLabels()");
		Font font = Font.font(null, FontWeight.BOLD, 12);

		deploymentLabel = new Label("Deployment Type: ");
		deploymentLabel.setFont(font);

		directoryLabel = new Label("Deployment Directory: ");
		directoryLabel.setFont(font);

		storeIDLabel = new Label("Store ID: ");
		storeIDLabel.setFont(font);

		registerIDLabel = new Label("Register ID: ");
		registerIDLabel.setFont(font);
	}

	private void configureProgressBar() {
		logger.info("Configuring progress bar: ApplicationUI.configureProgressBar()");
		pb = new ProgressBar(-1);
		pb.setPrefWidth(300);

		pb.indeterminateProperty().addListener((observable, oldVal, newVal) -> {
			if (newVal) {
				pbText.setText("Waiting");
				pbText.setFill(Color.BLACK);
				pb.setEffect(null);
			} else {
				pbText.setText("Processing");
				pbText.setFill(Color.BLACK);
				pb.setEffect(null);
			}
		});

		pb.progressProperty().addListener(new ChangeListener<Number>() {

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldVal, Number newVal) {
				if (newVal.floatValue() == 1) {
					pbText.setFill(Color.GREEN);
					ColorAdjust colorRemote = new ColorAdjust();
					colorRemote.setHue(-0.3);
					pb.setEffect(colorRemote);
					String headerText = "Build has been deployed successfully at \n" + directoryField.getText();
					if (isUpdateProcess) {
						headerText = "Build Updated Successfuly.";
					} else
						pbText.setText("Build Deployed");
					displayInformationAlert(headerText);
				}
			}
		});
	}

	private void configureMenus(Scene scene, Stage stage) {
		logger.info("Configuring menus: ApplicationUI.configureMenus(Scene scene, Stage stage)");
		Menu file = new Menu("File");
		MenuItem open = new MenuItem("Open");
		MenuItem exit = new MenuItem("Exit");
		file.getItems().addAll(open, new SeparatorMenuItem(), exit);

		Menu tools = new Menu("Tools");
		MenuItem update = new MenuItem("Update Build");
		tools.getItems().addAll(update);

		Menu help = new Menu("Help");
		MenuItem helpGuide = new MenuItem("Help Guide");
		MenuItem about = new MenuItem("About");
		help.getItems().addAll(helpGuide, about);

		mainMenu = new MenuBar(file, tools, help);

		open.setAccelerator(KeyCombination.keyCombination("Shortcut+O"));
		exit.setAccelerator(KeyCombination.keyCombination("Shortcut+E"));
		update.setAccelerator(KeyCombination.keyCombination("Shortcut+U"));
		helpGuide.setAccelerator(KeyCombination.keyCombination("F1"));
		about.setAccelerator(KeyCombination.keyCombination("F3"));

		open.setGraphic(new ImageView(getImage("open.png")));
		exit.setGraphic(new ImageView(getImage("exit.png")));
		update.setGraphic(new ImageView(getImage("update.png")));
		helpGuide.setGraphic(new ImageView(getImage("helpguide.png")));
		about.setGraphic(new ImageView(getImage("about.png")));

		scene.setOnKeyReleased(new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent ke) {
				if (ke.getCode() == KeyCode.F1)
					helpGuide.fire();
				else if (ke.getCode() == KeyCode.F3)
					about.fire();
				ke.consume();
			}
		});

		open.setOnAction((ae) -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(new File("C:\\Users\\" + System.getProperty("user.name") + "\\Desktop"));
			File openedFile = fileChooser.showOpenDialog(stage);
			try {
				if (openedFile != null)
					Desktop.getDesktop().open(openedFile);
			} catch (IOException | NullPointerException exception) {
				logger.error(exception);
				String headerText = "Sorry!! the file coud not be opened.";
				displayErrorAlert(exception, headerText);
			}
		});

		exit.setOnAction((ae) -> {
			Platform.exit();
		});

		update.setOnAction((ae) -> {
			BuildUpdateWorker updateWorker = new BuildUpdateWorker();
			try {
				updateWorker.updateBuild(pb, pbText, Thread.currentThread());
				isUpdateProcess = true;
			} catch (IOException | InterruptedException exception) {
				logger.error(exception);
				String headerText = "Error occured while updating the build.";
				displayErrorAlert(exception, headerText);
			}
		});

		helpGuide.setOnAction((ae) -> {
			File guide = new File("help/HelpGuide.docx");
			try {
				Desktop.getDesktop().open(guide);
			} catch (IOException | NullPointerException | IllegalArgumentException exception) {
				logger.error(exception);
				String headerText = "Sorry!! the help guide is not available.";
				displayErrorAlert(exception, headerText);
			}
		});

		about.setOnAction((ae) -> {
			String headerText = "Release Version:	 1.0";
			displayAboutAlert(headerText);
		});

		aboutMe.setOnAction((ae) -> {
			try {
				Desktop.getDesktop().browse(new URI("https://goo.gl/PHqIa5"));
			} catch (IOException | URISyntaxException exception) {
				logger.error(exception);
				String headerMessage = "Sorry!! There is a problem with the URL.";
				displayErrorAlert(exception, headerMessage);
			}
		});
	}

	private void configureComboBox() {
		logger.info("Configuring combo box: ApplicationUI.configureComboBox()");
		deploymentList = FXCollections.observableArrayList("<Select>", "Server", "Client", "Both");

		DeploymentUtil util = new DeploymentUtil();

		deploymentOption = new ComboBox<>(deploymentList);
		deploymentOption.setValue("<Select>");
		deploymentOption.requestFocus();

		deploymentOption.setOnAction((ae) -> {
			String depSel = deploymentOption.getValue();

			if ("Server".equalsIgnoreCase(depSel)) {
				String directory = directoryField.getText();
				String headerText = "";
				if (directory.length() > 0) {
					if (util.isValidDirectory(directory + "/Server")) {
						registerIDField.setDisable(true);
						storeIDField.setDisable(true);
					} else {
						directoryField.setEffect(shadow);
						directoryField.requestFocus();
						headerText = "Invalid Directory for Server deployment.";
						displayInvalidDataAlert(headerText);
					}
				} else {
					directoryField.setEffect(shadow);
					directoryField.requestFocus();
					headerText = "Deployment directory field cannot be left blank";
					displayInvalidDataAlert(headerText);
				}
			} else if ("Client".equalsIgnoreCase(depSel)) {

				String directory = directoryField.getText();
				String headerText = "";
				if (directory.length() > 0) {
					if (util.isValidDirectory(directory + "/Client")) {
						String appDir = "product/shared/pos/config/application.properties";
						if (util.isValidDirectory(appDir)) {
							registerIDField.setDisable(false);
							storeIDField.setDisable(false);
						} else {
							registerIDField.setDisable(true);
							storeIDField.setDisable(true);
						}
					} else {
						directoryField.setEffect(shadow);
						directoryField.requestFocus();
						headerText = "Invalid directory for Client Deployment";
						displayInvalidDataAlert(headerText);
					}

				} else {
					directoryField.setEffect(shadow);
					directoryField.requestFocus();
					headerText = "Deployment directory field cannot be left blank";
					displayInvalidDataAlert(headerText);
				}
			} else if ("Both".equalsIgnoreCase(depSel)) {

				String directory = directoryField.getText();
				String headerText = "";
				if (directory.length() > 0) {
					if (util.isValidDirectory(directory + "/Server") && util.isValidDirectory(directory + "/Client")) {
						String appDir = "product/shared/pos/config/application.properties";
						if (util.isValidDirectory(appDir)) {
							registerIDField.setDisable(false);
							storeIDField.setDisable(false);
						} else {
							registerIDField.setDisable(true);
							storeIDField.setDisable(true);
						}
					} else {
						directoryField.setEffect(shadow);
						directoryField.requestFocus();
						headerText = "Invalid directory for Server and Client Deployment";
						displayInvalidDataAlert(headerText);
					}
				} else {
					directoryField.setEffect(shadow);
					directoryField.requestFocus();
					headerText = "Deployment directory field cannot be left blank";
					displayInvalidDataAlert(headerText);
				}
			} else if ("<Select>".equalsIgnoreCase(depSel)) {
				registerIDField.setDisable(true);
				storeIDField.setDisable(true);
			}

		});
	}

	private boolean isVaildDeployment() {
		logger.info("Validating data and selection before build deployment: ApplicationUI.isVaildDeployment()");
		String deplOption = deploymentOption.getValue();
		String headerText = "";
		String directory = directoryField.getText();
		DeploymentUtil util = new DeploymentUtil();
		boolean isValid = false;

		if (deplOption.equalsIgnoreCase("<Select>")) {
			deploymentOption.setEffect(shadow);
			deploymentOption.requestFocus();
			headerText = "A valid deployment option should be chosen for deployment.";
			displayInvalidDataAlert(headerText);
			return false;
		}
		if (deplOption.equalsIgnoreCase("Server")) {

			if (util.isValidDirectory(directory + "/Server"))
				isValid = true;
			else {
				directoryField.setEffect(shadow);
				directoryField.requestFocus();
				headerText = "Invalid Directory for Server deployment.";
				displayInvalidDataAlert(headerText);
				isValid = false;
			}

		} else if (deplOption.equalsIgnoreCase("Client")) {
			if (util.isValidDirectory(directory + "/Client"))
				isValid = true;
			else {
				directoryField.setEffect(shadow);
				directoryField.requestFocus();
				headerText = "Invalid directory for Client Deployment";
				displayInvalidDataAlert(headerText);
				isValid = false;
			}
		} else if (deplOption.equalsIgnoreCase("Both")) {
			if (util.isValidDirectory(directory)) {
				if (util.isValidDirectory(directory + "/Server") && util.isValidDirectory(directory + "/Client"))
					isValid = true;
				else {
					directoryField.setEffect(shadow);
					directoryField.requestFocus();
					headerText = "Invalid directory for Server and Client Deployment";
					displayInvalidDataAlert(headerText);
					isValid = false;
				}

			} else {
				directoryField.setEffect(shadow);
				directoryField.requestFocus();
				headerText = "Invalid directory for Server and Client Deployment";
				displayInvalidDataAlert(headerText);
				isValid = false;

			}

		}

		if (!registerIDField.isDisabled()) {
			int registerIDLength = registerIDField.getText().length();
			int storeIDLength = storeIDField.getText().length();
			if (storeIDLength == 5) {
				if (registerIDLength == 3)
					isValid = true;
				else {
					isValid = false;
					registerIDField.setEffect(shadow);
					registerIDField.requestFocus();
					headerText = "Invalid data in RegisterID field.";
					displayInvalidDataAlert(headerText);
				}
			} else {
				isValid = false;
				storeIDField.setEffect(shadow);
				storeIDField.requestFocus();
				headerText = "Invalid data in StoreID field.";
				displayInvalidDataAlert(headerText);
			}

		}

		return isValid;

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
		error.show();
	}

	private ButtonType displayConfirmationAlert(String headerText) {
		logger.info("Displaying Confirmation alert :ApplicationUI.displayInformationAlert(String headerText)");
		Alert warning = new Alert(AlertType.CONFIRMATION);
		warning.setHeaderText(headerText);
		warning.setContentText("Press OK to proceed; Cancel otherwise");
		Optional<ButtonType> buttonPressed = warning.showAndWait();
		return buttonPressed.isPresent() == true ? buttonPressed.get() : ButtonType.CANCEL;
	}

	private void displayInformationAlert(String headerText) {
		logger.info("Displaying Information alert :ApplicationUI.displayInformationAlert(String headerText)");
		Alert information = new Alert(AlertType.CONFIRMATION);
		information.setHeaderText(headerText);
		information.setContentText("Press OK to return");
		information.show();
	}

	private void displayInvalidDataAlert(String headerText) {
		logger.info("Displaying Invalid data alert :ApplicationUI.displayInvalidDataAlert(String headerText)");
		Alert invalidData = new Alert(AlertType.ERROR);
		invalidData.setHeaderText(headerText);
		invalidData.setContentText("Press OK to return and enter valid data.");
		invalidData.show();
	}

	private void displayAboutAlert(String headerText) {
		logger.info("Displaying About window:ApplicationUI.displayAboutAlert(String headerText)");
		FlowPane flowPane = new FlowPane(10, 10);
		flowPane.getChildren().addAll(new Label("Developed by: "), aboutMe);

		Alert aboutInfo = new Alert(AlertType.INFORMATION);
		aboutInfo.setTitle("About Utility");
		aboutInfo.setHeaderText(headerText);
		aboutInfo.getDialogPane().setContent(flowPane);
		aboutInfo.show();
	}

	private void resetAllNodes() {
		logger.info("Resetting all the the fields and combox:ApplicationUI.resetAllNodes()");
		directoryField.clear();
		registerIDField.clear();
		storeIDField.clear();
		deploymentOption.setValue("<Select>");
	}

	private void disableAllNodes() {
		logger.info("Disabling all the fields and buttons:ApplicationUI.disableAllNodes()");
		directoryField.setDisable(true);
		registerIDField.setDisable(true);
		storeIDField.setDisable(true);
		deploymentOption.setDisable(true);
		deploy.setDisable(true);
		reset.setDisable(true);
	}

	private void paintGridPane(GridPane grid) {
		logger.info("Painting Grid Pane: ApplicationUI.paintGridPane(GridPane grid)");
		grid.setAlignment(Pos.TOP_LEFT);
		BackgroundImage bckImg = new BackgroundImage(getImage("header.jpg"), BackgroundRepeat.NO_REPEAT,
				BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT);
		grid.setBackground(new Background(bckImg));

		grid.getColumnConstraints().add(0, new ColumnConstraints(30));
		grid.getColumnConstraints().add(1, new ColumnConstraints(150));
		grid.getColumnConstraints().add(2, new ColumnConstraints(300));
		grid.getColumnConstraints().add(3, new ColumnConstraints(100));
		grid.getColumnConstraints().add(4, new ColumnConstraints(100));

		for (int i = 0; i < 6; i++)
			grid.getRowConstraints().add(i, new RowConstraints(20));

		grid.add(directoryLabel, 1, 5);
		grid.add(directoryField, 2, 5);
		grid.getRowConstraints().add(6, new RowConstraints(20));
		grid.getRowConstraints().add(7, new RowConstraints(20));

		grid.add(deploymentLabel, 1, 7);
		grid.add(deploymentOption, 2, 7);
		grid.getRowConstraints().add(8, new RowConstraints(20));
		grid.getRowConstraints().add(9, new RowConstraints(20));

		grid.add(storeIDLabel, 1, 9);
		grid.add(storeIDField, 2, 9);
		grid.getRowConstraints().add(10, new RowConstraints(20));
		grid.getRowConstraints().add(11, new RowConstraints(20));

		grid.add(registerIDLabel, 1, 11);
		grid.add(registerIDField, 2, 11);
		grid.getRowConstraints().add(12, new RowConstraints(20));
		grid.getRowConstraints().add(13, new RowConstraints(20));

		grid.getRowConstraints().add(14, new RowConstraints(30));

		grid.add(deploy, 1, 14);
		grid.add(reset, 2, 14);

		grid.add(pbText, 1, 16);
		grid.add(pb, 2, 16);
		// grid.add(new Label("100%"), 3, 16);
		grid.getRowConstraints().add(15, new RowConstraints(30));
	}

	private Image getImage(String imageName) {
		logger.info("Fetching image " + imageName + " from localdisk");
		File imgFile = new File("images/" + imageName);
		Image img = null;
		try (FileInputStream fin = new FileInputStream(imgFile);) {
			img = new Image(fin);
		} catch (IOException exception) {
			logger.error(exception);
			String headerText = "Error loading " + imageName + " file";
			displayErrorAlert(exception, headerText);
		}
		return img;
	}

}