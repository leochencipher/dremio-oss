/*
 * Copyright (C) 2017 Dremio Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dremio.hbase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.Test;

import com.dremio.exec.store.hbase.HBaseRegexParser;
import com.dremio.test.DremioTest;

public class TestHBaseRegexParser extends DremioTest {

  @Test
  public void testLikeExprToRegex() throws Exception {
    HBaseRegexParser parser = new HBaseRegexParser("ABC%[0-7][0-9A-Fa-f]").parse();
    assertEquals("^\\QABC\\E.*[0-7][0-9A-Fa-f]$", parser.getRegexString());
    assertEquals("ABC", parser.getPrefixString());
    Pattern pattern = Pattern.compile(parser.getRegexString(), Pattern.DOTALL);
    assertTrue(pattern.matcher("ABC79").matches());
    assertTrue(pattern.matcher("ABCxxxxxxx79").matches());

    parser = new HBaseRegexParser("ABC%[0-8]%_").parse();
    assertEquals("^\\QABC\\E.*[0-8].*.$", parser.getRegexString());
    assertEquals("ABC", parser.getPrefixString());
    pattern = Pattern.compile(parser.getRegexString(), Pattern.DOTALL);
    assertTrue(pattern.matcher("ABC79").matches());
    assertTrue(pattern.matcher("ABCxxxx79").matches());
    assertTrue(pattern.matcher("ABCxxxx7xxxxx9").matches());
    assertTrue(pattern.matcher("ABC[0-8]_").matches());

    parser = new HBaseRegexParser("ABC%[0-8]%_", '%').parse();
    assertEquals("^\\QABC\\E\\Q[\\E\\Q0-8]\\E\\Q_\\E$", parser.getRegexString());
    assertEquals("ABC[0-8]_", parser.getPrefixString());
    pattern = Pattern.compile(parser.getRegexString(), Pattern.DOTALL);
    assertFalse(pattern.matcher("ABC79").matches());
    assertTrue(pattern.matcher("ABC[0-8]_").matches());

    try {
      parser = new HBaseRegexParser("ABC%[0-8][^a-f]%", '%').parse();
      fail("Parsed an illegal LIKE expression.");
    } catch (IllegalArgumentException e) {
    }
  }

}
