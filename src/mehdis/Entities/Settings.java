package mehdis.Entities;

import java.util.Locale;

public class Settings {
	private int numOfPasses;
	private int instanceCounter;
	private int logIndex;
	private int language;
	private Locale locale;
	
	private static Settings instance = null;
	
	protected Settings(){
		this.numOfPasses = 1;
		this.instanceCounter = 0;
		this.logIndex = 0;
		this.language = 0;
		this.locale = Locale.ENGLISH;
	}
	
	public static Settings getSettings(){
		if(instance == null){
			instance = new Settings();
		}
		return instance;
	}

	public int getNumOfPasses() {
		return numOfPasses;
	}

	public void setNumOfPasses(int numOfPasses) {
		this.numOfPasses = numOfPasses;
	}

	public int getInstanceCounter() {
		return instanceCounter;
	}

	public void setInstanceCounter(int instanceCounter) {
		this.instanceCounter = instanceCounter;
	}

	public int getLogIndex() {
		return logIndex;
	}

	public void setLogIndex(int logIndex) {
		this.logIndex = logIndex;
	}
	
	public void incrementLogAndInstanceCounters(){
		this.logIndex = this.instanceCounter++;
	}
	
	public void incrementLogIndex(){
		this.logIndex++;
	}
	
	public void decrementLogIndex(){
		this.logIndex--;
	}

	public int getLanguage() {
		return language;
	}

	public void setLanguage(int language) {
		this.language = language;
	}

	public Locale getLocale() {
		return locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}
}
