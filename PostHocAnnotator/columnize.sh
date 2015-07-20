#!/bin/bash

original=${1:-.}
destination=${2:-.}
phrase=

for f in $original/*.csv
do
    b=$(basename $f)
    target=$destination/$b 
    
	echo "columnizing $f to $target"
	
    python columnize_annotations.py $f $target 
done
