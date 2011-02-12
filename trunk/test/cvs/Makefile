# ----------------------------------------------------------------------------
#
# $Source: /tmp/cvs/onzen/test/cvs/Makefile,v $
# $Revision: 1.2 $
# $Author: torsten $
# Contents: Makefile for CVS test data
# Systems: all
#			   
# ----------------------------------------------------------------------------

#--------------------------------- tool chain --------------------------------

#---------------------------------- commands----------------------------------

CD      = cd
CP      = cp
DD      = dd
ECHO    = echo
FIND    = find
GREP    = grep
INSTALL = install -c
MV      = mv
RMF     = rm -f
RMRF    = rm -rf

CVS     = cvs

#------------------------ specific variables/settings ------------------------

CVSROOT = $(shell pwd)/repository

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
	$(CVS) -d $(CVSROOT) init
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
	($(CD) tmp; $(CVS) -d $(CVSROOT) import -m "test" test test-vendor test-release)
	$(RMRF) tmp
	#
	# create test check-out
	$(INSTALL) -d test
	$(CVS) -d $(CVSROOT) get -d test test
	( \
          $(CD) test; \
          \
          $(CVS) -d $(CVSROOT) update; \
          $(ECHO) "more text" >> modified; \
          ($(ECHO) "Hello World"; $(ECHO) ""; $(ECHO) "The end";) > merge; \
          $(ECHO) "more text" >> conflict; \
        )
	#
	$(CVS) -d $(CVSROOT) get -d tmp test
	( \
          $(CD) tmp; \
          \
          ($(ECHO) "Hello World"; $(ECHO) "The middle"; $(ECHO) "";) > merge; \
          $(CVS) -d $(CVSROOT) commit -m "merge test" merge; \
          \
          $(RMF) removed; \
          $(CVS) -d $(CVSROOT) remove removed; \
          $(CVS) -d $(CVSROOT) commit -m "removed test" removed; \
          \
          $(ECHO) "conflict" >> conflict; \
          $(CVS) -d $(CVSROOT) commit -m "conflict test" conflict; \
        )
	$(RMRF) tmp
	#
	( \
          $(CD) test; \
          \
          $(CVS) -d $(CVSROOT) update conflict; \
        )

# end of file
