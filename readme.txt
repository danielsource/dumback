(This repository is part of a college assignment for Object-Oriented Programming.)

Dumback - simple graphical tool to automatically backup

Dumback stores its information in
  ~/.dumback/dumback.cfg
  ~/.dumback/dumback.log

Build prerequisites:
  - Java 17+ JDK

Example of building and running in a unix-like shell:
  $ javac -d out -sourcepath src src/gui/App.java
  $ jar cfm dumback.jar src/MANIFEST.MF -C out .
  $ java -jar dumback.jar
