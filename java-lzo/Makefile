# Makefile for GNU make
#
# Copyright (C) 1998, 1999 Markus F.X.J. Oberhumer

# for Unix
SOURCES = org/lzo/*.java
CLASSPATH := .:$(CLASSPATH)

# use this under Windows
##SOURCES = org\\lzo\\*.java
##CLASSPATH := .;$(CLASSPATH)

export CLASSPATH

# choose a java compiler
JAVAC = jikes
JAVAC = jikes +P
JAVAC = jikes +1.0
JAVAC = javac
JAVAC = javac -O


all:
	$(JAVAC) $(SOURCES)
	$(JAVAC) *.java

javadoc:
	-rm -f javadoc/*.html
	-mkdir -p javadoc
	javadoc -d javadoc -package -author $(SOURCES)

clean:
	-rm -f *.class org/lzo/*.class

distclean: clean

maintainer-clean: distclean
	-rm -f javadoc/*.html


.PHONY: all javadoc clean distclean maintainer-clean

.SUFFIXES: .class .java

