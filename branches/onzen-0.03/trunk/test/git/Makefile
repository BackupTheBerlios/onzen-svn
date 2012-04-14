# ----------------------------------------------------------------------------
#
# $Source: /tmp/cvs/onzen/test/svn/Makefile,v $
# $Revision: 1.2 $
# $Author: torsten $
# Contents: Makefile for GIT test data
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

GIT      = git
SVNADMIN = svnadmin

#------------------------ specific variables/settings ------------------------

GITROOT = $(shell pwd)/repository

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
	$(GIT) config --global user.name "test"
	$(GIT) config --global user.email "test@localhost"
	GIT_DIR=$(GITROOT)/.git $(GIT) init
	#
	(\
          $(CD) $(GITROOT); \
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
	(\
          $(CD) $(GITROOT); \
          \
          $(GIT) add *; \
          $(GIT) commit -m "test"; \
        )
	#
	# create test check-out
	$(INSTALL) -d test
	$(GIT) clone $(GITROOT) test
	( \
          $(CD) test; \
          \
          $(GIT) pull; \
          \
          $(ECHO) "more text" >> modified; \
          ($(ECHO) "Hello World"; $(ECHO) ""; $(ECHO) "The end";) > merge; \
          $(ECHO) "more text" >> conflict; \
          $(ECHO) "unknown" > unknown; \
        )
	#
	$(GIT) clone file://$(GITROOT) tmp
	( \
          $(CD) tmp; \
          \
          $(GIT) pull; \
          \
          ($(ECHO) "Hello World"; $(ECHO) "The middle"; $(ECHO) "";) > merge; \
          $(GIT) commit -m "merge test" merge; \
          \
          $(RMF) removed; \
          $(GIT) rm removed; \
          $(GIT) commit -m "removed test" removed; \
          \
          $(ECHO) "conflict" >> conflict; \
          $(GIT) commit -m "conflict test" conflict; \
        )
	$(RMRF) tmp
	#
	( \
          $(CD) test; \
          \
          ($(GIT) pull; true;) \
        )

# end of file
