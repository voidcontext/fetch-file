#!/bin/bash

DIR="$(dirname $0)"

echo "Destination dir: $DIR"

dd if=/dev/random of=$DIR/100MB.bin bs=1024 count=102400
shasum $DIR/100MB.bin | awk '{print $1}' > $DIR/100MB.bin.shasum
