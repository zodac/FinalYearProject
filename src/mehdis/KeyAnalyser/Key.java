package mehdis.KeyAnalyser;

//Class containing info for each key in DB
public class Key {
	
	private String name;
	private double length, angle;
	
	public Key(String name, double length, double angle){
		this.name = name;
		this.length = length;
		this.angle = angle;
	}
	
	//Constructor for default ResultStorage file (in case of no match)
	public Key(){
		name = "No model found";
		length = 0;
		angle = 0; 
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getLength() {
		return length;
	}

	public void setLength(double length) {
		this.length = length;
	}

	public double getAngle() {
		return angle;
	}

	public void setAngle(double angle) {
		this.angle = angle;
	}
}
