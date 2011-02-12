# ----------------------------------------------------------------------------
#
# $Source: /tmp/cvs/onzen/test/svn/Makefile,v $
# $Revision: 1.1 $
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
	@$(ECHO) create - create test data
	@$(ECHO) clean  - clean test data

clean:
	$(RMRF) $(TARGETS)

.PHONY: create
create:
	# create test repository
	$(INSTALL) -d repository
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
        )
	($(CD) tmp; $(SVN) import . file://$(SVNROOT)/test -m "test")
	$(RMRF) tmp
	#
	# create test check-out
	$(INSTALL) -d test
	$(SVN) checkout file://$(SVNROOT)/test test
	( \
          $(CD) test; \
          \
          $(SVN) update; \
          $(ECHO) "more text" >> modified; \
          ($(ECHO) "Hello World"; $(ECHO) ""; $(ECHO) "The end";) > merge; \
          $(ECHO) "more text" >> conflict; \
        )
	#
	$(SVN) checkout file://$(SVNROOT)/test tmp
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
