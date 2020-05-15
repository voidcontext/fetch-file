#!/bin/bash

set -x

DIR="$(dirname $0)"

echo "Destination dir: $DIR"

for size in 10 100; do
  filename=${size}MB.bin
  head -c $(($size * 1048576)) </dev/urandom > $DIR/$filename
  gzip -k -f $DIR/$filename
  shasum -a 256 $DIR/$filename | awk '{print $1}' > $DIR/$filename.sha256
  shasum -a 256 $DIR/$filename.gz | awk '{print $1}' > $DIR/$filename.gz.sha256
done
