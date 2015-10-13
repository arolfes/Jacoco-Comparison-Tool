package edu.cmu.jacoco.utils;

import java.util.regex.Pattern;

public class WildcardMatcher {

    private final Pattern pattern;

    public WildcardMatcher(final String expression) {
	this(expression.split("\\:"));
    }

    public WildcardMatcher(final String[] parts) {

	final StringBuilder regex = new StringBuilder(calculateLenght(parts));
	boolean next = false;
	for (final String part : parts) {
	    if (next) {
		regex.append('|');
	    }
	    regex.append('(').append(toRegex(part)).append(')');
	    next = true;
	}
	pattern = Pattern.compile(regex.toString());
    }

    private int calculateLenght(final String[] parts) {
	int result = 0;
	for (int i = 0; i < parts.length; i++) {
	    result = result + parts[i].length();
	}
	return result*2;
    }

    private CharSequence toRegex(final String expression) {
	final StringBuilder regex = new StringBuilder(expression.length() * 2);
	for (final char c : expression.toCharArray()) {
	    switch (c) {
	    case '?':
		regex.append(".?");
		break;
	    case '*':
		regex.append(".*");
		break;
	    default:
		regex.append(Pattern.quote(String.valueOf(c)));
		break;
	    }
	}
	return regex;
    }

    public boolean matches(final String s) {
	return pattern.matcher(s).matches();
    }
}
