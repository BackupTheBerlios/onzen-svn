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
	$(INSTALL) -d repository/trunk
	HGRCPATH=hgrc $(HG) init $(HGROOT)/trunk
	$(CP) hgrc $(HGROOT)/trunk/.hg/hgrc
	#
	(\
          $(CD) $(HGROOT)/trunk; \
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
          $(CD) $(HGROOT)/trunk; \
          \
          HGRCPATH=../hgrc $(HG) add *; \
          HGRCPATH=../hgrc $(HG) commit -m "test"; \
        )
	#
	# create test check-out
	HGRCPATH=hgrc $(HG) clone file://$(HGROOT)/trunk test
	( \
          $(CD) test; \
          \
          HGRCPATH=../hgrc $(HG) pull; \
          HGRCPATH=../hgrc HGMERGE=merge $(HG) -y update; \
          \
          $(ECHO) "more text" >> modified; \
          ($(ECHO) "Hello World"; $(ECHO) ""; $(ECHO) "The end";) > merge; \
          $(ECHO) "more text" >> conflict; \
          $(ECHO) "unknown" > unknown; \
        )
	# create branch
	$(INSTALL) -d repository/branches
	HGRCPATH=../hgrc $(HG) clone file://$(HGROOT)/trunk file://$(HGROOT)/branches/b1 ; \
	( \
          $(CD) test; \
          hg branch; \
        )
	#
	# modify files
	HGRCPATH=hgrc $(HG) clone file://$(HGROOT)/trunk tmp
	( \
          $(CD) tmp; \
          \
          HGRCPATH=../hgrc $(HG) pull; \
          HGRCPATH=../hgrc HGMERGE=merge $(HG) -y update; \
          \
          ($(ECHO) "Hello World"; $(ECHO) "The middle"; $(ECHO) "";) > merge; \
          HGRCPATH=../hgrc $(HG) commit -m "merge test" merge; \
          \
          $(RMF) removed; \
          HGRCPATH=../hgrc $(HG) remove removed; \
          HGRCPATH=../hgrc $(HG) commit -m "removed test" removed; \
          \
          $(ECHO) "conflict" >> conflict; \
          HGRCPATH=../hgrc $(HG) commit -m "conflict test" conflict; \
          \
          HGRCPATH=../hgrc $(HG) push; \
        )
	$(RMRF) tmp
	#
	( \
          $(CD) test; \
          \
          HGRCPATH=../hgrc $(HG) pull; \
          (HGMERGE=merge $(HG) -y update; true;) \
        )

# end of file
