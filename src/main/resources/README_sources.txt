-=-=-=-=-=--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
JSymphonic version 0.3.0 SOURCES
Release date:    14.02.2009
README date:     13.02.2009
Website:         http://symphonic.sourceforge.net



-=-=-=-=-=--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
About
-=-=-=-=-=--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

You have downloaded the sources of JSymphonic version 0.3.0. You need to compile these sources to use JSymphonic.
A compiled version can be downloaded from the website:
http://symphonic.sourceforge.net/download.php

Documentation about the program can be found on the website:
http://symphonic.sourceforge.net/page.php?3


-=-=-=-=-=--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
How to compile
-=-=-=-=-=--=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

First, compile the sources using the Java compiler. Being in the parent folder of the "org" and "localization" folders, type on a console:
$ javac org/danizmax/jsymphonic/gui/JSymphonic.java

From here, the program can be run using the command (always being in the same folder):
$ java org.danizmax.jsymphonic.gui.JSymphonic

You can create a ".jar" file with the command:
$ jar cvfm JSymphonic.jar META-INF/MANIFEST.MF davaguine localization org
