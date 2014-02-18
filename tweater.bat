@echo off

REM Command (start, stop, add, dump, etc) comes from the command line
set COMMAND=%1

REM Set the amount of memory TwEater can use. You might need more if your
REM queries generate tweets faster than they can be written to disk. If your
REM queries generate bursts of 500 tweets per minute or more, you should
REM consider increasing the memory parameter to leave more room for caching
REM tweets in RAM while they are waiting to be written to disk. If you are
REM running only low-volume queries and you have turned off sentiment
REM analysis, you can safely decrease this some, probably as far as 256m.
set MEMORY=2048m

REM This specifies the name of jar file that TwEater resides in. You may
REM have to change this if you're compiling the code yourself.
set JARNAME=TwEater

REM -------------------------------------------------------------------------
REM EDIT BELOW THIS LINE AT YOUR OWN RISK -----------------------------------
REM -------------------------------------------------------------------------

REM Make sure that RMIRegistry is up and running
tasklist /FI "IMAGENAME eq rmiregistry.exe" | find /I "rmiregistry.exe" > nul
if errorlevel 1 (
	start /B rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false > nul
)

REM Set the classpath and jarpath
setLocal EnableDelayedExpansion
set CP=
for /R "%cd%/build/dist" %%j in (*.jar) do (
	set CP=!CP!;%%j
)
set CP=!CP:\=/!
set JARPATH=%cd%/build/dist/%JARNAME%.jar

REM Uncomment these for debugging if you're having trouble
REM echo $CP
REM echo $JARPATH

REM Check whether the first argument is "start" or something else
if "%COMMAND%" == "start" (
	REM Run the program in a new cmd window and Xmx to start a new instance
	start "TwEater %DATE:~-4%-%DATE:~4,2%-%DATE:~7,2%" java -server -Xmx%MEMORY% -cp "%CP%" ^
			-XX:-UseGCOverheadLimit ^
			-Djava.rmi.server.codebase="file:/%JARPATH:\=/%" ^
			-Djava.util.logging.config.file=logging.properties ^
		-Dfile.encoding=UTF-8 ^
			edu.umd.cs.dmonner.tweater.TwEater %*
) else (
	REM Run the program in single-interaction mode
	java -cp "%CP%" ^
			-Djava.rmi.server.codebase="file:/%JARPATH:\=/%" ^
			-Djava.util.logging.config.file=logging.properties ^
		-Dfile.encoding=UTF-8 ^
			edu.umd.cs.dmonner.tweater.TwEater %*
)
