package mehdis.Entities;

public class Settings {
	private int numOfPasses;
	private int instanceCounter;
	private int logPointer;
	
	private static Settings instance = null;
	
	protected Settings(){
		this.numOfPasses = 1;
		this.instanceCounter = 0;
		this.logPointer = 0;
	}
	
	public static Settings getDefaultSettings(){
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

	public int getLogPointer() {
		return logPointer;
	}

	public void setLogPointer(int logPointer) {
		this.logPointer = logPointer;
	}
	
	public void incrementLogAndInstanceCounters(){
		this.logPointer = this.instanceCounter++;
	}
	
	public void incrementLogPointer(){
		this.logPointer++;
	}
	
	public void decrementLogPointer(){
		this.logPointer--;
	}
}
