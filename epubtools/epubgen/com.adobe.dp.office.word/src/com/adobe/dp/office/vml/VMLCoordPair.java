package com.adobe.dp.office.vml;

public class VMLCoordPair {

	public VMLCoordPair(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public static VMLCoordPair parse(String str) {
		if (str == null)
			return null;
		return parse(str, 0, 0);
	}

	static VMLCoordPair parse(String str, int x, int y) {
		if (str != null) {
			int index = str.indexOf(',');
			if (index > 0) {
				String xstr = str.substring(0, index);
				try {
					x = Integer.parseInt(xstr);
				} catch (Exception e) {
					e.printStackTrace();
				}
				String ystr = str.substring(index + 1);
				try {
					y = Integer.parseInt(ystr);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return new VMLCoordPair(x, y);
	}

	public final int x;

	public final int y;
}
