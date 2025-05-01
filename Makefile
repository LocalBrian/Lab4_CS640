JAVAC=javac
SRC_DIR=src
SOURCES=$(wildcard $(SRC_DIR)/*.java)

all: compile

compile:
	$(JAVAC) $(SOURCES)

clean:
	rm -f $(SRC_DIR)/*.class
