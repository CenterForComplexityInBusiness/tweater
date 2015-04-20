@echo off

set WD=%cd%
for %%f in (%WD%) do set PROJECT=%%~nxf

ant -f build.xml -Dproject="%PROJECT%" clean
