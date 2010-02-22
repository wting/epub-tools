package com.adobe.dp.css;

import java.io.PrintWriter;

public abstract class CSSValue {

	public abstract void serialize(PrintWriter out);
	
	public static void serialize(PrintWriter out, Object obj) {
		if (obj instanceof CSSValue)
			((CSSValue) obj).serialize(out);
		else
			out.print(obj.toString());
	}
}
