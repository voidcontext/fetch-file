#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

dd if=/dev/random of=$DIR/100MB.bin bs=1024 count=102400
shasum $DIR/100MB.bin | awk '{print $1}' > $DIR/100MB.bin.shasum
