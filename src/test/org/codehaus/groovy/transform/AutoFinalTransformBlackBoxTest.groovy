/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.transform

import gls.CompilableTestSupport
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4


/**
 * Tests for the {@code @AutoFinal} AST transform.
 */

// Execute single test:
// gradlew :test --build-cache --tests org.codehaus.groovy.transform.AutoFinalTransformTest
@RunWith(JUnit4)
class AutoFinalTransformBlackBoxTest extends CompilableTestSupport {

  @Test
  void testAutoFinal_Closure() {
    assertAutoFinalClassTestScript("param0", "String foo() { final cls = { String param0 -> param0 = 'abc'; param0 }; cls() }")
  }

  @Test
  void testAutoFinal_ClosureInClosure() {
    assertAutoFinalClassTestScript("param1", "String foo() { final cls0 = { String param0 -> final cls1 = { String param1 -> param1 = 'xyz'; param1 }; cls1() }; cls0() }")
  }

  @Test
  void testAutoFinal_ClassMethod_Param0() {
    assertAutoFinalClassTestScript("param0", "String foo(String param0, param1) {  param0 = 'abc'; param0 }")
  }

  @Test
  void testAutoFinal_ClassMethod_Param1() {
    assertAutoFinalClassTestScript("param1", "String foo(String param0, param1) {  param1 = new Object(); param1 }")
  }

  // Check default parameters are not negatively impacted by @AutoFinal
  @Test
  void testAutoFinalClassMethodDefaultParameters() {
    final String classPart = """
      String foo(String param0 = 'XyZ', param1 = Closure.IDENTITY) { 
        assert param0.equals('XyZ')
        assert param1.is(Closure.IDENTITY)
        return param0 
      }
    """
    final script = autoFinalTestScript(true, classPart, "final foo = new $autoFinalTestClassName(); foo.foo()" )
    assert script.contains('@AutoFinal')
    assertScript(script)
  }




  void assertAutoFinalClassTestScript(final String paramName, final String classPart) {
    assertAutoFinalTestScriptWithAnnotation(paramName, classPart)
    assertAutoFinalTestScriptWithoutAnnotation(paramName, classPart)
  }

  // Checks Groovy compiler behavior when putting the passed classPart into an @AutoFinal annotated class
  void assertAutoFinalTestScriptWithAnnotation(final String paramName, final String classPart) {
    final script = autoFinalTestScript(true, classPart)
    assert script.contains('@AutoFinal')
    final result = shouldNotCompile(script)
    println "\nassertAutoFinalTestScript result: |$result|\n\n"
    assert result.contains("The parameter [$paramName] is declared final but is reassigned")
  }

  void assertAutoFinalTestScriptWithoutAnnotation(final String paramName, final String classPart) {
    final script = autoFinalTestScript(false, classPart)
    assert !script.contains('@AutoFinal')
    shouldCompile(script)
  }

  String autoFinalTestScript(final boolean autoFinalAnnotationQ, final String classPart, final String scriptPart = '') {
    final String script = """
            import groovy.transform.AutoFinal
            import groovy.transform.ASTTest
            import static org.codehaus.groovy.control.CompilePhase.SEMANTIC_ANALYSIS
            import static java.lang.reflect.Modifier.isFinal

            ${autoFinalAnnotationQ ? '@AutoFinal' : ''}
            class $autoFinalTestClassName {
                $classPart
            } 

            $scriptPart
        """
    println "script: |$script|"
    return script
  }

  String getAutoFinalTestClassName() {
    'AutoFinalFoo'
  }

  /**
   * Prints better readable, unabbreviated stack trace for passed Throwable
   */
  void printStackTrace(final Throwable throwable) {
    println "${throwable.getClass().name}${throwable.message ? ": $throwable.message" : ""}"
    throwable.stackTrace.each { println it }
    final inner = throwable.cause
    if(inner != null) {
      println "Caused by........................................................................................."
      printStackTrace(inner)
    }
  }

  Throwable shouldThrow(final String script) {
    try {
      final GroovyClassLoader gcl = new GroovyClassLoader()
      gcl.parseClass(script, getTestClassName())
    }
    catch(Throwable throwable) {
      return throwable
    }
    throw new Exception("Script was expected to throw here!")
  }
}
