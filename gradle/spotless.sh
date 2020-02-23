#!/bin/sh
# From https://github.com/diffplug/spotless/issues/178#issuecomment-351638034

echo '[git hook] executing gradle spotlessApply before commit'

# stash any unstaged changes
git stash -q --keep-index

# run the spotlessCheck with the gradle wrapper
./gradlew spotlessApply --daemon

# store the last exit code in a variable
RESULT=$?

# unstash the unstashed changes
git stash pop -q

# return the './gradlew spotlessApply' exit code
exit $RESULT
