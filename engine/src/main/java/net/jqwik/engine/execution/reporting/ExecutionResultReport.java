package net.jqwik.engine.execution.reporting;

import java.lang.reflect.*;
import java.util.*;

import net.jqwik.api.parameters.ParameterSet;
import org.opentest4j.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.engine.descriptor.*;
import net.jqwik.engine.execution.lifecycle.*;
import net.jqwik.engine.support.*;

public class ExecutionResultReport {

	private static final String TRIES_KEY = "tries";
	private static final String CHECKS_KEY = "checks";
	private static final String GENERATION_KEY = "generation";
	private static final String EDGE_CASES_MODE_KEY = "edge-cases#mode";
	private static final String EDGE_CASES_TOTAL_KEY = "edge-cases#total";
	private static final String EDGE_CASES_TRIED_KEY = "edge-cases#tried";
	private static final String AFTER_FAILURE_KEY = "after-failure";
	private static final String FIXED_SEED_KEY = "when-fixed-seed";
	private static final String SEED_KEY = "seed";
	private static final String SAMPLE_HEADLINE = "Sample";
	private static final String SHRUNK_SAMPLE_HEADLINE = "Shrunk Sample";
	private static final String ORIGINAL_SAMPLE_HEADLINE = "Original Sample";

	public static String from(
		PropertyMethodDescriptor methodDescriptor,
		ExtendedPropertyExecutionResult executionResult,
		Collection<SampleReportingFormat> reportingFormats
	) {
		return buildJqwikReport(
			methodDescriptor.getConfiguration().getAfterFailureMode(),
			methodDescriptor.getConfiguration().getFixedSeedMode(),
			methodDescriptor.getTargetMethod(),
			executionResult,
			reportingFormats
		);
	}

	private static String buildJqwikReport(
		AfterFailureMode afterFailureMode,
		FixedSeedMode fixedSeedMode,
		Method propertyMethod,
		ExtendedPropertyExecutionResult executionResult,
		Collection<SampleReportingFormat> sampleReportingFormats
	) {
		StringBuilder reportBuilder = new StringBuilder();

		appendThrowableMessage(reportBuilder, executionResult);
		appendFixedSizedProperties(reportBuilder, executionResult, afterFailureMode, fixedSeedMode);
		appendSamples(reportBuilder, propertyMethod, executionResult, sampleReportingFormats);

		return reportBuilder.toString();
	}

	private static void appendSamples(
		StringBuilder reportBuilder,
		Method propertyMethod,
		PropertyExecutionResult executionResult,
		Collection<SampleReportingFormat> sampleReportingFormats
	) {
		executionResult.shrunkSample().ifPresent(shrunkSample -> {
			ParameterSet<Object> parameters = shrunkSample.shrinkables().map(Shrinkable::value);
			ParameterSet<Object> parametersAfterRun = shrunkSample.parameters();
			if (!parameters.isEmpty()) {
				String shrunkSampleHeadline = String.format("%s (%s steps)", SHRUNK_SAMPLE_HEADLINE, shrunkSample.countShrinkingSteps());
				SampleReporter.reportSample(reportBuilder, propertyMethod, parameters, shrunkSampleHeadline, 0, sampleReportingFormats);
				reportParameterChanges(reportBuilder, propertyMethod, parameters, parametersAfterRun, sampleReportingFormats);
			}
			reportFootnotes(reportBuilder, shrunkSample.footnotes());
		});

		executionResult.originalSample().ifPresent(originalSample -> {
			String originalSampleHeadline = executionResult.shrunkSample().isPresent() ? ORIGINAL_SAMPLE_HEADLINE : SAMPLE_HEADLINE;
			ParameterSet<Object> parameters = originalSample.shrinkables().map(Shrinkable::value);
			ParameterSet<Object> parametersAfterRun = originalSample.parameters();
			if (!parameters.isEmpty()) {
				SampleReporter.reportSample(reportBuilder, propertyMethod, parameters, originalSampleHeadline, 0, sampleReportingFormats);
				reportParameterChanges(reportBuilder, propertyMethod, parameters, parametersAfterRun, sampleReportingFormats);
			}
			reportFootnotes(reportBuilder, originalSample.footnotes());
			if (!parameters.isEmpty() && executionResult.shrunkSample().isPresent()) {
				originalSample.falsifyingError().ifPresent(error -> {
					appendOriginalError(reportBuilder, error);
				});
			}
		});
	}

