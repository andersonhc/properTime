package nl.xupwup.properTime;

public enum Preset {

	ALWAYSDAY (1 , 1 , 1 , 1 , 6000),
	ALWAYSNIGHT (1 , 1 , 1 , 1 , 18000),
	CYCLE24H (0.0118343 , 0.0118243 , 1 , 1 , -1),
	CYCLE24M ( 0.81 , 0.81 , 1 , 1 , -1),
	DEFAULT (1 , 1 , 1 , 1 , -1);
	
	private final double factorday;
	private final double factornight;
	private final double factordawn;
	private final double factordusk;
	private final int perma;
	
	Preset (double factorday, double factornight, double factordawn, double factordusk, int perma) {
		this.factorday = factorday;
		this.factornight = factornight;
		this.factordawn = factordawn;
		this.factordusk = factordusk;
		this.perma = perma;
	}
	
	public double getFactorday() {
		return factorday;
	}

	public double getFactornight() {
		return factornight;
	}

	public double getFactordawn() {
		return factordawn;
	}

	public double getFactordusk() {
		return factordusk;
	}

	public int getPerma() {
		return perma;
	}

}
