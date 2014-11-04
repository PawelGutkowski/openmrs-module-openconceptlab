package org.openmrs.module.openconceptlab;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.CountingInputStream;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.openmrs.module.openconceptlab.OclClient.OclResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("openconceptlab.updater")
public class Updater implements Runnable {
	
	@Autowired
	UpdateService updateService;
	
	@Autowired
	OclClient oclClient;
	
	@Autowired
	Importer importer;
	
	private CountingInputStream in = null;
	
	private volatile long totalBytesToProcess;
			
	/**
	 * @should start first update with response date
	 * @should start next update with updated since
	 * @should create item for each concept
	 */
	@Override
	public void run() {		
		Subscription subscription = updateService.getSubscription();
		Update lastUpdate = updateService.getLastUpdate();
		Date updatedSince = null;
		if (lastUpdate != null) {
			updatedSince = lastUpdate.getOclDateStarted();
		}
		
		OclResponse oclResponse;
		try {
			oclResponse = oclClient.fetchUpdates(subscription.getUrl(), updatedSince);
		}
		catch (IOException e) {
			throw new ImportException(e);
		}
		
		Update update = new Update();
		update.setOclDateStarted(oclResponse.getUpdatedTo());
		updateService.startUpdate(update);
		
		in = new CountingInputStream(oclResponse.getContentStream());
		totalBytesToProcess = oclResponse.getContentLength();
		try {
			process(update, in);
			in.close();
		}
		catch (IOException e) {
			throw new ImportException(e);
		}
		finally {
			IOUtils.closeQuietly(in);
			updateService.stopUpdate(update);
		}
	}
	
	public long getBytesDownloaded() {
		return oclClient.getBytesDownloaded();
	}
	
	public long getTotalBytesToDownload() {
		return oclClient.getTotalBytesToDownload();
	}
	
	public boolean isDownloaded() {
		return oclClient.isDownloaded();
	}
	
	public long getBytesProcessed() {
		if (in != null) {
			return in.getByteCount();
		} else {
			return 0;
		}
	}
	
	public long getTotalBytesToProcess() {
		return totalBytesToProcess;
	}
	
	void process(Update update, InputStream in) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonParser parser = objectMapper.getJsonFactory().createJsonParser(in);
		
		JsonToken token = parser.nextToken();
		if (token != JsonToken.START_OBJECT && token != JsonToken.START_ARRAY) {
			throw new IOException("JSON must start from an object or an array");
		}
		if (token == JsonToken.START_OBJECT) {
			token = parser.nextToken();
		}
		
		if (token == JsonToken.END_OBJECT) {
			return;
		}
		if (token != JsonToken.START_ARRAY) {
			throw new IOException("JSON must have a list of concepts or an empty list");
		}
		
		ImportQueue importQueue = new ImportQueue();
		
		while (parser.nextToken() != JsonToken.END_ARRAY) {
			OclConcept oclConcept = parser.readValueAs(OclConcept.class);
			
			importQueue.offer(oclConcept);
			
			while (!importQueue.isEmpty()) {
				Item item;
				try {
					oclConcept = importQueue.peek();
					item = importer.importConcept(update, importQueue);
				}
				catch (ImportException e) {
					item = new Item(update, oclConcept, State.ERROR);
				}
				
				if (!State.MISSING_DEPENDENCY.equals(item.getState())) {
					updateService.saveItem(item);
				}
			}
		}
	}
	
}