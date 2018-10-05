package com.github.spotbugs;

/**
 * Used by {@link SourceAnalysisPropertyTest}
 * as an example of code with a "SwitchFallthrough" deficiency,
 * which should be disabled through a SpotBugs analysis property.
 */
public class SourceAnalysisProperty {

    static int method(String someString) {
        int result = 5;

        switch (someString) {
            case "Hello":
                result = 10;
                // fall through

            case "Hello, World!":
                result += 4;
                // fall through

            default:
                result += 6;
                break;
        }

        return result;
    }

    public static void main(String[] args) {
        System.out.println("result = " + method("Hello, World!"));
    }
}
