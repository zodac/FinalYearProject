package mehdis.Entities;

public class Boundary {
	
	private int north;
	private int south;
	private int east;
	private int west;
	
	public Boundary(){
		this.north = 0;
		this.south = 0;
		this.east = 0;
		this.west = 0;
	}
	
	public Boundary(int north, int south, int east, int west){
		this.north = north;
		this.south = south;
		this.east = east;
		this.west = west;
	}

	public int getNorth() {
		return north;
	}

	public void setNorth(int north) {
		this.north = north;
	}

	public int getSouth() {
		return south;
	}

	public void setSouth(int south) {
		this.south = south;
	}

	public int getEast() {
		return east;
	}

	public void setEast(int east) {
		this.east = east;
	}

	public int getWest() {
		return west;
	}

	public void setWest(int west) {
		this.west = west;
	}
}