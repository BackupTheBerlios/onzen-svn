@set SWT_JAR_WINDOWS=jars\windows\swt-3.7.2.jar
rem @set SWT_JAR_WINDOWS=jars\windows\swt-3.7.2_64.jar
@set SQLITE_JAR=jars\sqlitejdbc.jar
@set MAIL_JAR=jars\mail.jar

@set VERSION_MAJOR=0
@set VERSION_MINOR=02
@set VERSION_REVISION=unknown 

@set CLASSPATH=classes;%SWT_JAR_WINDOWS%;%SQLITE_JAR%;%MAIL_JAR%

set UNZIP="C:\Program Files (x86)\7-Zip\7z.exe"
set JAR=jar

@if "%1" == "config" goto config
@if "%1" == "compile" goto compile
@if "%1" == "run" goto run
@if "%1" == "jars" goto jars
@goto compile

:config
@del src\Config.java 2>NUL
@for /f "tokens=* delims=" %%s in (src\Config.java.in) do @(
  @set "line=%%s"
  @call set line=%%line:@VERSION_MAJOR@=%VERSION_MAJOR%%%
  @call set line=%%line:@VERSION_MINOR@=%VERSION_MINOR%%%
  @call set line=%%line:@VERSION_REVISION@=%VERSION_REVISION%%%
  @call echo %%line%% >> src\Config.java
)
@goto end

:compile
@mkdir classes 2>NUL
if exist src\Config.java goto skipConfig
call make.bat config
:skipConfig
javac.exe -d classes -cp %CLASSPATH% src\*.java
@goto end

:run
make.bat compile
java -cp %CLASSPATH% Onzen 
@goto end

:jars
rem make.bat compile
mkdir tmp 2>NUL
mkdir tmp\jar 2>NUL
rem add classes
copy /Y classes\*.class tmp\jar 1>NUL
rem add SWT JAR
cd tmp\jar
%UNZIP% -y x ..\..\%SWT_JAR_WINDOWS% 1>NUL
del /S /Q META-INF 1>NUL 2>NUL
cd ..\..
rem add SQLite JAR
cd tmp\jar
%UNZIP% -y x ..\..\%SQLITE_JAR% 1>NUL
del /S /Q META-INF 1>NUL 2>NUL
cd ..\..
rem add mail JAR
cd tmp\jar
%UNZIP% -y x ..\..\%MAIL_JAR% 1>NUL
del /S /Q META-INF 1>NUL 2>NUL
cd ..\..
rem add images
mkdir tmp\jar\images 2>NUL
copy /Y images\*.png tmp\jar\images 1>NUL
rem create combined JAR
cd tmp\jar
%JAR% cmf ..\..\jar.txt ..\..\onzen-windows.jar *
cd ..\..
del /S /Q tmp\jar 1>NUL
@goto end


:end
