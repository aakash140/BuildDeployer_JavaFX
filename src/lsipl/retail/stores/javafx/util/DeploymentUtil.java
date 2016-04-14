package lsipl.retail.stores.javafx.util;

import java.io.File;

import org.apache.log4j.Logger;

public class DeploymentUtil {

	private static Logger logger = Logger.getLogger(DeploymentUtil.class);

	public boolean isDisableFields() {
		logger.info("Confirming if StoreID and RegisterID fields are to be disabled: DeploymentUtil.isDisableFields()");
		File file = new File("product/shared/pos/config/application.properties");
		return !file.exists();
	}

	public boolean isValidDirectory(String directory) {
		logger.info("Validating directory existense: DeploymentUtil.isValidDirectory(String directory)");
		File file = new File(directory);
		return file.exists();
	}
}