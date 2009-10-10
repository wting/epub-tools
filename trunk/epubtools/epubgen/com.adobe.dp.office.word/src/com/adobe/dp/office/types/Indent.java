package com.adobe.dp.office.types;

public class Indent {

	float left;
	
	float right;
	
	float firstLine;
	
	float hanging;

	public Indent(float left, float right, float firstLine, float hanging) {
		this.left = left;
		this.right = right;
		this.firstLine = firstLine;
		this.hanging = hanging;
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
	
	public float getHanging() {
		return hanging;
	}
	
}
