package com.adobe.dp.office.conv;

import com.adobe.dp.epub.style.PrototypeRule;

public class StylingResult {

	PrototypeRule containerRule;

	PrototypeRule elementRule = new PrototypeRule();

	String containerClassName;

	String elementClassName;

	String elementName;

	PrototypeRule tableCellRule;
}
