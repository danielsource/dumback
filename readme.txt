(This repository is part of a college assignment for Object-Oriented Programming.)

Dumback - graphical tool to automatically backup files
  - Simple GUI with Java Swing
  - Automatic scheduled backups
  - Checksum verification (MD5)

Dumback stores its information in
  ~/.dumback/dumback.cfg
  ~/.dumback/dumback.log

Build prerequisites:
  - Java 17+ JDK

Example of building and running:
  $ javac -d out -sourcepath src src/gui/App.java
  $ jar cfm dumback.jar src/MANIFEST.MF -C out .
  $ java -jar dumback.jar
