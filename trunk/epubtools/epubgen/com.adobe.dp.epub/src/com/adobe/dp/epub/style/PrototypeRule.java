package com.adobe.dp.epub.style;

import java.io.PrintWriter;

public class PrototypeRule extends BaseRule {

	public PrototypeRule() {
	}
	
	public void serialize(PrintWriter out) {
		throw new RuntimeException("should not be called");
	}
}
