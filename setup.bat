@echo off

setLocal EnableDelayedExpansion
set CP=
for /R "%cd%/build/dist" %%j in (*.jar) do (
	set CP=!CP!;%%j
)
set CP=!CP:\=/!

java -cp "%CP%" edu.umd.cs.dmonner.tweater.util.Setup %*
