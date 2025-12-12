/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.config;

import io.airlift.formatter.BraceStyle;
import io.airlift.formatter.FormatterConfiguration;
import io.airlift.formatter.ImportGroup;
import io.airlift.formatter.ImportOrder;
import io.airlift.formatter.WrapMode;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parses IntelliJ IDEA code style XML files into {@link FormatterConfiguration}.
 *
 * <p>Supports the standard IntelliJ code style XML format with three sections:
 * <ul>
 *   <li>Root-level {@code <option>} elements for global settings</li>
 *   <li>{@code <JavaCodeStyleSettings>} for Java-specific settings like import layout</li>
 *   <li>{@code <codeStyleSettings language="JAVA">} for Java formatting options</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * IntelliJConfigParser parser = new IntelliJConfigParser();
 * FormatterConfiguration config = parser.parse(Paths.get("Airlift.xml"));
 * AirliftFormatter formatter = new AirliftFormatter(config);
 * }</pre>
 */
public class IntelliJConfigParser
{
    /**
     * Parses an IntelliJ code style XML file.
     *
     * @param xmlPath path to the XML file
     * @return the parsed formatter configuration
     * @throws IOException if reading the file fails
     * @throws IntelliJConfigParseException if parsing the XML fails
     */
    public FormatterConfiguration parse(Path xmlPath)
            throws IOException
    {
        try (InputStream is = Files.newInputStream(xmlPath)) {
            return parse(is);
        }
    }

