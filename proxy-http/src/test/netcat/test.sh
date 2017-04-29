#!/bin/bash

file1="$(cat $1)"
file2="$(cat $2)"

if cmp -s "$file1" "$file2"
then
    echo "The files match"
else
    echo "The file do not match"
fi
