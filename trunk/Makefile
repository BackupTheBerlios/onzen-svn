# ----------------------------------------------------------------------------
#
# $Source: /tmp/cvs/onzen/Makefile,v $
# $Revision: 1.3 $
# $Author: torsten $
# Contents: Makefile for Onzen
# Systems: all
#			   
# ----------------------------------------------------------------------------

#---------------------------------- settings ---------------------------------

PLATFORM           = Linux

ENABLE_DEBUG       = no

#------------------------------------ paths ----------------------------------

SWT_DIR            = 
LAUNCH4J_DIR       = 

#--------------------------------- tool chain --------------------------------

JAVA               = /usr/local/bin/java
JAVA_FLAGS         =  -Xmx128m

JAVAC              = /usr/local/bin/javac
JAVAC_FLAGS        =  -source 1.5
# -Xlint

JAR                = /usr/bin/jar

SWT_JAR_LINUX      = jars/linux/swt-3.6.1.jar
SWT_JAR_LINUX_64   = jars/linux/swt-3.6.1_64.jar
SWT_JAR_WINDOWS    = jars/windows/swt-3.6.1.jar
SWT_JAR_WINDOWS_64 = jars/windows/swt-3.6.1_64.jar
SWT_JAR_SOLARIS    = jars/solaris/swt-3.6.1.jar
SWT_JAR_MACOSX     = jars/macosx/swt-3.6.1.jar
SWT_JAR_MACOSX_64  = jars/macosx/swt-3.6.1_64.jar

ifneq ($(SWT_DIR),)
SWT_JAR          = $(SWT_DIR)/swt.jar
else
ifeq ($(PLATFORM),Linux)
ifeq ($(JAVA_DATA_MODEL),64)
SWT_JAR          = $(SWT_JAR_LINUX_64)
else
SWT_JAR          = $(SWT_JAR_LINUX)
endif
endif
ifeq ($(PLATFORM),Windows)
ifeq ($(JAVA_DATA_MODEL),64)
SWT_JAR          = $(SWT_JAR_WINDOWS_64)
else
SWT_JAR          = $(SWT_JAR_WINDOWS)
endif
endif
ifeq ($(PLATFORM),Solaris)
SWT_JAR          = $(SWT_JAR_SOLARIS)
endif
ifeq ($(PLATFORM),MacOSX)
ifeq ($(JAVA_DATA_MODEL),64)
SWT_JAR          = $(SWT_JAR_MACOSX_64)
else
SWT_JAR          = $(SWT_JAR_MACOSX)
endif
endif
endif
SQLITE_JAR       = jars/sqlitejdbc.jar

CLASSPATH        = classes:$(SWT_JAR):$(SQLITE_JAR)

#---------------------------------- commands----------------------------------

CAT        = cat
CD         = cd
CHMOD      = chmod
CP         = cp
DIFF       = diff
ECHO       = echo
ECHO_NO_LF = echo -n
GREP       = grep
INSTALL    = /usr/bin/install -c
MKDIR      = mkdir
PERL       = /usr/bin/perl
RMDIR      = rmdir
RMF        = rm -f
RMRF       = rm -rf
SED        = sed
TAR        = tar
UNZIP      = unzip

#------------------------ specific variables/settings ------------------------

VERSION_MAJOR := $(shell $(CAT) version|$(GREP) MAJOR|$(SED) 's/MAJOR=//g')
VERSION_MINOR := $(shell $(CAT) version|$(GREP) MINOR|$(SED) 's/MINOR=//g')

Version = 0.03

#---------------------------------- rules ------------------------------------

classes/%.class: src/%.java
	$(INSTALL) -d classes
