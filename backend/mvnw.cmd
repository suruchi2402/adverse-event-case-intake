@echo off
rem Minimal Maven wrapper for Windows: downloads the Maven version in .mvn\wrapper\maven-wrapper.properties once.
rem Replace with the official wrapper any time: mvn -N wrapper:wrapper
setlocal
set "BASEDIR=%~dp0"
for /f "usebackq tokens=1,* delims==" %%A in ("%BASEDIR%.mvn\wrapper\maven-wrapper.properties") do if "%%A"=="distributionUrl" set "URL=%%B"
for %%F in ("%URL%") do set "ZIP=%%~nF"
set "NAME=%ZIP:-bin=%"
set "DIST=%USERPROFILE%\.m2\wrapper\dists\%NAME%"
if not exist "%DIST%\%NAME%\bin\mvn.cmd" (
  echo Downloading %URL%
  powershell -NoProfile -Command "New-Item -ItemType Directory -Force -Path '%DIST%' | Out-Null; Invoke-WebRequest -Uri '%URL%' -OutFile '%DIST%\download.zip'; Expand-Archive -Force '%DIST%\download.zip' '%DIST%'; Remove-Item '%DIST%\download.zip'"
)
"%DIST%\%NAME%\bin\mvn.cmd" %*
