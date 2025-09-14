@echo off
REM Windows batch script equivalent of gradle/spotless.sh
REM Stash any unstaged changes

git stash -q --keep-index

REM Run the spotlessApply with the gradle wrapper
call gradlew.bat spotlessApply --daemon
set RESULT=%ERRORLEVEL%

REM Unstash the unstaged changes
git stash pop -q

REM Return the gradlew spotlessApply exit code
exit /b %RESULT%
