package com.adobe.dp.office.vml;

import java.util.StringTokenizer;

import org.xml.sax.Attributes;

public class VMLFElement extends VMLElement {

	String command;

	Object[] args;

	public VMLFElement(Attributes attr) {
		super(attr);
		String eqn = attr.getValue("eqn");
		StringTokenizer tok = new StringTokenizer(eqn, " ");
		int len = tok.countTokens();
		command = tok.nextToken();
		args = new Object[len - 1];
		for (int i = 1; i < len; i++) {
			String t = tok.nextToken();
			char c = t.charAt(0);
			if (c == '@' || c == '#') {
				int v = Integer.parseInt(t.substring(1));
				args[i - 1] = new VMLCallout(t.charAt(0), v);
			} else if ('0' <= c && c <= '9') {
				int v = Integer.parseInt(t);
				args[i - 1] = new Integer(v);
			} else if ('a' <= c && c <= 'z') {
				args[i - 1] = t;
			} else {
				throw new RuntimeException("Unknown stuff: " + t);
			}
		}
	}

	private static int resolve(VMLEnv env, Object arg) {
		if (arg instanceof Integer)
			return ((Integer) arg).intValue();
		if (arg instanceof VMLCallout)
			return env.resolveCallout((VMLCallout) arg);
		if (arg instanceof String)
			return env.resolveEnv((String) arg);
		throw new RuntimeException("Unknown stuff: " + arg);
	}

	public int eval(VMLEnv env) {
		if (command.equals("val")) {
			return resolve(env, args[0]);
		} else if (command.equals("sum")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return v + p1 - p2;
		} else if (command.equals("prod")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return (int) Math.round(v * p1 / (double) p2);
		} else if (command.equals("mid")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (v + p1) / 2;
		} else if (command.equals("abs")) {
			int v = resolve(env, args[0]);
			return v > 0 ? v : -v;
		} else if (command.equals("min")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return v > p1 ? p1 : v;
		} else if (command.equals("max")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return v > p1 ? v : p1;
		} else if (command.equals("if")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return v > 0 ? p1 : p2;
		} else if (command.equals("mod")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return (int) Math.floor(Math.sqrt(v * (double) v + p1 * (double) p1 + p2 * (double) p2));
		} else if (command.equals("atan2")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (int) Math.floor(0x10000 * Math.toDegrees(Math.atan2(p1, v)));
		} else if (command.equals("sin")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (int) Math.floor(v * Math.sin(Math.toRadians(p1 / (double) 0x10000)));
		} else if (command.equals("cos")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (int) Math.floor(v * Math.cos(Math.toRadians(p1 / (double) 0x10000)));
		} else if (command.equals("cosatan2")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return (int) Math.floor(v * Math.cos(Math.atan2(p2, p1)));
		} else if (command.equals("sinatan2")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return (int) Math.floor(v * Math.sin(Math.atan2(p2, p1)));
		} else if (command.equals("sqrt")) {
			int v = resolve(env, args[0]);
			return (int) Math.floor(Math.sqrt(v));
		} else if (command.equals("sumangle")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			return v + p1 * 0x10000 - p2 * 0x10000;
		} else if (command.equals("ellipse")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			int p2 = resolve(env, args[2]);
			double r = v / (double) p1;
			return (int) Math.floor(p2 * Math.sqrt(1 - r * r));
		} else if (command.equals("tan")) {
			int v = resolve(env, args[0]);
			int p1 = resolve(env, args[1]);
			return (int) Math.floor(v * Math.tan(Math.toRadians(p1 / (double) 0x10000)));
		} else {
			throw new RuntimeException("unknown command: " + command);
		}
	}
}
