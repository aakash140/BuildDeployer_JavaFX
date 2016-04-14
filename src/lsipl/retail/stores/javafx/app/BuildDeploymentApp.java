package lsipl.retail.stores.javafx.app;

import org.apache.log4j.Logger;

import lsipl.retail.stores.javafx.ui.ApplicationUI;

public class BuildDeploymentApp {

	private static Logger logger = Logger.getLogger(BuildDeploymentApp.class);

	public static void launchApplication(String... args) {
		logger.info("Launching application in BuildDeploymentApp.launchApplication(String... args)");
		ApplicationUI.displayUserInterface(args);
	}

	public static void main(String... args) {
		logger.info("Beginning application in BuildDeploymentApp.main(String... args)");
		launchApplication(args);
	}
}