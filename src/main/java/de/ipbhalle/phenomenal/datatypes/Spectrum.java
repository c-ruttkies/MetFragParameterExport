package de.ipbhalle.phenomenal.datatypes;

public class Spectrum {

	// important parameters
	private String peakString;
	private String precursorAdduct;
	private double precursorIonMass;
	private String name;
	
	public Spectrum(String peakString, String precursorAdduct, double precursorIonMass, String name) {
		this.peakString = peakString;
		this.precursorAdduct = precursorAdduct;
		this.precursorIonMass = precursorIonMass;
		this.name = name;
	}

	public String getPeakString() {
		return peakString;
	}

	public void setPeakString(String peakString) {
		this.peakString = peakString;
	}

	public String getPrecursorAdduct() {
		return precursorAdduct;
	}

	public void setPrecursorAdduct(String precursorAdduct) {
		this.precursorAdduct = precursorAdduct;
	}

	public double getPrecursorIonMass() {
		return precursorIonMass;
	}

	public void setPrecursorIonMass(double precursorIonMass) {
		this.precursorIonMass = precursorIonMass;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
