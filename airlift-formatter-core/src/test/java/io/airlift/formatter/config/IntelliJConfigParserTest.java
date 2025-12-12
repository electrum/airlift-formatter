/*
 * Copyright (C) 2024 The Airlift Authors
 * SPDX-License-Identifier: EPL-2.0
 */
package io.airlift.formatter.config;

import io.airlift.formatter.BraceStyle;
import io.airlift.formatter.FormatterConfiguration;
import io.airlift.formatter.ImportGroup;
import io.airlift.formatter.ImportOrder;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link IntelliJConfigParser}.
 */
class IntelliJConfigParserTest
{
    private final IntelliJConfigParser parser = new IntelliJConfigParser();

    @Test
    void testParseAirliftXml()
            throws IOException
    {
        Path xmlPath = Path.of("src/main/resources/Airlift.xml");
        FormatterConfiguration config = parser.parse(xmlPath);

        // Verify brace styles
        assertThat(config.getClassBraceStyle()).isEqualTo(BraceStyle.NEXT_LINE);
        assertThat(config.getMethodBraceStyle()).isEqualTo(BraceStyle.NEXT_LINE);
        assertThat(config.getLambdaBraceStyle()).isEqualTo(BraceStyle.END_OF_LINE);

        // Verify control flow
        assertThat(config.isElseOnNewLine()).isTrue();
        assertThat(config.isCatchOnNewLine()).isTrue();
        assertThat(config.isFinallyOnNewLine()).isTrue();
    }

    @Test
    void testParseAirliftXmlFromInputStream()
    {
        try (InputStream is = getClass().getResourceAsStream("/Airlift.xml")) {
            FormatterConfiguration config = parser.parse(is);

            assertThat(config.getClassBraceStyle()).isEqualTo(BraceStyle.NEXT_LINE);
            assertThat(config.isElseOnNewLine()).isTrue();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testImportOrderParsing()
            throws IOException
    {
        Path xmlPath = Path.of("src/main/resources/Airlift.xml");
        FormatterConfiguration config = parser.parse(xmlPath);

        ImportOrder order = config.getImportOrder();
        List<ImportGroup> groups = order.getGroups();

        // Airlift order: others, javax, java, static
        assertThat(groups).hasSize(4);
        assertThat(groups.get(0).prefix()).isEmpty();      // All other
        assertThat(groups.get(0).isStatic()).isFalse();
        assertThat(groups.get(1).prefix()).isEqualTo("javax");
        assertThat(groups.get(1).isStatic()).isFalse();
        assertThat(groups.get(2).prefix()).isEqualTo("java");
        assertThat(groups.get(2).isStatic()).isFalse();
        assertThat(groups.get(3).prefix()).isEmpty();      // Static (catch-all)
        assertThat(groups.get(3).isStatic()).isTrue();
    }

    @Test
    void testParsedConfigMatchesBuiltinAirlift()
            throws IOException
    {
        // Parsed config should produce same essential options as built-in preset
        Path xmlPath = Path.of("src/main/resources/Airlift.xml");
        FormatterConfiguration parsed = parser.parse(xmlPath);
        FormatterConfiguration builtin = FormatterConfiguration.airlift();

        // Compare brace styles
        assertThat(parsed.getClassBraceStyle()).isEqualTo(builtin.getClassBraceStyle());
        assertThat(parsed.getMethodBraceStyle()).isEqualTo(builtin.getMethodBraceStyle());
        assertThat(parsed.getControlBraceStyle()).isEqualTo(builtin.getControlBraceStyle());
        assertThat(parsed.getLambdaBraceStyle()).isEqualTo(builtin.getLambdaBraceStyle());

        // Compare control flow
        assertThat(parsed.isElseOnNewLine()).isEqualTo(builtin.isElseOnNewLine());
        assertThat(parsed.isCatchOnNewLine()).isEqualTo(builtin.isCatchOnNewLine());
        assertThat(parsed.isFinallyOnNewLine()).isEqualTo(builtin.isFinallyOnNewLine());

        // Compare import order groups count
        assertThat(parsed.getImportOrder().getGroups()).hasSize(4);
    }

    @Test
    void testInvalidXmlThrowsException()
    {
        assertThatThrownBy(() -> parser.parse(Path.of("nonexistent.xml")))
                .isInstanceOf(IOException.class);
    }

    @Test
    void testBraceStyleMapping()
    {
        // IntelliJ value 1 = END_OF_LINE (K&R)
        assertThat(BraceStyle.fromIntellijValue(1)).isEqualTo(BraceStyle.END_OF_LINE);
        // IntelliJ value 2 = NEXT_LINE (Allman)
        assertThat(BraceStyle.fromIntellijValue(2)).isEqualTo(BraceStyle.NEXT_LINE);
        // Unknown values default to END_OF_LINE
        assertThat(BraceStyle.fromIntellijValue(99)).isEqualTo(BraceStyle.END_OF_LINE);
    }
}
