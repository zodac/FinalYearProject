package mehdis.KeyAnalyser;

import mehdis.Entities.AnalysisData;

public class ImageProcesser extends Thread {
	private AnalysisData analysisData;
	
	public ImageProcesser(AnalysisData analysisData){
		this.analysisData = analysisData;
	}
	
	public void run(){
		
	}
	
	//Only adding get/set to get rid of warnings - can be removed later
	public AnalysisData getAnalysisData() {
		return analysisData;
	}

	public void setAnalysisData(AnalysisData analysisData) {
		this.analysisData = analysisData;
	}
}