    /**
     * Parses an IntelliJ code style XML from an input stream.
     *
     * @param inputStream the XML input stream
     * @return the parsed formatter configuration
     * @throws IntelliJConfigParseException if parsing fails
     */
    public FormatterConfiguration parse(InputStream inputStream)
    {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Disable external entities for security
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            return parseDocument(doc);
        }
        catch (ParserConfigurationException | SAXException | IOException e) {
            throw new IntelliJConfigParseException("Failed to parse IntelliJ config XML", e);
        }
    }

    private FormatterConfiguration parseDocument(Document doc)
    {
        Element root = doc.getDocumentElement();
        FormatterConfiguration.Builder configBuilder = FormatterConfiguration.builder();

        // Parse root-level options as fallback
        Map<String, String> rootOptions = parseOptions(root);

        // Parse <codeStyleSettings language="JAVA"> section (primary)
        Element javaSettings = findCodeStyleSettings(root, "JAVA");
        Map<String, String> javaOptions = javaSettings != null ? parseOptions(javaSettings) : new HashMap<>();

        // Merge with root options (Java settings take precedence)
        Map<String, String> allOptions = new HashMap<>(rootOptions);
        allOptions.putAll(javaOptions);

        // Apply parsed options to configuration builder
        applyBraceStyles(configBuilder, allOptions);
        applyControlFlowOptions(configBuilder, allOptions);
        applyIndentationOptions(configBuilder, allOptions);
        applyWrappingOptions(configBuilder, allOptions);
        applyBlankLineOptions(configBuilder, allOptions);
        applyKeepOnOneLineOptions(configBuilder, allOptions);

        // Parse <JavaCodeStyleSettings> for import layout
        Element javaCodeStyleSettings = findElement(root, "JavaCodeStyleSettings");
        if (javaCodeStyleSettings != null) {
            ImportOrder importOrder = parseImportLayout(javaCodeStyleSettings);
            if (importOrder != null) {
                configBuilder.importOrder(importOrder);
            }
        }

        return configBuilder.build();
    }

    private Map<String, String> parseOptions(Element parent)
    {
        Map<String, String> options = new HashMap<>();
        NodeList optionNodes = parent.getChildNodes();

        for (int i = 0; i < optionNodes.getLength(); i++) {
            if (optionNodes.item(i) instanceof Element element) {
                if ("option".equals(element.getTagName())) {
                    String name = element.getAttribute("name");
                    String value = element.getAttribute("value");
                    if (!name.isEmpty()) {
                        options.put(name, value);
                    }
                }
            }
        }
        return options;
    }

    private Element findCodeStyleSettings(Element root, String language)
    {
        NodeList nodes = root.getElementsByTagName("codeStyleSettings");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            if (language.equals(element.getAttribute("language"))) {
                return element;
            }
        }
        return null;
    }

    private Element findElement(Element root, String tagName)
    {
        NodeList nodes = root.getElementsByTagName(tagName);
        return nodes.getLength() > 0 ? (Element) nodes.item(0) : null;
    }

    private void applyBraceStyles(FormatterConfiguration.Builder builder, Map<String, String> options)
    {
        // Class brace style
        if (options.containsKey("CLASS_BRACE_STYLE")) {
            builder.classBraceStyle(BraceStyle.fromIntellijValue(
                    Integer.parseInt(options.get("CLASS_BRACE_STYLE"))));
        }

        // Method brace style
        if (options.containsKey("METHOD_BRACE_STYLE")) {
            builder.methodBraceStyle(BraceStyle.fromIntellijValue(
                    Integer.parseInt(options.get("METHOD_BRACE_STYLE"))));
        }

        // Lambda brace style
        if (options.containsKey("LAMBDA_BRACE_STYLE")) {
            builder.lambdaBraceStyle(BraceStyle.fromIntellijValue(
                    Integer.parseInt(options.get("LAMBDA_BRACE_STYLE"))));
        }
    }

    private void applyControlFlowOptions(FormatterConfiguration.Builder builder, Map<String, String> options)
    {
        if (options.containsKey("ELSE_ON_NEW_LINE")) {
            builder.elseOnNewLine(Boolean.parseBoolean(options.get("ELSE_ON_NEW_LINE")));
        }

        if (options.containsKey("CATCH_ON_NEW_LINE")) {
            builder.catchOnNewLine(Boolean.parseBoolean(options.get("CATCH_ON_NEW_LINE")));
        }

        if (options.containsKey("FINALLY_ON_NEW_LINE")) {
            builder.finallyOnNewLine(Boolean.parseBoolean(options.get("FINALLY_ON_NEW_LINE")));
        }

        // Note: IntelliJ's WHILE_ON_NEW_LINE=true means while on new line
        // Our whileOnSameLine is the inverse
        if (options.containsKey("WHILE_ON_NEW_LINE")) {
            builder.whileOnSameLine(!Boolean.parseBoolean(options.get("WHILE_ON_NEW_LINE")));
        }
    }

    private void applyIndentationOptions(FormatterConfiguration.Builder builder, Map<String, String> options)
    {
        if (options.containsKey("TAB_SIZE")) {
            builder.tabSize(Integer.parseInt(options.get("TAB_SIZE")));
        }

        if (options.containsKey("INDENT_SIZE")) {
            builder.indentationSize(Integer.parseInt(options.get("INDENT_SIZE")));
        }

        if (options.containsKey("CONTINUATION_INDENT_SIZE")) {
            // IntelliJ stores actual spaces, we store units (divide by indent size)
            int continuationSpaces = Integer.parseInt(options.get("CONTINUATION_INDENT_SIZE"));
            int indentSize = options.containsKey("INDENT_SIZE")
                    ? Integer.parseInt(options.get("INDENT_SIZE"))
                    : 4;
            builder.continuationIndent(continuationSpaces / indentSize);
        }
    }

    private void applyWrappingOptions(FormatterConfiguration.Builder builder, Map<String, String> options)
    {
        if (options.containsKey("RIGHT_MARGIN")) {
            builder.lineWidth(Integer.parseInt(options.get("RIGHT_MARGIN")));
        }

        // Method parameters wrapping
        if (options.containsKey("METHOD_PARAMETERS_WRAP")) {
            builder.methodParametersWrap(WrapMode.fromIntellijValue(
                    Integer.parseInt(options.get("METHOD_PARAMETERS_WRAP"))));
        }

        // Call parameters wrapping
        if (options.containsKey("CALL_PARAMETERS_WRAP")) {
            builder.methodArgumentsWrap(WrapMode.fromIntellijValue(
                    Integer.parseInt(options.get("CALL_PARAMETERS_WRAP"))));
        }

        // Method call chain wrapping
        if (options.containsKey("METHOD_CALL_CHAIN_WRAP")) {
            builder.methodChainWrap(WrapMode.fromIntellijValue(
                    Integer.parseInt(options.get("METHOD_CALL_CHAIN_WRAP"))));
        }

        // Ternary operation wrapping
        if (options.containsKey("TERNARY_OPERATION_WRAP")) {
            builder.ternaryWrap(WrapMode.fromIntellijValue(
                    Integer.parseInt(options.get("TERNARY_OPERATION_WRAP"))));
        }

        // Extends list wrapping
        if (options.containsKey("EXTENDS_LIST_WRAP")) {
            builder.extendsListWrap(WrapMode.fromIntellijValue(
                    Integer.parseInt(options.get("EXTENDS_LIST_WRAP"))));
        }

        // Throws list wrapping
        if (options.containsKey("THROWS_LIST_WRAP")) {
            builder.throwsListWrap(WrapMode.fromIntellijValue(
                    Integer.parseInt(options.get("THROWS_LIST_WRAP"))));
        }

        // Array initializer wrapping
        if (options.containsKey("ARRAY_INITIALIZER_WRAP")) {
            builder.arrayInitializerWrap(WrapMode.fromIntellijValue(
                    Integer.parseInt(options.get("ARRAY_INITIALIZER_WRAP"))));
        }
    }

    private void applyBlankLineOptions(FormatterConfiguration.Builder builder, Map<String, String> options)
    {
        if (options.containsKey("KEEP_BLANK_LINES_IN_DECLARATIONS")) {
            builder.blankLinesInDeclarations(Integer.parseInt(options.get("KEEP_BLANK_LINES_IN_DECLARATIONS")));
        }

        if (options.containsKey("KEEP_BLANK_LINES_IN_CODE")) {
            builder.blankLinesInCode(Integer.parseInt(options.get("KEEP_BLANK_LINES_IN_CODE")));
        }

        if (options.containsKey("KEEP_BLANK_LINES_BEFORE_RBRACE")) {
            builder.blankLinesBeforeClosingBrace(Integer.parseInt(options.get("KEEP_BLANK_LINES_BEFORE_RBRACE")));
        }
    }

    private void applyKeepOnOneLineOptions(FormatterConfiguration.Builder builder, Map<String, String> options)
    {
        if (options.containsKey("KEEP_SIMPLE_METHODS_IN_ONE_LINE")) {
            builder.keepSimpleMethodsOnOneLine(Boolean.parseBoolean(options.get("KEEP_SIMPLE_METHODS_IN_ONE_LINE")));
        }

        if (options.containsKey("KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE")) {
            builder.keepSimpleLambdasOnOneLine(Boolean.parseBoolean(options.get("KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE")));
        }

        if (options.containsKey("KEEP_SIMPLE_CLASSES_IN_ONE_LINE")) {
            builder.keepSimpleClassesOnOneLine(Boolean.parseBoolean(options.get("KEEP_SIMPLE_CLASSES_IN_ONE_LINE")));
        }
    }

    private ImportOrder parseImportLayout(Element javaCodeStyleSettings)
    {
        // Find the IMPORT_LAYOUT_TABLE option
        NodeList optionNodes = javaCodeStyleSettings.getElementsByTagName("option");
        Element importLayoutOption = null;

        for (int i = 0; i < optionNodes.getLength(); i++) {
            Element option = (Element) optionNodes.item(i);
            if ("IMPORT_LAYOUT_TABLE".equals(option.getAttribute("name"))) {
                importLayoutOption = option;
                break;
            }
        }

        if (importLayoutOption == null) {
            return null;
        }

        // Find the <value> element
        NodeList valueNodes = importLayoutOption.getElementsByTagName("value");
        if (valueNodes.getLength() == 0) {
            return null;
        }

        Element valueElement = (Element) valueNodes.item(0);
        NodeList packageNodes = valueElement.getElementsByTagName("package");

        List<ImportGroup> groups = new ArrayList<>();
        for (int i = 0; i < packageNodes.getLength(); i++) {
            Element pkg = (Element) packageNodes.item(i);
            String name = pkg.getAttribute("name");
            boolean isStatic = "true".equals(pkg.getAttribute("static"));
            groups.add(new ImportGroup(name, isStatic));
        }

        return groups.isEmpty() ? null : new ImportOrder(groups);
    }

    /**
     * Exception thrown when parsing an IntelliJ config file fails.
     */
    public static class IntelliJConfigParseException
            extends RuntimeException
    {
        public IntelliJConfigParseException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}