ifneq ($(JAVAC),)
	$(JAVAC) $(JAVAC_FLAGS) -d classes -classpath $(CLASSPATH) src/*.java
else
	@$(ECHO) "ERROR: no 'javac' command - cannot compile Java sources"
endif

#--------------------------------- objects -----------------------------------

SOURCES        = src/Onzen.java \
                 src/Settings.java \
                 src/Repository.java \
                 src/RepositoryCVS.java \
                 src/RepositorySVN.java \
                 src/RepositoryHG.java \
                 src/RepositoryGit.java \
                 src/RepositoryList.java \
                 src/RepositoryTab.java \
                 src/Exec.java \
                 src/Message.java \
                 src/Background.java \
                 $(wildcard src/Command*.java) \
                 \
                 src/TableLayout.java \
                 src/TableLayoutData.java \
                 src/Pane.java \
                 src/ProgressBar.java \
                 src/BusyDialog.java \
                 src/Widgets.java \
                 src/Dialogs.java \
                 src/StringParser.java \
                 src/StringUtils.java \
                 src/Option.java \
                 src/Options.java \
                 src/OptionEnumeration.java \
                 src/OptionSpecial.java \
                 src/Dprintf.java \
                 \
                 src/DataModel.java \

Sources = onzen.tcl \
          onzen_conflict.tcl \
          onzen_repository.tcl \
          onzen_view.tcl \
          onzen_busyindicator.tcl \
          onzen_file.tcl \
          onzen_tag.tcl \
          onzen_config.tcl \
          onzen_preferences.tcl \
          onzen_update.tcl \
          \
          pkgIndex.tcl \
          tools.tcl \
          dialogs.tcl \
          localize.tcl \
          mclistbox.tcl \


OTHER_SOURCES  = onzen.in \
                 onzen.bat.in \
                 jar.txt \

OBJECTS        = $(foreach z,$(SOURCES),$(basename $(subst src/,classes/,$z)).class) \

#                 classes/Config.class \

JARS           = onzen.jar \
                 onzen-linux.jar \
                 onzen-linux_64.jar \
                 onzen-windows.jar \
                 onzen-windows_64.jar \
                 onzen-solaris.jar \
                 onzen-macosx.jar \
                 onzen-macosx_64.jar \

TARGETS        = onzen \
                 $(if $(LAUNCH4J),onzen.exe,onzen.bat) \
                 onzen2 \

#------------------------------ dependencies ---------------------------------

.PHONY: all clean distclean depend
all: $(TARGETS)

clean: \
  clean_jars
	$(RMF) $(OBJECTS)
	$(RMF) classes/*.class
	$(RMF) $(JARS)
	$(RMF) $(TARGETS)
	$(RMRF) tmp

distclean: \
  clean
	$(RMF) Makefile
	$(RMF) src/Config.java
	$(RMF) barcontrol
	$(RMF) barcontrol.bat

# JARs
.PHONY: jars clean_jars
jars: $(JARS)

clean_jars:
	$(RMF) $(JARS)

# create JARs
onzen.jar: jar.txt $(OBJECTS) images/*.png
ifneq ($(JAR),)
	$(INSTALL) -d tmp/jar
	$(CP) classes/*.class tmp/jar
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

onzen-linux.jar: jar.txt $(OBJECTS) src/Errors.java images/*.png $(SWT_JAR_LINUX)
ifneq ($(JAR),)
	$(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_LINUX) 1>/dev/null; $(RMRF) META-INF)
	# add SQLite JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SQLITE_JAR) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

onzen-linux_64.jar: jar.txt $(OBJECTS) src/Errors.java images/*.png $(SWT_JAR_LINUX_64)
ifneq ($(JAR),)
	$(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_LINUX_64) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add SQLite JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SQLITE_JAR) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

onzen-windows.jar: jar.txt $(OBJECTS) images/*.png $(SWT_JAR_WINDOWS)
ifneq ($(JAR),)
	$(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_WINDOWS) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add SQLite JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SQLITE_JAR) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

onzen-windows_64.jar: jar.txt $(OBJECTS) images/*.png $(SWT_JAR_WINDOWS_64)
ifneq ($(JAR),)
	$(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_WINDOWS_64) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add SQLite JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SQLITE_JAR) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

onzen-solaris.jar: jar.txt $(OBJECTS) src/Errors.java images/*.png $(SWT_JAR_SOLARIS)
ifneq ($(JAR),)
	$(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_SOLARIS) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add SQLite JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SQLITE_JAR) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

onzen-macosx.jar: jar.txt $(OBJECTS) src/Errors.java images/*.png $(SWT_JAR_MACOSX)
ifneq ($(JAR),)
	$(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_MACOSX) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add SQLite JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SQLITE_JAR) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

onzen-macosx_64.jar: jar.txt $(OBJECTS) src/Errors.java images/*.png $(SWT_JAR_MACOSX_64)
ifneq ($(JAR),)
	$(INSTALL) -d tmp/jar
	# add classes
	$(CP) classes/*.class tmp/jar
	# add SWT JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SWT_JAR_MACOSX_64) 1>/dev/null; $(RMRF) META-INF)
	$(CP) classes/*.class tmp/jar
	# add SQLite JAR
	($(CD) tmp/jar; $(UNZIP) ../../$(SQLITE_JAR) 1>/dev/null; $(RMRF) META-INF)
	# add images
	$(INSTALL) -d tmp/jar/images
	$(CP) images/*.png tmp/jar/images
	# create combined JAR
	($(CD) tmp/jar; $(JAR) cmf ../../jar.txt ../../$@ *)
	$(RMRF) tmp/jar
else
	@$(ECHO) "ERROR: no 'jar' command - cannot create $@"
endif

# create Unix start script
xonzen: onzen.in onzen-linux.jar onzen-linux_64.jar
	$(CAT) barcontrol.in | \
        ( \
          while IFS="" read line; do \
            line=`$(ECHO) "$$line"|$(SED) 's|\@JAVA\@|$(JAVA)|g'`; \
            line=`$(ECHO) "$$line"|$(SED) 's|\@JAVA_FLAGS\@|$(JAVA_FLAGS)|g'`; \
            $(ECHO) "$$line"; \
          done; \
        ) > $@
	$(CHMOD) 775 $@

ifneq ($(LAUNCH4J),)
# create Windows executable
xonzen.exe: onzen-windows.jar
	$(LAUNCH4J) onzen.xml
else
# create Windows start script
xonzen.bat: onzen.bat.in onzen-windows.jar
	$(CAT) onzen.bat.in | \
        ( \
          while IFS="" read line; do \
            line=`$(ECHO) "$$line"|$(SED) 's|\@JAVA\@|$(JAVA)|g'`; \
            line=`$(ECHO) "$$line"|$(SED) 's|\@JAVA_FLAGS\@|$(JAVA_FLAGS)|g'`; \
            $(ECHO) "$$line"; \
          done; \
        ) > $@
	$(CHMOD) 775 $@
endif

# run
run: $(OBJECTS)
ifneq ($(JAVA),)
	$(JAVA) $(JAVA_FLAGS) -classpath $(CLASSPATH) Onzen $(OPTIONS)
else
	@$(ECHO) "ERROR: no 'java' command - cannot execute Onzen"
endif
rundebug: $(OBJECTS)
ifneq ($(JAVA),)
	$(JAVA) $(JAVA_FLAGS) -enableassertions -classpath $(CLASSPATH) Onzen --debug $(OPTIONS)
else
	@$(ECHO) "ERROR: no 'java' command - cannot execute Onzen"
endif
runxdebug: $(OBJECTS)
ifneq ($(JAVA),)
	$(JAVA) $(JAVA_FLAGS) -enableassertions -classpath $(CLASSPATH) Onzen --debug $(OPTIONS)
else
	@$(ECHO) "ERROR: no 'java' command - cannot execute Onzen"
endif

# run with JAR file
ifneq ($(JAVA),)
runjar: onzen
	./onzen2
else
	@$(ECHO) "ERROR: no 'java' commadn - cannot execute Onzen"
endif

.PHONY: install
#install: $(JARS) onzen2
install: onzen-linux.jar onzen2
	$(INSTALL) -d /home/torsten/bin
	$(INSTALL) -d /home/torsten/bin
	$(INSTALL) -m 664 \
                   onzen-linux.jar \
                   /home/torsten/bin
	$(INSTALL) -m 775 \
                   onzen2 \
                   /home/torsten/bin

# create distribution
.PHONY: dist
dist:
ifneq ($(DIRECTORY),)
	$(INSTALL) -d $(DIRECTORY)/onzen
	$(INSTALL) -d $(DIRECTORY)/onzen/src
	$(INSTALL) -m 664 \
                   Makefile.in \
                   onzen.cfg \
                   $(DIRECTORY)/onzen
	$(INSTALL) -m 664 \
                   $(sort $(SOURCES)) \
                   src/Config.java.in \
                   $(DIRECTORY)/onzen/src
	$(INSTALL) -d $(DIRECTORY)/onzen/classes
	$(INSTALL) -m 664 \
                   $(sort $(OTHER_SOURCES)) \
                   $(DIRECTORY)/onzen
	$(INSTALL) -d $(DIRECTORY)/onzen/images
	$(INSTALL) -m 664 \
                   images/*.png \
                   $(DIRECTORY)/onzen/images
	$(INSTALL) -d $(DIRECTORY)/onzen/jars
	$(INSTALL) -m 664 \
                   jars/*.jar \
                   $(DIRECTORY)/onzen/jars
	$(INSTALL) -d $(DIRECTORY)/onzen/jars/linux
	$(INSTALL) -m 664 \
                   jars/linux/*.jar \
                   $(DIRECTORY)/onzen/jars/linux
	$(INSTALL) -d $(DIRECTORY)/onzen/jars/solaris
	$(INSTALL) -m 664 \
                   jars/solaris/*.jar \
                   $(DIRECTORY)/onzen/jars/solaris
	$(INSTALL) -d $(DIRECTORY)/onzen/jars/windows
	$(INSTALL) -m 664 \
                   jars/windows/*.jar \
                   $(DIRECTORY)/onzen/jars/windows
	$(INSTALL) -d $(DIRECTORY)/onzen/jars/macosx
	$(INSTALL) -m 664 \
                   jars/macosx/*.jar \
                   $(DIRECTORY)/onzen/jars/macosx
else
	$(ECHO) Please call 'make dist' from top-level directory to create a distribution.
endif

# end of file

source-dist:
	tar czf onzen-$(Version).tgz $(Sources)

