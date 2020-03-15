#!/bin/bash

DIR="$(dirname $0)"

echo "Destination dir: $DIR"

head -c 104857600 </dev/urandom > $DIR/100MB.bin
shasum $DIR/100MB.bin | awk '{print $1}' > $DIR/100MB.bin.shasum
