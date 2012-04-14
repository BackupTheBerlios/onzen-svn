# ----------------------------------------------------------------------------
#
# $Source: /tmp/cvs/onzen/test/hg/Makefile,v $
# $Revision: 1.2 $
# $Author: torsten $
# Contents: Makefile for Mercuial test data
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

HG       = hg

#------------------------ specific variables/settings ------------------------

HGROOT = $(shell pwd)/repository

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
	$(HG) init $(HGROOT)
	$(CP) hgrc $(HGROOT)/.hg/hgrc
	#
	(\
          $(CD) $(HGROOT); \
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
          $(CD) $(HGROOT); \
          \
          $(HG) add *; \
          $(HG) commit -m "test"; \
         )
	ls -la $(HGROOT)
	#
	# create test check-out
	$(HG) clone file://$(HGROOT) test
	( \
          $(CD) test; \
          \
          $(HG) pull; \
          HGMERGE=merge $(HG) -y update; \
          \
          $(ECHO) "more text" >> modified; \
          ($(ECHO) "Hello World"; $(ECHO) ""; $(ECHO) "The end";) > merge; \
          $(ECHO) "more text" >> conflict; \
          $(ECHO) "unknown" > unknown; \
        )
	#
	$(HG) clone file://$(HGROOT) tmp
	( \
          $(CD) tmp; \
          \
          $(HG) pull; \
          HGMERGE=merge $(HG) -y update; \
          \
          ($(ECHO) "Hello World"; $(ECHO) "The middle"; $(ECHO) "";) > merge; \
          $(HG) commit -m "merge test" merge; \
          \
          $(RMF) removed; \
          $(HG) remove removed; \
          $(HG) commit -m "removed test" removed; \
          \
          $(ECHO) "conflict" >> conflict; \
          $(HG) commit -m "conflict test" conflict; \
          \
          $(HG) push; \
        )
	$(RMRF) tmp
	#
	( \
          $(CD) test; \
          \
          $(HG) pull; \
          (HGMERGE=merge $(HG) -y update; true;) \
        )

# end of file
