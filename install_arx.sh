#!/bin/bash


mvn install:install-file -Dfile=lib/libarx-2.2.0.jar -DgroupId=libarx \
  -DartifactId=libarx -Dversion=2.2.0 -Dpackaging=jar
