@set SVN=svn
@set SVNADMIN=svnadmin
@set SVNROOT=%CD%\repository

@if "%1" == "help" goto help
@if "%1" == "create" goto create
@if "%1" == "clean" goto clean
@goto help

:help
@echo Targets:
@echo.
@echo   create - create test SVN repository
@echo   clean  - clean test SVN repository
@goto end

:create
rem create test repository
%SVNADMIN% create %SVNROOT%

mkdir tmp
cd tmp
echo Hello World >modified 
echo Hello World >merge 
echo . >>merge
echo . >>merge
echo Hello World >>merge 
echo Hello World >removed
echo Hello World >conflict
echo xxxx >binary
echo Hello World >"name with space"
mkdir subdirectory
echo Hello Underworld > subdirectory\sub-file
%SVN% import . file:///%SVNROOT%/trunk -m "test"
cd ..
rmdir /S /Q tmp 1>NUL

mkdir tmp
cd tmp
mkdir branches
%SVN% import . file:///%SVNROOT% -m "test"
cd ..
rmdir /S /Q tmp 1>NUL

mkdir tmp
cd tmp
mkdir tags
%SVN% import . file:///%SVNROOT% -m "test"
cd ..
rmdir /S /Q tmp 1>NUL

rem create trunk check-out
mkdir test
%SVN% checkout file:///%SVNROOT%/trunk test
cd test
%SVN% update
echo more text >>modified
echo Hello World >merge
echo . >>merge
echo The end >>merge
echo more text >>conflict
echo unknown >unknown
cd ..

rem create branch
%SVN% copy file:///%SVNROOT%/trunk file:///%SVNROOT%/branches/b1 --force-log -m "test"

rem modify files
%SVN% checkout file:///%SVNROOT%/trunk tmp
cd tmp
echo Hello World >merge
echo The middle >>merge
echo . >>merge
%SVN% commit -m "merge test" merge
del removed
%SVN% remove removed
%SVN% commit -m "removed test" removed
echo conflict >>conflict
%SVN% commit -m "conflict test" conflict
cd ..
rmdir /S /Q tmp 1>NUL

cd test
%SVN% update --accept=postpone conflict
@goto end

:clean
rmdir /S /Q repository 1>NUL
rmdir /S /Q test 1>NUL
@goto end

:end
