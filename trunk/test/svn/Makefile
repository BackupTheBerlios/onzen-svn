# ----------------------------------------------------------------------------
#
# $Source: /tmp/cvs/onzen/test/svn/Makefile,v $
# $Revision: 1.2 $
# $Author: torsten $
# Contents: Makefile for SVN test data
# Systems: all
#			   
# ----------------------------------------------------------------------------

#--------------------------------- tool chain --------------------------------

#---------------------------------- commands----------------------------------

CD       = cd
CP       = cp
DD       = dd
ECHO     = echo
FIND     = find
GREP     = grep
INSTALL  = install -c
MV       = mv
RMF      = rm -f
RMRF     = rm -rf

SVN      = svn
SVNADMIN = svnadmin

#------------------------ specific variables/settings ------------------------

SVNROOT = $(shell pwd)/repository

#--------------------------------- objects -----------------------------------

TARGETS = repository \
          test

#------------------------------ dependencies ---------------------------------

all:
	@$(ECHO) create - create test repository
	@$(ECHO) clean  - clean test repository

clean:
	$(RMRF) $(TARGETS)

.PHONY: create
create:
	# create test repository
	$(SVNADMIN) create $(SVNROOT)
	#
	$(INSTALL) -d tmp
	(\
          $(CD) tmp; \
          \
          $(ECHO) "Hello World" > modified; \
          ($(ECHO) "Hello World"; $(ECHO) ""; $(ECHO) "";) > merge; \
          $(ECHO) "Hello World" > merge; \
          $(ECHO) "Hello World" > removed; \
          $(ECHO) "Hello World" > conflict; \
          $(DD) if=/dev/random bs=1024 count=1 1>binary 2>/dev/null; \
          $(ECHO) "Hello World" > "name with space"; \
          $(INSTALL) -d subdirectory; \
          $(ECHO) "Hello Underworld" > subdirectory/sub-file; \
        )
	($(CD) tmp; $(SVN) import . file://$(SVNROOT)/trunk -m "test")
	$(RMRF) tmp
	$(INSTALL) -d tmp
	(\
          $(CD) tmp; \
          \
          $(INSTALL) -d branches; \
        )
	($(CD) tmp; $(SVN) import . file://$(SVNROOT) -m "test")
	$(RMRF) tmp
	$(INSTALL) -d tmp
	(\
          $(CD) tmp; \
          \
          $(INSTALL) -d tags; \
        )
	($(CD) tmp; $(SVN) import . file://$(SVNROOT) -m "test")
	$(RMRF) tmp
	#
	# create trunk check-out
	$(INSTALL) -d test
	$(SVN) checkout file://$(SVNROOT)/trunk test
	( \
          $(CD) test; \
          \
          $(SVN) update; \
          $(ECHO) "more text" >> modified; \
          ($(ECHO) "Hello World"; $(ECHO) ""; $(ECHO) "The end";) > merge; \
          $(ECHO) "more text" >> conflict; \
          $(ECHO) "unknown" > unknown; \
        )
	#
	# create branch
	$(SVN) copy file://$(SVNROOT)/trunk file://$(SVNROOT)/branches/b1 --force-log -m "test"
	#
	# modify files
	$(SVN) checkout file://$(SVNROOT)/trunk tmp
	( \
          $(CD) tmp; \
          \
          ($(ECHO) "Hello World"; $(ECHO) "The middle"; $(ECHO) "";) > merge; \
          $(SVN) commit -m "merge test" merge; \
          \
          $(RMF) removed; \
          $(SVN) remove removed; \
          $(SVN) commit -m "removed test" removed; \
          \
          $(ECHO) "conflict" >> conflict; \
          $(SVN) commit -m "conflict test" conflict; \
        )
	$(RMRF) tmp
	#
	( \
          $(CD) test; \
          \
          $(SVN) update --accept=postpone conflict; \
        )

# end of file
