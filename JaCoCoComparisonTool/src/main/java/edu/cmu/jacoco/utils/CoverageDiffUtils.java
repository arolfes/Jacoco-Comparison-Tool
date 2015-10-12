package edu.cmu.jacoco.utils;

public final class CoverageDiffUtils {

    private static final String COMMA_SEPARATOR = ",";

    private CoverageDiffUtils() {
	// empty priv. default cosntructor for utils class
    }

    public static String[] getSplittedString(final String value2split) {
	return getSplittedString(value2split, COMMA_SEPARATOR);
    }

    public static String[] getSplittedString(final String value2split, final String separator) {
	if (value2split != null && !value2split.trim().isEmpty()) {
	    return value2split.split(separator);
	} else {
	    return new String[0];
	}
    }

    public static String[] wrapTitles(final String[] optionValues, final int numberOfExecFiles) {

	int givenTitles = optionValues.length;

	if (givenTitles == numberOfExecFiles)
	    return optionValues;

	final String[] wrapped = new String[numberOfExecFiles];

	System.arraycopy(optionValues, 0, wrapped, 0, givenTitles);

	for (int counter = givenTitles; counter < numberOfExecFiles; counter++) {
	    wrapped[counter] = "Test Suite " + (counter + 1);
	}

	return wrapped;

    }

}
