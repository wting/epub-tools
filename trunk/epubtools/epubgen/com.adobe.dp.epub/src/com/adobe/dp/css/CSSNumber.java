package com.adobe.dp.css;

import java.io.PrintWriter;

public class CSSNumber extends CSSValue {

	private Number number;

	CSSNumber(Number number) {
		this.number = number;
	}

	public Number getNumber() {
		return number;
	}

	public void serialize(PrintWriter out) {
		out.print(number);
	}

	public String toString() {
		return number.toString();
	}

	public boolean equals(Object other) {
		if (other.getClass() != getClass())
			return false;
		return ((CSSNumber) other).number.equals(number);
	}

	public int hashCode() {
		return number.hashCode();
	}
}
