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
package com.dremio.jdbc.test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.dremio.jdbc.Driver;
import com.dremio.jdbc.JdbcTestBase;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


public class Drill2463GetNullsFailedWithAssertionsBugTest extends JdbcTestBase {

  private static Connection connection;
  private static Statement statement;

  @BeforeClass
  public static void setUpConnection() throws SQLException {
    // (Note: Can't use JdbcTest's connect(...) because JdbcTest closes
    // Connection--and other JDBC objects--on test method failure, but this test
    // class uses some objects across methods.)
    connection = new Driver().connect( "jdbc:dremio:zk=local", JdbcAssert.getDefaultProperties() );
    statement = connection.createStatement();
  }

  @AfterClass
  public static void tearDownConnection() throws SQLException {
    connection.close();
  }

  // Test primitive types vs. non-primitive types:

  @Test
  public void testGetPrimitiveTypeNullAsOwnType() throws Exception {
    final ResultSet rs = statement.executeQuery(
        "SELECT CAST( NULL AS INTEGER ) FROM INFORMATION_SCHEMA.CATALOGS" );
    assertTrue( rs.next() );
    assertThat( "getInt(...) for NULL", rs.getInt( 1 ), equalTo( 0 ) );
    assertThat( "wasNull", rs.wasNull(), equalTo( true ) );
  }

  @Test
  public void testGetPrimitiveTypeNullAsObject() throws Exception {
    final ResultSet rs = statement.executeQuery(
        "SELECT CAST( NULL AS INTEGER ) FROM INFORMATION_SCHEMA.CATALOGS" );
    assertTrue( rs.next() );
    assertThat( "getObject(...) for NULL", rs.getObject( 1 ), nullValue() );
    assertThat( "wasNull", rs.wasNull(), equalTo( true ) );
  }

  @Test
  public void testGetNonprimitiveTypeNullAsOwnType() throws Exception {
    final ResultSet rs = statement.executeQuery(
        "SELECT CAST( NULL AS VARCHAR ) FROM INFORMATION_SCHEMA.CATALOGS" );
    assertTrue( rs.next() );
    assertThat( "getString(...) for NULL", rs.getString( 1 ), nullValue() );
    assertThat( "wasNull", rs.wasNull(), equalTo( true ) );
  }

  // Test a few specifics

  @Test
  public void testGetBooleanNullAsOwnType() throws Exception {
    final ResultSet rs = statement.executeQuery(
        "SELECT CAST( NULL AS BOOLEAN ) FROM INFORMATION_SCHEMA.CATALOGS" );
    assertTrue( rs.next() );
    assertThat( "getBoolean(...) for NULL", rs.getBoolean( 1 ), equalTo( false ) );
    assertThat( "wasNull", rs.wasNull(), equalTo( true ) );
  }

  @Test
  public void testGetBooleanNullAsObject() throws Exception {
    final ResultSet rs = statement.executeQuery(
        "SELECT CAST( NULL AS BOOLEAN ) FROM INFORMATION_SCHEMA.CATALOGS" );
    assertTrue( rs.next() );
    assertThat( "getObject(...) for NULL", rs.getObject( 1 ), nullValue() );
    assertThat( "wasNull", rs.wasNull(), equalTo( true ) );
  }

  @Test
  public void testGetIntegerNullAsOwnType() throws Exception {
    final ResultSet rs = statement.executeQuery(
        "SELECT CAST( NULL AS INTEGER ) FROM INFORMATION_SCHEMA.CATALOGS" );
    assertTrue( rs.next() );
    assertThat( "getInt(...) for NULL", rs.getInt( 1 ), equalTo( 0 ) );
    assertThat( "wasNull", rs.wasNull(), equalTo( true ) );
  }

  @Test
  public void testGetIntegerNullAsObject() throws Exception {
    final ResultSet rs = statement.executeQuery(
        "SELECT CAST( NULL AS INTEGER ) FROM INFORMATION_SCHEMA.CATALOGS" );
    assertTrue( rs.next() );
    assertThat( "getObject(...) for NULL", rs.getObject( 1 ), nullValue() );
    assertThat( "wasNull", rs.wasNull(), equalTo( true ) );
  }
}
