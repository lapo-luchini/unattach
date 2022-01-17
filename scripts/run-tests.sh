#!/bin/bash

set -e -u -x -o pipefail

mvn clean test
