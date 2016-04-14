package lsipl.retail.stores.javafx.util;

import java.io.File;

import org.apache.log4j.Logger;

public class FileQueue {

	private static Logger logger = Logger.getLogger(FileQueue.class);

	int front, rear, queueSize, files;
	File[] fileQueue;

	public FileQueue() {
		this.queueSize = 50;
		files = 0;
		front = 0;
		rear = -1;
		fileQueue = new File[queueSize];
	}

	public int getNumberOfFiles() {
		return files;
	}

	public boolean isEmpty() {
		return files == 0;
	}

	public boolean isFull() {
		return files == queueSize;
	}

	public void push(File newFile) {
		logger.info("Pushing " + newFile.toString() + " in FileQueue");
		if (rear == queueSize - 1)
			rear = -1;
		fileQueue[++rear] = newFile;
		++files;
	}

	public File pop() {
		File nextFile = fileQueue[front++];
		logger.info("Popping " + nextFile.toString() + " from FileQueue");
		if (front == queueSize)
			front = 0;
		--files;
		return nextFile;
	}
}
