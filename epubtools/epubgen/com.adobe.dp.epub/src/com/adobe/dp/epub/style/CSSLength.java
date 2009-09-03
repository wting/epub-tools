package com.adobe.dp.epub.style;

public class CSSLength {

	double value;
	String unit;

	public CSSLength(double value, String unit) {
		this.value = value;
		this.unit = unit;
	}

	public double getValue() {
		return value;
	}

	public String getUnit() {
		return unit;
	}
	
	public String toString() {
		return Math.round(value*1000)/1000.0 + unit;
	}
	
	public int hashCode() {
		return (int)(value*1000) + unit.hashCode();
	}
	
	public boolean equals(Object other) {
		if( other.getClass() == getClass() ) {
			CSSLength o = (CSSLength)other;
			return o.value == value && o.unit.equals(unit);
		}
		return false;
	}
}
