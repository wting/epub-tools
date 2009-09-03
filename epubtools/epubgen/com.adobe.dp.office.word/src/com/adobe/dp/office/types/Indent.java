package com.adobe.dp.office.types;

public class Indent {

	float left;
	
	float right;
	
	float firstLine;

	public Indent(float left, float right, float firstLine) {
		this.left = left;
		this.right = right;
		this.firstLine = firstLine;
	}

	public float getLeft() {
		return left;
	}

	public float getRight() {
		return right;
	}

	public float getFirstLine() {
		return firstLine;
	}
	
	
}
