package mehdis.KeyAnalyser;

//Class containing info for each key in DB
public class Key {
	
	public String name;
	public double length;
	public int degree;
	
	//Constructor
	public Key(String _name, double _length, int _degree)
	{
		name = _name;
		length = _length;
		degree = _degree;
	}
	
	//Constructor for default ResultStorage file (in case of no match)
	public Key()
	{
		name = "No model found";
		length = 0;
		degree = 0; 
	}
}
