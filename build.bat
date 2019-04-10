@echo off
chcp 936
set APP_HOME=%~dp0
del daemon.dex
rem javac -Xlint:deprecation ^
javac ^
	-cp "%ANDROID_HOME%"/platforms/android-27/android.jar ^
    -encoding utf-8 -source 1.7 -target 1.7 -d "./output" ^
    ./src/*.java

rem move /Y .\output\*.class ret\sec\oxygenauto
rem cd output

dx --dex --output %APP_HOME%/daemon.dex ^
    --core-library %APP_HOME%/output 
pause


   