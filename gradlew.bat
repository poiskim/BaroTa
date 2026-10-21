@echo off
setlocal
cd /d "%~dp0"
if not exist "gradle\wrapper\gradle-wrapper.jar" (
  echo Missing Gradle Wrapper JAR. Run setup-wrapper.bat first.
  exit /b 1
)
if defined JAVA_HOME (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
) else (
  set "JAVA_EXE=java.exe"
)
"%JAVA_EXE%" -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
exit /b %ERRORLEVEL%
