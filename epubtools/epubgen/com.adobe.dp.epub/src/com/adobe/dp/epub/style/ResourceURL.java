package com.adobe.dp.epub.style;

import com.adobe.dp.css.CSSURL;
import com.adobe.dp.epub.opf.Resource;

public class ResourceURL extends CSSURL {

	Stylesheet owner;

	Resource target;

	public ResourceURL(Stylesheet owner, Resource target) {
		this.owner = owner;
		this.target = target;
	}

	public String getURI() {
		return owner.owner.makeReference(target, null);
	}

}