	private static void reportFootnotes(StringBuilder reportBuilder, List<String> footnotes) {
		if (footnotes.isEmpty()) {
			return;
		}
		for (int i = 0; i < footnotes.size(); i++) {
			String footnote = footnotes.get(i);
			reportBuilder.append(String.format("%n  #%s %s", i + 1, footnote));
		}
		reportBuilder.append(String.format("%n"));
	}

	private static void reportParameterChanges(
		StringBuilder reportBuilder,
		Method propertyMethod,
		ParameterSet<Object> parameters,
		ParameterSet<Object> parametersAfterRun,
		Collection<SampleReportingFormat> sampleReportingFormats
	) {
		if (!ParameterChangesDetector.haveParametersChanged(parameters, parametersAfterRun)) {
			return;
		}
		if (!hasDifferentOutput(propertyMethod, parameters, parametersAfterRun, sampleReportingFormats)) {
			return;
		}
		SampleReporter.reportSample(
			reportBuilder,
			propertyMethod,
			parametersAfterRun,
			"After Execution", 1,
			sampleReportingFormats
		);
	}

	private static boolean hasDifferentOutput(
		Method propertyMethod,
		ParameterSet<Object> parameters,
		ParameterSet<Object> parametersAfterRun,
		Collection<SampleReportingFormat> sampleReportingFormats
	) {
		StringBuilder beforeBuilder = new StringBuilder();
		SampleReporter.reportSample(beforeBuilder, propertyMethod, parameters, "", 0, sampleReportingFormats);
		String beforeString = beforeBuilder.toString();
		StringBuilder afterBuilder = new StringBuilder();
		SampleReporter.reportSample(afterBuilder, propertyMethod, parametersAfterRun, "", 0, sampleReportingFormats);
		String afterString = afterBuilder.toString();
		boolean outputDifference = !beforeString.equals(afterString);
		return outputDifference;
	}

	private static void appendOriginalError(StringBuilder reportLines, Throwable error) {
		reportLines.append(String.format("%n  Original Error%n  --------------"));
		appendThrowable(reportLines, error);
	}

	private static void appendFixedSizedProperties(
		StringBuilder reportBuilder,
		ExtendedPropertyExecutionResult executionResult,
		AfterFailureMode afterFailureMode,
		FixedSeedMode fixedSeedMode
	) {
		List<String> propertiesLines = new ArrayList<>();
		int countTries = 0;
		int countChecks = 0;
		String generationMode = "<none>";
		String edgeCasesMode = "<none>";
		String randomSeed = "<none>";
		String helpGenerationMode = "";
		String helpEdgeCasesMode = "";

		if (executionResult.isExtended()) {
			countTries = executionResult.countTries();
			countChecks = executionResult.countChecks();
			generationMode = executionResult.generation().name();
			edgeCasesMode = executionResult.edgeCases().mode().name();
			randomSeed = executionResult.seed().orElse("");
			helpGenerationMode = helpGenerationMode(executionResult.generation());
			helpEdgeCasesMode = helpEdgeCasesMode(executionResult.edgeCases().mode());
		}

		appendProperty(propertiesLines, TRIES_KEY, Integer.toString(countTries), "# of calls to property");
		appendProperty(propertiesLines, CHECKS_KEY, Integer.toString(countChecks), "# of not rejected calls");
		appendProperty(propertiesLines, GENERATION_KEY, generationMode, helpGenerationMode);
		if (afterFailureMode != AfterFailureMode.NOT_SET) {
			appendProperty(propertiesLines, AFTER_FAILURE_KEY, afterFailureMode.name(), helpAfterFailureMode(afterFailureMode));
		}
		if (fixedSeedMode != FixedSeedMode.NOT_SET) {
			appendProperty(propertiesLines, FIXED_SEED_KEY, fixedSeedMode.name(), helpFixedSeedMode(fixedSeedMode));
		}
		appendProperty(propertiesLines, EDGE_CASES_MODE_KEY, edgeCasesMode, helpEdgeCasesMode);
		if (executionResult.edgeCases().mode().activated()) {
			appendProperty(propertiesLines, EDGE_CASES_TOTAL_KEY, executionResult.edgeCases().total(), "# of all combined edge cases");
			appendProperty(propertiesLines, EDGE_CASES_TRIED_KEY, executionResult.edgeCases()
																				 .tried(), "# of edge cases tried in current run");
		}
		appendProperty(propertiesLines, SEED_KEY, randomSeed, "random seed to reproduce generated values");

		prependFixedSizedPropertiesHeader(reportBuilder, propertiesLines);
		propertiesLines.forEach(reportBuilder::append);

	}

