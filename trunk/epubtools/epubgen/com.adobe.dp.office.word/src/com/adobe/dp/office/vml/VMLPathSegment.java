package com.adobe.dp.office.vml;

import java.util.Vector;

public class VMLPathSegment {

	final public String command;

	final public Object[] args;

	private final static int INIT = 0;

	private final static int CALLOUT = 1;

	private final static int ARG = 2;

	private final static int COMMAND = 3;

	private final static int COMMA = 4;

	public VMLPathSegment(String command, Object[] args) {
		this.command = command;
		this.args = args;
	}

	public static VMLPathSegment[] parse(String path) {
		if (path == null)
			return null;
		Vector segments = new Vector();
		Vector args = new Vector();
		StringBuffer command = new StringBuffer();
		int index = 0;
		int len = path.length();
		int state = INIT;
		while (index <= len) {
			char c = (index == len ? 'e' : path.charAt(index));
			if (('0' <= c && c <= '9') || c == '-') {
				// read numerical arg
				int start = index;
				while (index < len) {
					c = path.charAt(++index);
					if ('0' > c || c > '9')
						break;
				}
				int arg = Integer.parseInt(path.substring(start, index));
				if (state == CALLOUT)
					args.add(new VMLCallout('@', arg));
				else
					args.add(new Integer(arg));
				state = ARG;
			} else if (c == ',') {
				index++;
				if (state == COMMA || state == COMMAND)
					args.add(new Integer(0));
				state = COMMA;
			} else if ('a' <= c && c <= 'z') {
				if (command.length() > 0) {
					char f = command.charAt(0);
					if (command.length() == 2 || (f != 'h' && f != 'n' && f != 'a' && f != 'w' && f != 'q')) {
						String cmd = command.toString();
						if (state == COMMA)
							args.add(new Integer(0));
						Object[] cmdargs = new Object[args.size()];
						args.copyInto(cmdargs);
						segments.add(new VMLPathSegment(cmd, cmdargs));
						args.setSize(0);
						command.delete(0, command.length());
					}
				}
				command.append(c);
				state = COMMAND;
				index++;
			} else if (c == '@') {
				state = CALLOUT;
				index++;
			} else if (c == ' ') {
				index++;
			} else {
				index++;
				System.out.println("unknown char in path string: " + c);
			}
		}
		VMLPathSegment[] segs = new VMLPathSegment[segments.size()];
		segments.copyInto(segs);
		return segs;
	}
}