	private static void prependFixedSizedPropertiesHeader(StringBuilder reportBuilder, List<String> propertiesLines) {
		int halfBorderLength =
			(propertiesLines.stream().mapToInt(String::length).max().orElse(50) - 37) / 2 + 1;
		String halfBorder = String.join("", Collections.nCopies(halfBorderLength, "-"));

		reportBuilder.append(String.format("%n"));
		reportBuilder.append(buildLine("", "|" + halfBorder + "jqwik" + halfBorder));
	}

	private static void appendThrowableMessage(StringBuilder reportBuilder, ExtendedPropertyExecutionResult executionResult) {
		if (executionResult.status() != PropertyExecutionResult.Status.SUCCESSFUL) {
			Throwable throwable = executionResult.throwable().orElse(new AssertionFailedError(null));
			appendThrowable(reportBuilder, throwable);
		}
	}

	private static void appendThrowable(StringBuilder reportBuilder, Throwable throwable) {
		String assertionClass = throwable.getClass().getName();
		reportBuilder.append(String.format("%n  %s", assertionClass));
		List<String> assertionMessageLines = JqwikStringSupport.toLines(throwable.getMessage());
		if (!assertionMessageLines.isEmpty()) {
			reportBuilder.append(":");
			for (String line : assertionMessageLines) {
				reportBuilder.append(String.format("%n    %s", line));
			}
		}
		reportBuilder.append(String.format("%n"));
	}

	private static String helpAfterFailureMode(AfterFailureMode afterFailureMode) {
		switch (afterFailureMode) {
			case RANDOM_SEED:
				return "use a new random seed";
			case PREVIOUS_SEED:
				return "use the previous seed";
			case SAMPLE_ONLY:
				return "only try the previously failed sample";
			case SAMPLE_FIRST:
				return "try previously failed sample, then previous seed";
			default:
				return "RANDOM_SEED, PREVIOUS_SEED or SAMPLE_FIRST";
		}
	}

	private static String helpFixedSeedMode(FixedSeedMode fixedSeedMode) {
		switch (fixedSeedMode) {
			case ALLOW:
				return "fixing the random seed is allowed";
			case FAIL:
				return "fail when fixed random seed";
			case WARN:
				return "warn when fixed random seed";
			default:
				return "ALLOW, FAIL or WARN";
		}
	}

	private static String helpGenerationMode(GenerationMode generation) {
		switch (generation) {
			case RANDOMIZED:
				return "parameters are randomly generated";
			case EXHAUSTIVE:
				return "parameters are exhaustively generated";
			case DATA_DRIVEN:
				return "parameters are taken from data provider";
			default:
				return "RANDOMIZED, EXHAUSTIVE or DATA_DRIVEN";
		}
	}

	private static String helpEdgeCasesMode(EdgeCasesMode edgeCases) {
		switch (edgeCases) {
			case FIRST:
				return "edge cases are generated first";
			case MIXIN:
				return "edge cases are mixed in";
			case NONE:
				return "edge cases are not explicitly generated";
			default:
				return "FIRST, MIXIN or NONE";
		}
	}

	private static void appendProperty(List<String> propertiesLines, String triesKey, Object value, String comment) {
		propertiesLines.add(buildPropertyLine(triesKey, value.toString(), comment));
	}

	private static String buildPropertyLine(String key, String value, String help) {
		return buildLine(buildProperty(key, value), String.format("| %s", help));
	}

	private static String buildProperty(String key, String value) {
		return String.format("%s = %s", key, value);
	}

	private static String buildLine(String body, String helpString) {
		return String.format("%-30s%s%n", body, helpString);
	}

}
